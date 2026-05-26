package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Notification
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationType

interface NotificationRepository : JpaRepository<Notification, String> {
    fun findByRecipient_Id(recipientId: String): List<Notification>
    fun findByRecipient_IdOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun countByRecipient_IdAndReadFalse(recipientId: String): Long
    fun findByChannel(channel: NotificationChannel): List<Notification>
    fun findByType(type: NotificationType): List<Notification>
}
