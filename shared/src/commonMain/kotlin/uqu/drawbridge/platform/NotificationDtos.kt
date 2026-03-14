package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class NotificationChannel {
    SYSTEM,
    SMS
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class NotificationType {
    ORDER,
    STOCK,
    PAYMENT,
    SYSTEM
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class NotificationDTO(
    val id: String, // Long -> String for JS safety
    val type: NotificationType,
    val message: String,
    val time: String,
    val read: Boolean
)
