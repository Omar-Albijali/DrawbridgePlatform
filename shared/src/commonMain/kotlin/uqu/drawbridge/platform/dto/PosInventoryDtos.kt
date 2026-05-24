package uqu.drawbridge.platform.dto

import kotlinx.serialization.Serializable

@Serializable
enum class PosInventoryChangeType {
    DELTA,
    SET
}

@Serializable
enum class PosOutboundEventStatus {
    STORED,
    PENDING,
    SENT,
    DEAD_LETTER
}

@Serializable
enum class InventoryAuditSourceType {
    MANUAL,
    ORDER,
    RESTOCK,
    POS,
    SYSTEM
}

@Serializable
data class PosInventoryChangeRequest(
    val eventId: String,
    val eventTime: String? = null,
    val retailerId: String,
    val gtin: String,
    val changeType: PosInventoryChangeType,
    val quantityDelta: Int? = null,
    val quantityAfter: Int? = null,
    val reason: String? = null
)

@Serializable
data class PosInventoryChangeResponse(
    val eventId: String,
    val alreadyProcessed: Boolean,
    val inventoryItemId: String? = null,
    val productId: String? = null,
    val gtin: String,
    val quantityBefore: Int? = null,
    val quantityAfter: Int? = null,
    val productName: String? = null
)

@Serializable
data class PosOutboundInventoryEventDTO(
    val eventId: String,
    val eventType: String,
    val status: PosOutboundEventStatus,
    val eventTime: String,
    val sourceType: InventoryAuditSourceType,
    val sourceId: String?,
    val gtin: String,
    val productId: String,
    val inventoryItemId: String,
    val quantityBefore: Int,
    val quantityAfter: Int,
    val changeAmount: Int,
    val reason: String?
)
