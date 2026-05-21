package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
class SupportTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "ticket_number", nullable = false, unique = true)
    var ticketNumber: String,

    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Column(nullable = false)
    var subject: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: SupportTicketCategory,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(name = "attachment_url")
    var attachmentUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SupportTicketStatus = SupportTicketStatus.OPEN,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null
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
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var recipientId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: NotificationType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var eventKey: NotificationEventKey,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var entityType: NotificationEntityType,

    @Column(nullable = true)
    var entityId: String? = null,

    @Column(nullable = false)
    var deepLink: String,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var channel: NotificationChannel,

    @Column(name = "is_read", nullable = false, columnDefinition = "bit(1) not null default b'0'")
    var read: Boolean = false,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipientId", insertable = false, updatable = false)
    var recipient: User? = null
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
class NotificationPreference(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_key", nullable = false)
    var preferenceKey: NotificationPreferenceKey,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var channel: NotificationChannel,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null
)

@Entity
@Table(name = "web_push_subscriptions")
class WebPushSubscription(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    var endpoint: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var p256dh: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var auth: String,

    @Column(nullable = true)
    var userAgent: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    var user: User? = null
)
