package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class NotificationChannel {
    SYSTEM,
    SMS
}

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class NotificationType {
    ORDER,
    STOCK,
    PAYMENT,
    SYSTEM
}

@OptIn(ExperimentalJsExport::class)
@JsExport
data class NotificationDTO(
    val id: String, // Long -> String for JS safety
    val type: NotificationType,
    val message: String,
    val time: String,
    val read: Boolean
)
