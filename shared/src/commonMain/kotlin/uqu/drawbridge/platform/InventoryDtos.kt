@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport

@JsExport
enum class ScheduleType {
    THRESHOLD_BASED,
    DAILY,
    WEEKLY,
    MONTHLY,
    INTERVAL_DAYS
}

@JsExport
enum class InventoryStatus {
    LOW_STOCK,
    OPTIMAL,
    OUT_OF_STOCK
}

@JsExport
data class InventoryItemDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val currentStock: Int,
    val autoRestock: Boolean,
    val autoOrderConfig: AutoOrderConfigDTO?,
    val status: InventoryStatus,
    val supplier: String,
    val lastRestocked: String?, // LocalDateTime -> String (ISO 8601)
    val reorderQuantity: Int?,
    val imageUrl: String?
)

@JsExport
data class AutoOrderConfigDTO(
    val enabled: Boolean,
    val minThreshold: Int,
    val reorderQuantity: Int,
    val scheduleType: ScheduleType,
    val intervalDays: Int?,
    val dayOfWeek: String?,
    val dayOfMonth: String?,
    val lastTriggeredAt: String?, // LocalDateTime -> String
    val nextScheduledAt: String?  // LocalDateTime -> String
)

@JsExport
data class CreateInventoryItemRequest(
    val productId: String, // Long -> String
    val retailerId: String, // Long -> String
    val currentStock: Int,
    val minThreshold: Int,
    val autoRestock: Boolean
)

@JsExport
data class UpdateAutoOrderConfigRequest(
    val enabled: Boolean,
    val minThreshold: Int,
    val reorderQuantity: Int,
    val scheduleType: ScheduleType,
    val intervalDays: Int?,
    val dayOfWeek: String?,
    val dayOfMonth: String?
)
