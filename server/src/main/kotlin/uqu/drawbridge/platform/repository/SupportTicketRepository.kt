package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.SupportTicket

interface SupportTicketRepository : JpaRepository<SupportTicket, Long> {
    fun findAllByUser_IdOrderByUpdatedAtDesc(userId: String): List<SupportTicket>
    fun findByIdAndUser_Id(id: Long, userId: String): SupportTicket?
    fun findTopByTicketNumberStartingWithOrderByTicketNumberDesc(prefix: String): SupportTicket?
    fun existsByTicketNumber(ticketNumber: String): Boolean
}
