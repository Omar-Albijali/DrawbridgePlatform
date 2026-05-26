package uqu.drawbridge.platform.service

import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Base64
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.PosIntegrationApiKeyRotateResponse
import uqu.drawbridge.platform.PosIntegrationConfigDTO
import uqu.drawbridge.platform.PosIntegrationConfigUpdateRequest
import uqu.drawbridge.platform.PosIntegrationEventLogDTO
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.model.PosIntegration
import uqu.drawbridge.platform.model.PosIntegrationStatus
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.repository.InventoryAuditLogRepository
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.PosEventReceiptRepository
import uqu.drawbridge.platform.repository.PosIntegrationRepository
import uqu.drawbridge.platform.repository.PosOutboundInventoryEventRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.validation.RequestValidation

@Service
class PosIntegrationManagementService(
    private val userService: UserService,
    private val posIntegrationRepository: PosIntegrationRepository,
    private val posEventReceiptRepository: PosEventReceiptRepository,
    private val posOutboundInventoryEventRepository: PosOutboundInventoryEventRepository,
    private val inventoryAuditLogRepository: InventoryAuditLogRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val productRepository: ProductRepository,
    private val posApiKeyService: PosApiKeyService,
    private val userRepository: UserRepository
) {
    private val secureRandom = SecureRandom()

    @Transactional(readOnly = true)
    fun getCurrentRetailerConfig(authenticationName: String): PosIntegrationConfigDTO {
        val retailer = requireRetailer(authenticationName)
        val retailerId = retailer.id ?: throw IllegalStateException("User id missing")
        val integration = posIntegrationRepository.findByRetailer_Id(retailerId)
        return integration.toConfigDto(retailerId)
    }

    @Transactional
    fun updateCurrentRetailerConfig(
        authenticationName: String,
        request: PosIntegrationConfigUpdateRequest
    ): PosIntegrationConfigDTO {
        val retailer = requireRetailer(authenticationName)
        val retailerId = retailer.id ?: throw IllegalStateException("User id missing")
        val integration = posIntegrationRepository.findByRetailer_Id(retailerId)
            ?: throw IllegalArgumentException("Generate POS API key first")

        val normalizedStatus = parseStatus(request.status)
        val normalizedWebhookUrl = request.webhookUrl?.trim().orEmpty().ifBlank { null }
        val normalizedWebhookEnabled = request.webhookEnabled && !normalizedWebhookUrl.isNullOrBlank()

        if (request.webhookEnabled && normalizedWebhookUrl.isNullOrBlank()) {
            throw IllegalArgumentException("webhookUrl is required when webhookEnabled=true")
        }
        normalizedWebhookUrl?.let { validateWebhookUrl(it) }

        integration.status = normalizedStatus
        integration.webhookEnabled = normalizedWebhookEnabled
        integration.webhookUrl = normalizedWebhookUrl
        request.webhookSecret?.ifBlank { null }?.let { secret ->
            integration.webhookSecret = secret.trim()
        }



        val updated = posIntegrationRepository.save(integration)
        return updated.toConfigDto(retailerId)
    }

    @Transactional
    fun rotateCurrentRetailerApiKey(authenticationName: String): PosIntegrationApiKeyRotateResponse {
        val retailer = requireRetailer(authenticationName)
        val retailerId = retailer.id ?: throw IllegalStateException("User id missing")

        val rawApiKey = generateRawApiKey()
        val apiKeyHash = posApiKeyService.sha256Hex(rawApiKey)
        val apiKeyPrefix = rawApiKey.take(16)
        val now = LocalDateTime.now()

        val integration = posIntegrationRepository.findByRetailer_Id(retailerId)?.apply {
            this.apiKeyHash = apiKeyHash
            this.apiKeyPrefix = apiKeyPrefix
            this.rotatedAt = now
            this.status = PosIntegrationStatus.ACTIVE
        } ?: PosIntegration(
            retailer = userRepository.getReferenceById(retailerId),
            apiKeyHash = apiKeyHash,
            apiKeyPrefix = apiKeyPrefix,
            status = PosIntegrationStatus.ACTIVE,
            webhookEnabled = false,
            webhookUrl = null,
            webhookSecret = null,
            createdAt = now,
            rotatedAt = now
        )

        posIntegrationRepository.save(integration)
        return PosIntegrationApiKeyRotateResponse(
            retailerId = retailerId,
            apiKey = rawApiKey,
            apiKeyPrefix = apiKeyPrefix,
            rotatedAt = now.toString()
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentRetailerEventLogs(
        authenticationName: String,
        since: String?,
        limit: Int
    ): List<PosIntegrationEventLogDTO> {
        val retailer = requireRetailer(authenticationName)
        val retailerId = retailer.id ?: throw IllegalStateException("User id missing")
        val sinceFilter = parseSince(since)
        val safeLimit = limit.coerceIn(1, 200)

        val outboundLogs = posOutboundInventoryEventRepository
            .findTop200ByRetailer_IdOrderByEventTimeDesc(retailerId)
            .map { event ->
                PosIntegrationEventLogDTO(
                    direction = "OUTBOUND",
                    eventId = event.eventId,
                    eventType = event.eventType,
                    status = event.status.name,
                    eventTime = event.eventTime.toString(),
                    sourceType = event.sourceType.name,
                    sourceId = event.sourceId,
                    gtin = event.gtin,
                    productId = event.productId,
                    inventoryItemId = event.inventoryItemId,
                    quantityBefore = event.quantityBefore,
                    quantityAfter = event.quantityAfter,
                    changeAmount = event.changeAmount,
                    reason = event.reason,
                    attemptCount = event.attemptCount,
                    lastError = event.lastError
                )
            }

        val inboundLogs = posEventReceiptRepository
            .findTop200ByRetailer_IdOrderByProcessedAtDesc(retailerId)
            .map { receipt ->
                val auditLog = inventoryAuditLogRepository.findFirstBySourceTypeAndSourceIdOrderByCreatedAtDesc(
                    sourceType = InventoryAuditSourceType.POS,
                    sourceId = receipt.eventId
                )?.takeIf { audit ->
                    val inventoryItemId = audit.inventoryItemId ?: return@takeIf false
                    val inventoryItem = inventoryItemRepository.findById(inventoryItemId).orElse(null)
                    inventoryItem?.retailerId == retailerId
                }

                val product = auditLog?.productId?.let { logProductId ->
                    productRepository.findById(logProductId).orElse(null)
                }

                PosIntegrationEventLogDTO(
                    direction = "INBOUND",
                    eventId = receipt.eventId,
                    eventType = receipt.eventType,
                    status = receipt.status.name,
                    eventTime = receipt.processedAt.toString(),
                    sourceType = "POS",
                    sourceId = auditLog?.sourceId,
                    gtin = product?.gtin,
                    productId = auditLog?.productId,
                    inventoryItemId = auditLog?.inventoryItemId,
                    quantityBefore = auditLog?.quantityBefore,
                    quantityAfter = auditLog?.quantityAfter,
                    changeAmount = auditLog?.changeAmount,
                    reason = auditLog?.reason,
                    attemptCount = null,
                    lastError = null
                )
            }

        return (outboundLogs + inboundLogs)
            .asSequence()
            .filter { log ->
                sinceFilter == null || LocalDateTime.parse(log.eventTime) >= sinceFilter
            }
            .sortedByDescending { LocalDateTime.parse(it.eventTime) }
            .take(safeLimit)
            .toList()
    }

    private fun requireRetailer(authenticationName: String): uqu.drawbridge.platform.model.User {
        val user = userService.getUserByEmail(authenticationName)
            ?: throw NoSuchElementException("User not found")
        require(user.role == UserRole.RETAILER) { "POS integration is available for retailers only" }
        return user
    }

    private fun PosIntegration?.toConfigDto(retailerId: String): PosIntegrationConfigDTO {
        if (this == null) {
            return PosIntegrationConfigDTO(
                retailerId = retailerId,
                integrationExists = false,
                status = PosIntegrationStatus.DISABLED.name,
                apiKeyPrefix = null,
                webhookEnabled = false,
                webhookUrl = null,
                webhookSecretConfigured = false,
                createdAt = null,
                rotatedAt = null
            )
        }

        return PosIntegrationConfigDTO(
            retailerId = retailerId,
            integrationExists = true,
            status = this.status.name,
            apiKeyPrefix = this.apiKeyPrefix,
            webhookEnabled = this.webhookEnabled,
            webhookUrl = this.webhookUrl,
            webhookSecretConfigured = !this.webhookSecret.isNullOrBlank(),
            createdAt = this.createdAt.toString(),
            rotatedAt = this.rotatedAt?.toString()
        )
    }

    private fun parseStatus(value: String): PosIntegrationStatus {
        val normalized = RequestValidation.requireNotBlank(value, "status")
        return runCatching { PosIntegrationStatus.valueOf(normalized) }
            .getOrElse { throw IllegalArgumentException("Invalid status: $normalized") }
    }

    private fun parseSince(value: String?): LocalDateTime? {
        val normalized = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { LocalDateTime.parse(normalized) }
            .recoverCatching { OffsetDateTime.parse(normalized).toLocalDateTime() }
            .getOrElse { throw IllegalArgumentException("Invalid since filter: $normalized") }
    }

    private fun validateWebhookUrl(url: String) {
        val lower = url.lowercase()
        require(lower.startsWith("http://") || lower.startsWith("https://")) {
            "webhookUrl must start with http:// or https://"
        }
    }

    private fun generateRawApiKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return "pos_live_$token"
    }
}
