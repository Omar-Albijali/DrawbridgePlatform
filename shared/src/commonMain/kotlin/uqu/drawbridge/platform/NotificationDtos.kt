@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
enum class NotificationChannel {
    SYSTEM,
    SMS,
    EMAIL,
    PUSH
}

@JsExport
@Serializable
enum class NotificationType {
    ORDER,
    STOCK,
    PAYMENT,
    SYSTEM
}

@JsExport
@Serializable
enum class NotificationEventKey {
    ORDER_CREATED,
    ORDER_STATUS_UPDATED,
    LOW_STOCK_ALERT,
    AUTO_RESTOCK_TRIGGERED,
    PAYMENT_STATUS_UPDATED,
    SUPPORT_TICKET_OPENED,
    SUPPORT_MESSAGE_ADDED
}

@JsExport
@Serializable
enum class NotificationEntityType {
    ORDER,
    INVENTORY_ITEM,
    PAYMENT,
    SUPPORT_TICKET,
    SYSTEM
}

@JsExport
@Serializable
enum class NotificationPreferenceKey {
    ORDER_CONFIRMATION,
    SHIPPING_STATUS,
    LOW_STOCK_WARNING,
    AUTO_RESTOCK_CONFIRMATION,
    NEW_WHOLESALERS,
    PAYMENT_STATUS,
    SUPPORT_UPDATES
}

@JsExport
@Serializable
data class NotificationDTO(
    val id: String, // Long -> String for JS safety
    val type: NotificationType,
    val eventKey: NotificationEventKey,
    val entityType: NotificationEntityType,
    val entityId: String?,
    val deepLink: String,
    val title: String,
    val message: String,
    val time: String,
    val read: Boolean
)

@JsExport
@Serializable
data class NotificationPreferenceDTO(
    val userId: String,
    val preferenceKey: NotificationPreferenceKey,
    val channel: NotificationChannel,
    val enabled: Boolean
)

@JsExport
@Serializable
data class UpsertNotificationPreferenceRequest(
    val preferenceKey: NotificationPreferenceKey,
    val channel: NotificationChannel,
    val enabled: Boolean
)

@JsExport
@Serializable
data class UnreadCountDTO(
    val recipientId: String,
    val count: Int
)

@JsExport
@Serializable
data class WebPushSubscriptionDTO(
    val id: String,
    val userId: String,
    val endpoint: String,
    val p256dh: String,
    val auth: String,
    val userAgent: String?,
    val createdAt: String
)

@JsExport
@Serializable
data class RegisterWebPushSubscriptionRequest(
    val userId: String,
    val endpoint: String,
    val p256dh: String,
    val auth: String,
    val userAgent: String? = null
)
