package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class SupportTicketCategory {
    ORDER,
    POS,
    PAYMENT,
    OTHER
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class SupportTicketStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class SupportTicketDTO(
    val id: String,
    val ticketNumber: String,
    val userId: String,
    val subject: String,
    val category: SupportTicketCategory,
    val description: String,
    val attachmentUrl: String?,
    val status: SupportTicketStatus,
    val createdAt: String,
    val updatedAt: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CreateTicketRequest(
    val subject: String,
    val category: SupportTicketCategory,
    val description: String
)
