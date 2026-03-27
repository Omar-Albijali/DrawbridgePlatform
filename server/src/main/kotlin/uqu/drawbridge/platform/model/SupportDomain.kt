package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime
import uqu.drawbridge.platform.TicketStatus
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationType



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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var type: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var eventKey: NotificationEventKey,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var entityType: NotificationEntityType,

    @Column(nullable = true)
    open var entityId: String? = null,

    @Column(nullable = false)
    open var deepLink: String,

    @Column(nullable = false)
    open var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var channel: NotificationChannel,

    @Column(name = "is_read", nullable = false, columnDefinition = "bit(1) not null default b'0'")
    open var read: Boolean = false,

    @Column(nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(
    name = "notification_preferences",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_notification_preference_user_key_channel",
            columnNames = ["user_id", "preference_key", "channel"]
        )
    ]
)
open class NotificationPreference(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(name = "user_id", nullable = false)
    open var userId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_key", nullable = false)
    open var preferenceKey: NotificationPreferenceKey,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var channel: NotificationChannel,

    @Column(nullable = false)
    open var enabled: Boolean = true,

    @Column(nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "web_push_subscriptions")
open class WebPushSubscription(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(name = "user_id", nullable = false)
    open var userId: String,

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    open var endpoint: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var p256dh: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var auth: String,

    @Column(nullable = true)
    open var userAgent: String? = null,

    @Column(nullable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)