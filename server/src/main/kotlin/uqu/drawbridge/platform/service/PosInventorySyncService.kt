package uqu.drawbridge.platform.service

import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.dto.PosInventoryChangeRequest
import uqu.drawbridge.platform.dto.PosInventoryChangeResponse
import uqu.drawbridge.platform.dto.PosInventoryChangeType
import uqu.drawbridge.platform.model.InventoryAuditSourceType
import uqu.drawbridge.platform.model.PosEventReceipt
import uqu.drawbridge.platform.model.PosEventReceiptStatus
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.PosEventReceiptRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.validation.RequestValidation

@Service
class PosInventorySyncService(
    private val productRepository: ProductRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val posEventReceiptRepository: PosEventReceiptRepository,
    private val inventoryService: InventoryService
) {
    private val logger = LoggerFactory.getLogger(PosInventorySyncService::class.java)

    @Transactional
    fun applyIncomingInventoryChange(
        authenticatedRetailerId: String,
        request: PosInventoryChangeRequest
    ): PosInventoryChangeResponse {
        val eventId = RequestValidation.requireNotBlank(request.eventId, "eventId")
        val retailerId = RequestValidation.requireNotBlank(request.retailerId, "retailerId")
        val gtin = RequestValidation.requireNotBlank(request.gtin, "gtin")
        require(retailerId == authenticatedRetailerId) { "retailerId does not match API key scope" }

        val eventType = "inventory.changed"
        val receipt = tryStartReceipt(retailerId, eventId, eventType)
            ?: return PosInventoryChangeResponse(
                eventId = eventId,
                alreadyProcessed = true,
                gtin = gtin
            )

        try {
            val product = productRepository.findByGtin(gtin)
                ?: throw NoSuchElementException("No product found for GTIN: $gtin")
            val inventoryItem = inventoryItemRepository.findByRetailerIdAndProductId(retailerId, product.id.orEmpty())
                ?: throw NoSuchElementException("No inventory entry found for retailer and GTIN")
            val inventoryItemId = inventoryItem.id ?: throw IllegalStateException("Inventory item has no id")

            val previousQuantity = inventoryItem.currentQuantity
            val updated = when (request.changeType) {
                PosInventoryChangeType.DELTA -> {
                    val delta = request.quantityDelta
                        ?: throw IllegalArgumentException("quantityDelta is required for DELTA changes")
                    val nextQuantity = previousQuantity + delta
                    require(nextQuantity >= 0) { "Resulting inventory cannot be negative" }
                    inventoryService.adjustQuantity(
                        id = inventoryItemId,
                        adjustment = delta,
                        sourceType = InventoryAuditSourceType.POS,
                        sourceId = eventId,
                        reason = request.reason ?: "POS inventory delta sync"
                    )
                }

                PosInventoryChangeType.SET -> {
                    val quantityAfter = request.quantityAfter
                        ?: throw IllegalArgumentException("quantityAfter is required for SET changes")
                    RequestValidation.requireNonNegative(quantityAfter, "quantityAfter")
                    inventoryService.updateQuantity(
                        id = inventoryItemId,
                        newQuantity = quantityAfter,
                        sourceType = InventoryAuditSourceType.POS,
                        sourceId = eventId,
                        reason = request.reason ?: "POS inventory absolute sync"
                    )
                }
            } ?: throw IllegalStateException("Failed to update inventory item")

            receipt.status = PosEventReceiptStatus.PROCESSED
            receipt.processedAt = LocalDateTime.now()
            posEventReceiptRepository.save(receipt)

            return PosInventoryChangeResponse(
                eventId = eventId,
                alreadyProcessed = false,
                inventoryItemId = updated.id,
                productId = product.id,
                gtin = gtin,
                quantityBefore = previousQuantity,
                quantityAfter = updated.currentQuantity
            )
        } catch (ex: Exception) {
            logger.warn("Failed POS inventory sync for retailer={} event={}", retailerId, eventId, ex)
            receipt.id?.let { posEventReceiptRepository.deleteById(it) }
            throw ex
        }
    }

    private fun tryStartReceipt(retailerId: String, eventId: String, eventType: String): PosEventReceipt? {
        if (posEventReceiptRepository.existsByRetailerIdAndEventIdAndEventType(retailerId, eventId, eventType)) {
            return null
        }
        return try {
            posEventReceiptRepository.saveAndFlush(
                PosEventReceipt(
                    retailerId = retailerId,
                    eventId = eventId,
                    eventType = eventType,
                    status = PosEventReceiptStatus.PROCESSING,
                    processedAt = LocalDateTime.now()
                )
            )
        } catch (_: DataIntegrityViolationException) {
            null
        }
    }
}
