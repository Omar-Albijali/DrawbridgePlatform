package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.NotificationRepository
import uqu.drawbridge.platform.repository.SupportTicketChatRepository
import uqu.drawbridge.platform.repository.SupportTicketRepository
import uqu.drawbridge.platform.repository.AdminRepository
import uqu.drawbridge.platform.*
import java.time.LocalDateTime

@Service
class SupportService(
    private val supportTicketRepository: SupportTicketRepository,
    private val supportTicketChatRepository: SupportTicketChatRepository,
    private val notificationRepository: NotificationRepository,
    private val adminRepository: AdminRepository
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
        return supportTicketRepository.save(ticket)
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
        return savedTicket.chats.last()
    }

    // ==================== NOTIFICATION OPERATIONS ====================

    fun getAllNotifications(): List<Notification> {
        return notificationRepository.findAll()
    }

    fun getNotificationById(id: String): Notification? {
        return notificationRepository.findById(id).orElse(null)
    }

    fun getNotificationsByRecipient(recipientId: String): List<Notification> {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
    }

    fun getNotificationsByChannel(channel: NotificationChannel): List<Notification> {
        return notificationRepository.findByChannel(channel)
    }

    fun getNotificationsByType(type: String): List<Notification> {
        return notificationRepository.findByType(type)
    }

    @Transactional
    fun createNotification(notification: Notification): Notification {
        notification.createdAt = LocalDateTime.now()
        return notificationRepository.save(notification)
    }

    @Transactional
    fun sendNotification(
        recipientId: String,
        type: String,
        title: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.SYSTEM
    ): Notification {
        val notification = Notification(
            recipientId = recipientId,
            type = type,
            title = title,
            message = message,
            channel = channel,
            createdAt = LocalDateTime.now()
        )
        return notificationRepository.save(notification)
    }

    @Transactional
    fun deleteNotification(id: String): Boolean {
        return if (notificationRepository.existsById(id)) {
            notificationRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    @Transactional
    fun deleteAllNotificationsForRecipient(recipientId: String): Int {
        val notifications = notificationRepository.findByRecipientId(recipientId)
        notificationRepository.deleteAll(notifications)
        return notifications.size
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

    fun getNotificationsDTOByRecipient(recipientId: String): List<NotificationDTO> = 
        getNotificationsByRecipient(recipientId).map { notification ->
            NotificationDTO(
                id = (notification.id ?: ""),
                type = NotificationType.valueOf(notification.type),
                message = notification.message,
                time = notification.createdAt.toString(),
                read = false // Notifications don't have read status in the entity
            )
        }
}
