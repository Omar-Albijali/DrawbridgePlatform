package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Notification
import uqu.drawbridge.platform.NotificationChannel

interface NotificationRepository : JpaRepository<Notification, String> {
    fun findByRecipientId(recipientId: String): List<Notification>
    fun findByRecipientIdOrderByCreatedAtDesc(recipientId: String): List<Notification>
    fun findByChannel(channel: NotificationChannel): List<Notification>
    fun findByType(type: String): List<Notification>
}
