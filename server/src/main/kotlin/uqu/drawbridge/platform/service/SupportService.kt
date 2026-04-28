package uqu.drawbridge.platform.service

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.SupportTicketCategory
import uqu.drawbridge.platform.SupportTicketDTO
import uqu.drawbridge.platform.SupportTicketStatus
import uqu.drawbridge.platform.model.SupportTicket
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.SupportTicketRepository

@Service
class SupportService(
    private val supportTicketRepository: SupportTicketRepository,
    private val fileStorageService: FileStorageService,
    private val notificationService: NotificationService,
    private val emailService: EmailService
) {
    private val log = LoggerFactory.getLogger(SupportService::class.java)
    private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
    private val supportEmailRecipient = "thameralsh790@gmail.com"

    @Transactional
    fun createTicket(
        user: User,
        subject: String,
        category: SupportTicketCategory,
        description: String,
        attachment: MultipartFile?
    ): SupportTicketDTO {
        val normalizedSubject = subject.trim()
        val normalizedDescription = description.trim()

        require(normalizedSubject.isNotBlank()) { "Subject is required" }
        require(normalizedDescription.isNotBlank()) { "Description is required" }

        val attachmentUrl = attachment
            ?.takeIf { !it.isEmpty }
            ?.let { uploadedFile ->
                val relativePath = fileStorageService.storeFile(uploadedFile, "support")
                fileStorageService.getFileUrl(relativePath)
            }

        val ticket = SupportTicket(
            ticketNumber = generateTicketNumber(),
            userId = requireNotNull(user.id) { "User ID is required" },
            subject = normalizedSubject,
            category = category,
            description = normalizedDescription,
            attachmentUrl = attachmentUrl,
            status = SupportTicketStatus.OPEN
        )

        val saved = supportTicketRepository.save(ticket)

        runCatching {
            notificationService.sendEventNotification(
                recipientId = user.id!!,
                type = NotificationType.SYSTEM,
                eventKey = NotificationEventKey.SUPPORT_TICKET_OPENED,
                entityType = NotificationEntityType.SUPPORT_TICKET,
                entityId = saved.id?.toString(),
                preferenceKey = NotificationPreferenceKey.SUPPORT_UPDATES,
                title = "Support ticket received",
                message = "Ticket ${saved.ticketNumber} has been created successfully.",
                deepLink = "/support"
            )
        }.onFailure { ex ->
            log.warn("Failed to create in-app notification for support ticket {}", saved.ticketNumber, ex)
        }

        runCatching {
            emailService.sendSupportTicketEmail(
                toEmail = supportEmailRecipient,
                ticketNumber = saved.ticketNumber,
                subject = saved.subject,
                category = saved.category.name,
                description = saved.description,
                userEmail = user.email,
                userId = user.id!!,
                attachmentUrl = saved.attachmentUrl
            )
        }.onFailure { ex ->
            log.warn("Support email delivery failed for ticket {}", saved.ticketNumber, ex)
        }

        return saved.toDTO()
    }

    @Transactional(readOnly = true)
    fun getMyTickets(userId: String): List<SupportTicketDTO> {
        return supportTicketRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).map { it.toDTO() }
    }

    @Transactional(readOnly = true)
    fun getTicketById(id: Long, userId: String): SupportTicketDTO {
        val ticket = supportTicketRepository.findByIdAndUserId(id, userId)
            ?: throw NoSuchElementException("Support ticket not found")
        return ticket.toDTO()
    }

    private fun generateTicketNumber(): String {
        val prefix = "TCK-${LocalDate.now().format(dateFormatter)}-"
        val lastSequence = supportTicketRepository
            .findTopByTicketNumberStartingWithOrderByTicketNumberDesc(prefix)
            ?.ticketNumber
            ?.substringAfterLast("-")
            ?.toIntOrNull()
            ?: 0

        var nextSequence = lastSequence + 1
        var candidate = prefix + nextSequence.toString().padStart(4, '0')

        while (supportTicketRepository.existsByTicketNumber(candidate)) {
            nextSequence += 1
            candidate = prefix + nextSequence.toString().padStart(4, '0')
        }

        return candidate
    }

    private fun SupportTicket.toDTO() = SupportTicketDTO(
        id = this.id?.toString() ?: "",
        ticketNumber = this.ticketNumber,
        userId = this.userId,
        subject = this.subject,
        category = this.category,
        description = this.description,
        attachmentUrl = this.attachmentUrl,
        status = this.status,
        createdAt = this.createdAt.toString(),
        updatedAt = this.updatedAt.toString()
    )
}
