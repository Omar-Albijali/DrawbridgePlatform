package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.WebPushSubscription

interface WebPushSubscriptionRepository : JpaRepository<WebPushSubscription, String> {
    fun findByUser_Id(userId: String): List<WebPushSubscription>
    fun findByEndpoint(endpoint: String): WebPushSubscription?
    fun deleteByEndpoint(endpoint: String)
}
