package uqu.drawbridge.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.SupportTicketCategory
import uqu.drawbridge.platform.SupportTicketStatus

@Entity
@Table(
    name = "support_ticket_requests",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_support_ticket_requests_ticket_number",
            columnNames = ["ticket_number"]
        )
    ]
)
open class SupportTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    @Column(name = "ticket_number", nullable = false, unique = true)
    open var ticketNumber: String,

    @Column(name = "user_id", nullable = false)
    open var userId: String,

    @Column(nullable = false)
    open var subject: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var category: SupportTicketCategory,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var description: String,

    @Column(name = "attachment_url")
    open var attachmentUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: SupportTicketStatus = SupportTicketStatus.OPEN,

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}

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
