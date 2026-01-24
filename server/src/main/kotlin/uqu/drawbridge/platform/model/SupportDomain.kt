package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime
import uqu.drawbridge.platform.TicketStatus
import uqu.drawbridge.platform.NotificationChannel



@Entity
@Table(name = "support_tickets")
open class SupportTicket(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var userId: String,

    @Column(nullable = false)
    open var subject: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: TicketStatus,

    @Column(nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "ticket_id", nullable = false)
    open var chats: MutableList<SupportTicketChat> = mutableListOf()
)

@Entity
@Table(name = "support_ticket_chats")
open class SupportTicketChat(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var chatId: String? = null,

    @Column(name = "ticket_id", insertable = false, updatable = false, nullable = false)
    open var ticketId: String? = null,

    @Column(nullable = true)
    open var adminId: String? = null, // Null if message is from ticket user, otherwise this admin id

    @Column(nullable = false, columnDefinition = "TEXT")
    open var message: String,

    @Column(nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "notifications")
open class Notification(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var recipientId: String,

    @Column(nullable = false)
    open var type: String,

    @Column(nullable = false)
    open var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var channel: NotificationChannel,

    @Column(nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)