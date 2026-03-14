package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class ScheduleType {
    THRESHOLD_BASED,
    DAILY,
    WEEKLY,
    MONTHLY,
    INTERVAL_DAYS
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class InventoryStatus {
    LOW_STOCK,
    OPTIMAL,
    OUT_OF_STOCK
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class InventoryItemDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val currentStock: Int,
    val autoRestock: Boolean,
    val autoOrderConfig: AutoOrderConfigDTO?,
    val status: InventoryStatus,
    val supplier: String,
    val lastRestocked: String?, // LocalDateTime -> String (ISO 8601)
    val reorderQuantity: Int?
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
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

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CreateInventoryItemRequest(
    val productId: String, // Long -> String
    val retailerId: String, // Long -> String
    val currentStock: Int,
    val minThreshold: Int,
    val autoRestock: Boolean
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class UpdateAutoOrderConfigRequest(
    val enabled: Boolean,
    val minThreshold: Int,
    val reorderQuantity: Int,
    val scheduleType: ScheduleType,
    val intervalDays: Int?,
    val dayOfWeek: String?,
    val dayOfMonth: String?
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class PosScanRequest(
    val retailerId: String,
    val gtin: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class PosScanResponse(
    val productName: String,
    val newStock: Int,
    val message: String
)
