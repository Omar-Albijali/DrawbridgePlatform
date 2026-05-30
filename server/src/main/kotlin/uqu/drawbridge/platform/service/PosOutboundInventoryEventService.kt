package uqu.drawbridge.platform.service

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.dto.PosOutboundInventoryEventDTO
import uqu.drawbridge.platform.model.InventoryAuditLog
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.dto.PosOutboundEventStatus
import uqu.drawbridge.platform.model.PosOutboundInventoryEvent
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.PosIntegrationRepository
import uqu.drawbridge.platform.repository.PosOutboundInventoryEventRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.dto.PosInventoryWebhookPayload
import kotlin.math.min

@Service
class PosOutboundInventoryEventService(
    private val posOutboundInventoryEventRepository: PosOutboundInventoryEventRepository,
    private val posIntegrationRepository: PosIntegrationRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    @Value("\${pos.webhook.max-retries:6}")
    private val maxRetries: Int,
    @Value("\${pos.webhook.retry-base-delay-ms:5000}")
    private val retryBaseDelayMs: Long,
    @Value("\${pos.webhook.retry-max-delay-ms:300000}")
    private val retryMaxDelayMs: Long,
    @Value("\${pos.webhook.request-timeout-seconds:5}")
    private val requestTimeoutSeconds: Int
) {
    private val logger = LoggerFactory.getLogger(PosOutboundInventoryEventService::class.java)
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(requestTimeoutSeconds.toLong()))
        .build()

    @Transactional
    fun captureInventoryAuditChange(auditLog: InventoryAuditLog) {
        if (!shouldPublish(auditLog.sourceType)) {
            return
        }
        val inventoryItemId = auditLog.inventoryItemId ?: return
        val inventoryItem = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return
        val integration = posIntegrationRepository.findByRetailer_Id(inventoryItem.retailerId ?: "") ?: return
        val productId = auditLog.productId ?: return
        val product = productRepository.findById(productId).orElse(null) ?: return

        val eventId = "audit-${auditLog.id ?: UUID.randomUUID()}"
        if (posOutboundInventoryEventRepository.findByEventIdAndRetailer_Id(eventId, inventoryItem.retailerId ?: "") != null) {
            return
        }

        val payload = PosInventoryWebhookPayload(
            eventId = eventId,
            eventTime = auditLog.createdAt.toString(),
            retailerId = inventoryItem.retailerId ?: "",
            sourceType = auditLog.sourceType.name,
            sourceId = auditLog.sourceId,
            gtin = product.gtin,
            productId = product.id.orEmpty(),
            inventoryItemId = inventoryItemId,
            quantityBefore = auditLog.quantityBefore,
            quantityAfter = auditLog.quantityAfter,
            changeAmount = auditLog.changeAmount,
            reason = auditLog.reason
        )
        val payloadJson = objectMapper.writeValueAsString(payload)
        val webhookEnabled = integration.webhookEnabled && !integration.webhookUrl.isNullOrBlank()

        posOutboundInventoryEventRepository.save(
            PosOutboundInventoryEvent(
                eventId = eventId,
                retailer = userRepository.getReferenceById(inventoryItem.retailerId ?: ""),
                product = product,
                inventoryItem = inventoryItem,
                sourceType = auditLog.sourceType,
                sourceId = auditLog.sourceId,
                gtin = product.gtin,
                quantityBefore = auditLog.quantityBefore,
                quantityAfter = auditLog.quantityAfter,
                changeAmount = auditLog.changeAmount,
                reason = auditLog.reason,
                payload = payloadJson,
                eventTime = auditLog.createdAt,
                status = if (webhookEnabled) PosOutboundEventStatus.PENDING else PosOutboundEventStatus.STORED,
                nextRetryAt = if (webhookEnabled) LocalDateTime.now() else null
            )
        )
    }

    @Transactional(readOnly = true)
    fun getRetailerEvent(retailerId: String, eventId: String): PosOutboundInventoryEventDTO? {
        return posOutboundInventoryEventRepository.findByEventIdAndRetailer_Id(eventId, retailerId)?.toDto()
    }

    @Transactional(readOnly = true)
    fun getRetailerEvents(retailerId: String, since: String?, limit: Int): List<PosOutboundInventoryEventDTO> {
        val safeLimit = limit.coerceIn(1, 200)
        val events = parseSince(since)?.let { value ->
            posOutboundInventoryEventRepository.findTop200ByRetailer_IdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(
                retailerId = retailerId,
                eventTime = value
            )
        } ?: posOutboundInventoryEventRepository.findTop200ByRetailer_IdOrderByEventTimeDesc(retailerId)
        return events.take(safeLimit).map { it.toDto() }
    }

    @Transactional
    fun processPendingWebhookEvents() {
        val due = posOutboundInventoryEventRepository.findTop100ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
            status = PosOutboundEventStatus.PENDING,
            nextRetryAt = LocalDateTime.now()
        )
        due.forEach { event ->
            dispatchEvent(event)
        }
    }

    private fun dispatchEvent(event: PosOutboundInventoryEvent) {
        val integration = posIntegrationRepository.findByRetailer_Id(event.retailerId)
        if (integration == null) {
            markDeadLetter(event, "POS integration no longer exists")
            return
        }
        if (!integration.webhookEnabled || integration.webhookUrl.isNullOrBlank()) {
            event.status = PosOutboundEventStatus.STORED
            event.nextRetryAt = null
            event.updatedAt = LocalDateTime.now()
            event.lastError = "Webhook disabled"
            posOutboundInventoryEventRepository.save(event)
            return
        }

        val url = integration.webhookUrl!!.trim()
        if (url.isEmpty()) {
            event.status = PosOutboundEventStatus.STORED
            event.nextRetryAt = null
            event.updatedAt = LocalDateTime.now()
            event.lastError = "Webhook URL is blank"
            posOutboundInventoryEventRepository.save(event)
            return
        }

        val now = LocalDateTime.now()
        val timestamp = OffsetDateTime.now().toEpochSecond().toString()
        val signature = hmacSha256(integration.webhookSecret.orEmpty(), "$timestamp.${event.payload}")
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(requestTimeoutSeconds.toLong()))
            .header("Content-Type", "application/json")
            .header("X-Webhook-Event", event.eventType)
            .header("X-Webhook-Id", event.eventId)
            .header("X-Webhook-Timestamp", timestamp)
            .header("X-Webhook-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(event.payload, StandardCharsets.UTF_8))
            .build()

        try {
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            if (response.statusCode() in 200..299) {
                event.status = PosOutboundEventStatus.SENT
                event.deliveredAt = now
                event.lastAttemptAt = now
                event.nextRetryAt = null
                event.lastError = null
                event.updatedAt = now
                posOutboundInventoryEventRepository.save(event)
                return
            }
            scheduleRetry(event, "HTTP ${response.statusCode()}: ${response.body().take(500)}")
        } catch (ex: Exception) {
            scheduleRetry(event, ex.message ?: "Unknown webhook delivery failure")
        }
    }

    private fun scheduleRetry(event: PosOutboundInventoryEvent, error: String) {
        val now = LocalDateTime.now()
        val nextAttempt = event.attemptCount + 1
        event.attemptCount = nextAttempt
        event.lastAttemptAt = now
        event.lastError = error
        event.updatedAt = now

        if (nextAttempt >= maxRetries) {
            event.status = PosOutboundEventStatus.DEAD_LETTER
            event.nextRetryAt = null
            posOutboundInventoryEventRepository.save(event)
            return
        }

        event.status = PosOutboundEventStatus.PENDING
        val multiplier = 1L shl min(nextAttempt - 1, 10)
        val delayMs = min(retryBaseDelayMs * multiplier, retryMaxDelayMs)
        event.nextRetryAt = now.plusNanos(delayMs * 1_000_000)
        posOutboundInventoryEventRepository.save(event)
    }

    private fun markDeadLetter(event: PosOutboundInventoryEvent, reason: String) {
        event.status = PosOutboundEventStatus.DEAD_LETTER
        event.nextRetryAt = null
        event.lastError = reason
        event.updatedAt = LocalDateTime.now()
        posOutboundInventoryEventRepository.save(event)
    }

    private fun shouldPublish(sourceType: InventoryAuditSourceType): Boolean {
        return sourceType == InventoryAuditSourceType.ORDER || sourceType == InventoryAuditSourceType.RESTOCK || sourceType == InventoryAuditSourceType.MANUAL
    }

    private fun hmacSha256(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun parseSince(value: String?): LocalDateTime? {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { LocalDateTime.parse(normalized) }
            .recoverCatching { OffsetDateTime.parse(normalized).toLocalDateTime() }
            .getOrElse { throw IllegalArgumentException("Invalid since filter: $normalized") }
    }

    private fun PosOutboundInventoryEvent.toDto(): PosOutboundInventoryEventDTO {
        return PosOutboundInventoryEventDTO(
            eventId = this.eventId,
            eventType = this.eventType,
            status = this.status,
            eventTime = this.eventTime.toString(),
            sourceType = this.sourceType,
            sourceId = this.sourceId,
            gtin = this.gtin,
            productId = this.productId,
            inventoryItemId = this.inventoryItemId,
            quantityBefore = this.quantityBefore,
            quantityAfter = this.quantityAfter,
            changeAmount = this.changeAmount,
            reason = this.reason
        )
    }
}
