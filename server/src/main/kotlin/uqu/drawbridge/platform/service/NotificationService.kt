package uqu.drawbridge.platform.service

import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationDTO
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceDTO
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.RegisterWebPushSubscriptionRequest
import uqu.drawbridge.platform.UnreadCountDTO
import uqu.drawbridge.platform.UpsertNotificationPreferenceRequest
import uqu.drawbridge.platform.WebPushSubscriptionDTO
import uqu.drawbridge.platform.model.Notification
import uqu.drawbridge.platform.model.NotificationPreference
import uqu.drawbridge.platform.model.WebPushSubscription
import uqu.drawbridge.platform.repository.NotificationPreferenceRepository
import uqu.drawbridge.platform.repository.NotificationRepository
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.repository.WebPushSubscriptionRepository

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationPreferenceRepository: NotificationPreferenceRepository,
    private val webPushSubscriptionRepository: WebPushSubscriptionRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val webPushService: WebPushService,
    private val smsService: SmsService,
    @Qualifier("notificationTaskExecutor") private val notificationTaskExecutor: TaskExecutor
) {
    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    fun getAllNotifications(): List<Notification> {
        return notificationRepository.findAll()
    }

    fun getNotificationById(id: String): Notification? {
        return notificationRepository.findById(id).orElse(null)
    }

    fun getNotificationsByRecipient(recipientId: String): List<Notification> {
        return notificationRepository.findByRecipient_IdOrderByCreatedAtDesc(recipientId)
    }

    fun getUnreadCount(recipientId: String): Long {
        return notificationRepository.countByRecipient_IdAndReadFalse(recipientId)
    }

    fun getNotificationsByChannel(channel: NotificationChannel): List<Notification> {
        return notificationRepository.findByChannel(channel)
    }

    fun getNotificationsByType(type: NotificationType): List<Notification> {
        return notificationRepository.findByType(type)
    }

    @Transactional
    fun sendEventNotification(
        recipientId: String,
        type: NotificationType,
        eventKey: NotificationEventKey,
        entityType: NotificationEntityType,
        entityId: String?,
        preferenceKey: NotificationPreferenceKey,
        title: String,
        message: String,
        deepLink: String
    ): Notification {
        val recipientRef = userRepository.getReferenceById(recipientId)
        val inApp = notificationRepository.save(Notification(
            recipient = recipientRef,
            type = type,
            eventKey = eventKey,
            entityType = entityType,
            entityId = entityId,
            deepLink = deepLink,
            title = title,
            message = message,
            channel = NotificationChannel.SYSTEM
        ))

        val recipient = userRepository.findById(recipientId).orElse(null)
        if (recipient != null) {
            if (isChannelEnabled(recipientId, preferenceKey, NotificationChannel.EMAIL)) {
                notificationTaskExecutor.execute {
                    runCatching {
                        emailService.sendNotificationEmail(
                            toEmail = recipient.email,
                            recipientName = recipient.representative.name,
                            subject = title,
                            title = title,
                            message = message
                        )
                    }.onFailure { ex ->
                        log.warn("Email notification delivery failed for user {}", recipientId, ex)
                    }
                }
            }

            if (isChannelEnabled(recipientId, preferenceKey, NotificationChannel.SMS)) {
                runCatching {
                    smsService.sendNotificationSms(
                        toNumber = recipient.phoneNumber,
                        title = title,
                        message = message
                    )
                }.onFailure { ex ->
                    log.warn("SMS notification delivery failed for user {}", recipientId, ex)
                }
            }

            if (isChannelEnabled(recipientId, preferenceKey, NotificationChannel.PUSH)) {
                val subscriptions = webPushSubscriptionRepository.findByUser_Id(recipientId)
                subscriptions.forEach { subscription ->
                    val sent = webPushService.send(subscription, title, message, inApp.id, deepLink)
                    if (!sent) {
                        log.debug("Skipped or failed web push send for subscription {}", subscription.id)
                    }
                }
            }
        }

        return inApp
    }

    @Transactional
    fun markNotificationRead(id: String): Notification? {
        val notification = notificationRepository.findById(id).orElse(null) ?: return null
        if (!notification.read) {
            notification.read = true
            return notificationRepository.save(notification)
        }
        return notification
    }

    @Transactional
    fun markAllNotificationsRead(recipientId: String): Int {
        val unread = notificationRepository.findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(recipientId)
        unread.forEach { it.read = true }
        notificationRepository.saveAll(unread)
        return unread.size
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
        val notifications = notificationRepository.findByRecipient_Id(recipientId)
        notificationRepository.deleteAll(notifications)
        return notifications.size
    }

    fun getPreferences(userId: String): List<NotificationPreference> {
        return notificationPreferenceRepository.findByUser_Id(userId)
    }

    @Transactional
    fun upsertPreference(userId: String, request: UpsertNotificationPreferenceRequest): NotificationPreference {
        val existing = notificationPreferenceRepository.findByUser_IdAndPreferenceKeyAndChannel(
            userId = userId,
            preferenceKey = request.preferenceKey,
            channel = request.channel
        )

        val preference = if (existing != null) {
            existing.enabled = request.enabled
            existing.updatedAt = LocalDateTime.now()
            existing
        } else {
            NotificationPreference(
                user = userRepository.getReferenceById(userId),
                preferenceKey = request.preferenceKey,
                channel = request.channel,
                enabled = request.enabled,
                updatedAt = LocalDateTime.now()
            )
        }

        return notificationPreferenceRepository.save(preference)
    }

    @Transactional
    fun registerPushSubscription(request: RegisterWebPushSubscriptionRequest): WebPushSubscription {
        val existing = webPushSubscriptionRepository.findByEndpoint(request.endpoint)
        val subscription = if (existing != null) {
            existing.user = userRepository.getReferenceById(request.userId)
            existing.p256dh = request.p256dh
            existing.auth = request.auth
            existing.userAgent = request.userAgent
            existing
        } else {
            WebPushSubscription(
                user = userRepository.getReferenceById(request.userId),
                endpoint = request.endpoint,
                p256dh = request.p256dh,
                auth = request.auth,
                userAgent = request.userAgent,
                createdAt = LocalDateTime.now()
            )
        }
        return webPushSubscriptionRepository.save(subscription)
    }

    @Transactional
    fun unregisterPushSubscription(endpoint: String): Boolean {
        val existing = webPushSubscriptionRepository.findByEndpoint(endpoint) ?: return false
        webPushSubscriptionRepository.delete(existing)
        return true
    }

    fun getPushSubscriptions(userId: String): List<WebPushSubscription> {
        return webPushSubscriptionRepository.findByUser_Id(userId)
    }

    fun getPushSubscriptionByEndpoint(endpoint: String): WebPushSubscription? {
        return webPushSubscriptionRepository.findByEndpoint(endpoint)
    }

    // ==================== DTO MAPPING ====================

    fun Notification.toDTO() = NotificationDTO(
        id = (this.id ?: ""),
        type = this.type,
        eventKey = this.eventKey,
        entityType = this.entityType,
        entityId = this.entityId,
        deepLink = this.deepLink,
        title = this.title,
        message = this.message,
        time = this.createdAt.toString(),
        read = this.read
    )

    fun NotificationPreference.toDTO() = NotificationPreferenceDTO(
        userId = this.userId,
        preferenceKey = this.preferenceKey,
        channel = this.channel,
        enabled = this.enabled
    )

    fun WebPushSubscription.toDTO() = WebPushSubscriptionDTO(
        id = (this.id ?: ""),
        userId = this.userId,
        endpoint = this.endpoint,
        p256dh = this.p256dh,
        auth = this.auth,
        userAgent = this.userAgent,
        createdAt = this.createdAt.toString()
    )

    // ==================== DTO-RETURNING METHODS ====================

    fun getNotificationsDTOByRecipient(recipientId: String): List<NotificationDTO> =
        getNotificationsByRecipient(recipientId).map { it.toDTO() }

    fun markNotificationReadDTO(id: String): NotificationDTO? =
        markNotificationRead(id)?.toDTO()

    fun getUnreadCountDTO(recipientId: String): UnreadCountDTO =
        UnreadCountDTO(recipientId = recipientId, count = getUnreadCount(recipientId).toInt())

    fun getPreferencesDTO(userId: String): List<NotificationPreferenceDTO> =
        getPreferences(userId).map { it.toDTO() }

    fun upsertPreferenceDTO(userId: String, request: UpsertNotificationPreferenceRequest): NotificationPreferenceDTO =
        upsertPreference(userId, request).toDTO()

    fun registerPushSubscriptionDTO(request: RegisterWebPushSubscriptionRequest): WebPushSubscriptionDTO =
        registerPushSubscription(request).toDTO()

    fun getPushSubscriptionsDTO(userId: String): List<WebPushSubscriptionDTO> =
        getPushSubscriptions(userId).map { it.toDTO() }

    private fun isChannelEnabled(
        userId: String,
        preferenceKey: NotificationPreferenceKey,
        channel: NotificationChannel
    ): Boolean {
        val preference = notificationPreferenceRepository.findByUser_IdAndPreferenceKeyAndChannel(
            userId = userId,
            preferenceKey = preferenceKey,
            channel = channel
        )
        if (preference != null) {
            return preference.enabled
        }
        return channel != NotificationChannel.SMS
    }
}
