package uqu.drawbridge.platform.service

import java.nio.charset.StandardCharsets
import java.security.Security
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uqu.drawbridge.platform.model.WebPushSubscription

@Service
class WebPushService(
    @Value("\${push.vapid.subject}") private val vapidSubject: String,
    @Value("\${push.vapid.public-key}") private val vapidPublicKey: String,
    @Value("\${push.vapid.private-key}") private val vapidPrivateKey: String
) {
    private val log = LoggerFactory.getLogger(WebPushService::class.java)

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private fun isConfigured(): Boolean =
        vapidPublicKey.isNotBlank() && vapidPrivateKey.isNotBlank() && vapidSubject.isNotBlank()

    fun publicKey(): String? = vapidPublicKey.takeIf { it.isNotBlank() }

    fun send(
        subscription: WebPushSubscription,
        title: String,
        message: String,
        notificationId: String? = null,
        deepLink: String
    ): Boolean {
        if (!isConfigured()) {
            return false
        }

        val payload = buildPayload(title, message, notificationId, deepLink)
        return try {
            val pushService = PushService(vapidPublicKey, vapidPrivateKey, vapidSubject)
            val notification = Notification(
                subscription.endpoint,
                subscription.p256dh,
                subscription.auth,
                payload.toByteArray(StandardCharsets.UTF_8)
            )
            pushService.send(notification)
            true
        } catch (ex: Exception) {
            log.warn("Failed to send web push notification for subscription {}", subscription.id ?: "unknown", ex)
            false
        }
    }

    private fun buildPayload(title: String, message: String, notificationId: String?, deepLink: String): String {
        val escapedTitle = title.replace("\"", "\\\"")
        val escapedMessage = message.replace("\"", "\\\"")
        val escapedId = (notificationId ?: "").replace("\"", "\\\"")
        val escapedDeepLink = deepLink.replace("\"", "\\\"")
        return "{\"title\":\"$escapedTitle\",\"body\":\"$escapedMessage\",\"notificationId\":\"$escapedId\",\"deepLink\":\"$escapedDeepLink\"}"
    }
}
