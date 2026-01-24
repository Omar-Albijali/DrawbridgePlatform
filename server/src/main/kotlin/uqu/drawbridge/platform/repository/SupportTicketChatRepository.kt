package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.SupportTicketChat

interface SupportTicketChatRepository : JpaRepository<SupportTicketChat, String> {
    fun findByTicketIdOrderByCreatedAtAsc(ticketId: String): List<SupportTicketChat>
    fun findByAdminIdNotNull(): List<SupportTicketChat>
    fun findByAdminIdIsNull(): List<SupportTicketChat>
}
