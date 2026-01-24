package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.SupportTicket
import uqu.drawbridge.platform.TicketStatus

interface SupportTicketRepository : JpaRepository<SupportTicket, String> {
    fun findByUserId(userId: String): List<SupportTicket>
    fun findByStatus(status: TicketStatus): List<SupportTicket>
    fun findByUserIdAndStatus(userId: String, status: TicketStatus): List<SupportTicket>
}
