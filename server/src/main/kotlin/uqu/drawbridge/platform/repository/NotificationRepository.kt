package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Notification
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationType

interface NotificationRepository : JpaRepository<Notification, String> {
    fun findByRecipientId(recipientId: String): List<Notification>
    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun findByRecipientIdAndReadFalseOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun countByRecipientIdAndReadFalse(recipientId: String): Long
    fun findByChannel(channel: NotificationChannel): List<Notification>
    fun findByType(type: NotificationType): List<Notification>
}
