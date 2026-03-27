package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.model.NotificationPreference

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, String> {
    fun findByUserId(userId: String): List<NotificationPreference>

    fun findByUserIdAndPreferenceKeyAndChannel(
        userId: String,
        preferenceKey: NotificationPreferenceKey,
        channel: NotificationChannel
    ): NotificationPreference?
}
