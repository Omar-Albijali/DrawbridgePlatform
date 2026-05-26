package uqu.drawbridge.platform.dto

import uqu.drawbridge.platform.model.InventoryAuditChangeType
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.model.InventoryStockTargetType

data class InventoryAuditLogDTO(
    val id: String,
    val productId: String,
    val inventoryItemId: String?,
    val stockTargetType: InventoryStockTargetType,
    val changeType: InventoryAuditChangeType,
    val sourceType: InventoryAuditSourceType,
    val sourceId: String?,
    val quantityBefore: Int,
    val quantityAfter: Int,
    val changeAmount: Int,
    val changedBy: String,
    val reason: String?,
    val createdAt: String
)

data class InventoryAuditLogPageResponse(
    val items: List<InventoryAuditLogDTO>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)
