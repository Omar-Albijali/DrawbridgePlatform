package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class TicketStatus {
    OPEN,
    CLOSED
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SupportTicketDTO(
    val id: String, // Long -> String for JS safety
    val userId: String, // Long -> String
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val createdAt: String // LocalDateTime -> String (ISO 8601)
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SupportTicketChatDTO(
    val id: String, // Long -> String for JS safety
    val ticketId: String, // Long -> String
    val adminId: String?, // Long? -> String?
    val message: String,
    val isAdmin: Boolean,
    val createdAt: String // LocalDateTime -> String (ISO 8601)
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CreateTicketRequest(
    val userId: String, // Long -> String
    val subject: String,
    val description: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class AddMessageRequest(
    val message: String,
    val adminId: String? = null // Long? -> String?
)
