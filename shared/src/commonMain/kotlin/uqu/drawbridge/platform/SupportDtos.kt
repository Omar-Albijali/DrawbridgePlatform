@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport

@JsExport
enum class SupportTicketCategory {
    ORDER,
    POS,
    PAYMENT,
    OTHER
}

@JsExport
enum class SupportTicketStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}

@JsExport
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

@JsExport
data class CreateTicketRequest(
    val subject: String,
    val category: SupportTicketCategory,
    val description: String
)
