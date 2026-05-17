package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uqu.drawbridge.platform.NotificationDTO
import uqu.drawbridge.platform.NotificationPreferenceDTO
import uqu.drawbridge.platform.RegisterWebPushSubscriptionRequest
import uqu.drawbridge.platform.UnreadCountDTO
import uqu.drawbridge.platform.UpsertNotificationPreferenceRequest
import uqu.drawbridge.platform.WebPushSubscriptionDTO
import uqu.drawbridge.platform.service.NotificationService
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService,
    private val userService: UserService
) {

    @GetMapping("/recipient/{recipientId}")
    fun getNotifications(authentication: Authentication, @PathVariable recipientId: String): ResponseEntity<List<NotificationDTO>> {
        requireCurrentUser(authentication, recipientId)
        return ResponseEntity.ok(notificationService.getNotificationsDTOByRecipient(recipientId))
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    fun getUnreadCount(authentication: Authentication, @PathVariable recipientId: String): ResponseEntity<UnreadCountDTO> {
        requireCurrentUser(authentication, recipientId)
        return ResponseEntity.ok(notificationService.getUnreadCountDTO(recipientId))
    }

    @PutMapping("/{id}/read")
    fun markNotificationRead(authentication: Authentication, @PathVariable id: String): ResponseEntity<NotificationDTO> {
        if (!requireNotificationOwner(authentication, id)) {
            return ResponseEntity.notFound().build()
        }
        val updated = notificationService.markNotificationReadDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    fun markAllNotificationsRead(authentication: Authentication, @PathVariable recipientId: String): ResponseEntity<UnreadCountDTO> {
        requireCurrentUser(authentication, recipientId)
        notificationService.markAllNotificationsRead(recipientId)
        return ResponseEntity.ok(notificationService.getUnreadCountDTO(recipientId))
    }

    @GetMapping("/preferences/{userId}")
    fun getNotificationPreferences(authentication: Authentication, @PathVariable userId: String): ResponseEntity<List<NotificationPreferenceDTO>> {
        requireCurrentUser(authentication, userId)
        return ResponseEntity.ok(notificationService.getPreferencesDTO(userId))
    }

    @PutMapping("/preferences/{userId}")
    fun upsertNotificationPreference(
        authentication: Authentication,
        @PathVariable userId: String,
        @RequestBody request: UpsertNotificationPreferenceRequest
    ): ResponseEntity<NotificationPreferenceDTO> {
        requireCurrentUser(authentication, userId)
        return ResponseEntity.ok(notificationService.upsertPreferenceDTO(userId, request))
    }

    @GetMapping("/push-subscriptions/{userId}")
    fun getPushSubscriptions(authentication: Authentication, @PathVariable userId: String): ResponseEntity<List<WebPushSubscriptionDTO>> {
        requireCurrentUser(authentication, userId)
        return ResponseEntity.ok(notificationService.getPushSubscriptionsDTO(userId))
    }

    @PostMapping("/push-subscriptions")
    fun registerPushSubscription(
        authentication: Authentication,
        @RequestBody request: RegisterWebPushSubscriptionRequest
    ): ResponseEntity<WebPushSubscriptionDTO> {
        requireCurrentUser(authentication, request.userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.registerPushSubscriptionDTO(request))
    }

    @DeleteMapping("/push-subscriptions")
    fun unregisterPushSubscription(authentication: Authentication, @RequestParam endpoint: String): ResponseEntity<Void> {
        val subscription = notificationService.getPushSubscriptionByEndpoint(endpoint)
            ?: return ResponseEntity.notFound().build()
        requireCurrentUser(authentication, subscription.userId)
        return if (notificationService.unregisterPushSubscription(endpoint)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(authentication: Authentication, @PathVariable id: String): ResponseEntity<Void> {
        if (!requireNotificationOwner(authentication, id)) {
            return ResponseEntity.notFound().build()
        }
        return if (notificationService.deleteNotification(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun currentUserId(authentication: Authentication): String =
        userService.getUserByEmail(authentication.name)?.id
            ?: throw IllegalArgumentException("User not found")

    private fun requireCurrentUser(authentication: Authentication, userId: String) {
        if (currentUserId(authentication) != userId) {
            throw AccessDeniedException("Cannot access another user's notifications")
        }
    }

    private fun requireNotificationOwner(authentication: Authentication, notificationId: String): Boolean {
        val notification = notificationService.getNotificationById(notificationId)
            ?: return false
        requireCurrentUser(authentication, notification.recipientId)
        return true
    }
}
