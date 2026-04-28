package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.SupportTicket

interface SupportTicketRepository : JpaRepository<SupportTicket, Long> {
    fun findAllByUserIdOrderByUpdatedAtDesc(userId: String): List<SupportTicket>
    fun findByIdAndUserId(id: Long, userId: String): SupportTicket?
    fun findTopByTicketNumberStartingWithOrderByTicketNumberDesc(prefix: String): SupportTicket?
    fun existsByTicketNumber(ticketNumber: String): Boolean
}
