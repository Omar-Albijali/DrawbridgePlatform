package uqu.drawbridge.platform.service

import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.AddMessageRequest
import uqu.drawbridge.platform.CreateTicketRequest
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.SupportTicketChatDTO
import uqu.drawbridge.platform.SupportTicketDTO
import uqu.drawbridge.platform.TicketStatus
import uqu.drawbridge.platform.model.SupportTicket
import uqu.drawbridge.platform.model.SupportTicketChat
import uqu.drawbridge.platform.repository.AdminRepository
import uqu.drawbridge.platform.repository.SupportTicketChatRepository
import uqu.drawbridge.platform.repository.SupportTicketRepository

@Service
class SupportService(
    private val supportTicketRepository: SupportTicketRepository,
    private val supportTicketChatRepository: SupportTicketChatRepository,
    private val adminRepository: AdminRepository,
    private val notificationService: NotificationService
) {

    // ==================== SUPPORT TICKET OPERATIONS ====================

    fun getAllTickets(): List<SupportTicket> {
        return supportTicketRepository.findAll()
    }

    fun getTicketById(id: String): SupportTicket? {
        return supportTicketRepository.findById(id).orElse(null)
    }

    fun getTicketsByUserId(userId: String): List<SupportTicket> {
        return supportTicketRepository.findByUserId(userId)
    }

    fun getTicketsByStatus(status: TicketStatus): List<SupportTicket> {
        return supportTicketRepository.findByStatus(status)
    }

    fun getOpenTickets(): List<SupportTicket> {
        return supportTicketRepository.findByStatus(TicketStatus.OPEN)
    }

    fun getTicketsByUserAndStatus(userId: String, status: TicketStatus): List<SupportTicket> {
        return supportTicketRepository.findByUserIdAndStatus(userId, status)
    }

    @Transactional
    fun createTicket(ticket: SupportTicket): SupportTicket {
        ticket.status = TicketStatus.OPEN
        ticket.createdAt = LocalDateTime.now()
        return supportTicketRepository.save(ticket)
    }

    @Transactional
    fun createTicket(userId: String, subject: String, description: String): SupportTicket {
        val ticket = SupportTicket(
            userId = userId,
            subject = subject,
            description = description,
            status = TicketStatus.OPEN,
            createdAt = LocalDateTime.now()
        )
        val saved = supportTicketRepository.save(ticket)
        notificationService.sendEventNotification(
            recipientId = userId,
            type = NotificationType.SYSTEM,
            eventKey = NotificationEventKey.SUPPORT_TICKET_OPENED,
            entityType = NotificationEntityType.SUPPORT_TICKET,
            entityId = saved.id,
            preferenceKey = NotificationPreferenceKey.SUPPORT_UPDATES,
            title = "Support ticket opened",
            message = "Your support ticket '${saved.subject}' was created.",
            deepLink = "/support"
        )
        return saved
    }

    @Transactional
    fun closeTicket(id: String): SupportTicket? {
        val ticket = supportTicketRepository.findById(id).orElse(null) ?: return null
        ticket.status = TicketStatus.CLOSED
        return supportTicketRepository.save(ticket)
    }

    @Transactional
    fun reopenTicket(id: String): SupportTicket? {
        val ticket = supportTicketRepository.findById(id).orElse(null) ?: return null
        ticket.status = TicketStatus.OPEN
        return supportTicketRepository.save(ticket)
    }

    @Transactional
    fun deleteTicket(id: String): Boolean {
        return if (supportTicketRepository.existsById(id)) {
            supportTicketRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== SUPPORT TICKET CHAT OPERATIONS ====================

    fun getChatsByTicketId(ticketId: String): List<SupportTicketChat> {
        return supportTicketChatRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
    }

    @Transactional
    fun addUserMessage(ticketId: String, message: String): SupportTicketChat? {
        return createAndAddChat(ticketId, null, message)
    }

    @Transactional
    fun addAdminMessage(ticketId: String, adminId: String, message: String): SupportTicketChat? {
        // Verify admin exists
        adminRepository.findById(adminId).orElse(null) ?: return null
        return createAndAddChat(ticketId, adminId, message)
    }

    private fun createAndAddChat(ticketId: String, adminId: String?, message: String): SupportTicketChat? {
        val ticket = supportTicketRepository.findById(ticketId).orElse(null) ?: return null

        val chat = SupportTicketChat(
            adminId = adminId,
            message = message,
            createdAt = LocalDateTime.now()
        )
        ticket.chats.add(chat)
        val savedTicket = supportTicketRepository.save(ticket)
        notificationService.sendEventNotification(
            recipientId = ticket.userId,
            type = NotificationType.SYSTEM,
            eventKey = NotificationEventKey.SUPPORT_MESSAGE_ADDED,
            entityType = NotificationEntityType.SUPPORT_TICKET,
            entityId = ticket.id,
            preferenceKey = NotificationPreferenceKey.SUPPORT_UPDATES,
            title = "New support message",
            message = "There is a new message on ticket '${ticket.subject}'.",
            deepLink = "/support"
        )
        return savedTicket.chats.last()
    }

    // ==================== DTO MAPPING ====================

    fun SupportTicket.toDTO() = SupportTicketDTO(
        id = (this.id ?: ""),
        userId = this.userId,
        subject = this.subject,
        description = this.description,
        status = this.status,
        createdAt = this.createdAt.toString()
    )

    fun SupportTicketChat.toDTO() = SupportTicketChatDTO(
        id = (this.chatId ?: ""),
        ticketId = this.ticketId ?: "",
        adminId = this.adminId,
        message = this.message,
        isAdmin = this.adminId != null,
        createdAt = this.createdAt.toString()
    )

    // ==================== DTO-RETURNING METHODS ====================

    fun getAllTicketsDTO(): List<SupportTicketDTO> = getAllTickets().map { it.toDTO() }

    fun getTicketDTOById(id: String): SupportTicketDTO? = getTicketById(id)?.toDTO()

    fun getTicketsDTOByUserId(userId: String): List<SupportTicketDTO> = getTicketsByUserId(userId).map { it.toDTO() }

    fun createTicketDTO(request: CreateTicketRequest): SupportTicketDTO {
        return createTicket(
            request.userId,
            request.subject,
            request.description
        ).toDTO()
    }

    fun closeTicketDTO(id: String): SupportTicketDTO? = closeTicket(id)?.toDTO()

    fun getChatsDTOByTicketId(ticketId: String): List<SupportTicketChatDTO> =
        getChatsByTicketId(ticketId).map { it.toDTO() }

    fun addUserMessageDTO(ticketId: String, message: String): SupportTicketChatDTO? =
        addUserMessage(ticketId, message)?.toDTO()

    fun addAdminMessageDTO(ticketId: String, adminId: String, message: String): SupportTicketChatDTO? =
        addAdminMessage(ticketId, adminId, message)?.toDTO()

    fun addMessageDTO(ticketId: String, request: AddMessageRequest): SupportTicketChatDTO? {
        val adminIdStr = request.adminId
        return if (adminIdStr != null) {
            addAdminMessageDTO(ticketId, adminIdStr, request.message)
        } else {
            addUserMessageDTO(ticketId, request.message)
        }
    }
}
