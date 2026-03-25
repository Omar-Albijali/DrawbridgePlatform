package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping("/recipient/{recipientId}")
    fun getNotifications(@PathVariable recipientId: String): ResponseEntity<List<NotificationDTO>> =
        ResponseEntity.ok(notificationService.getNotificationsDTOByRecipient(recipientId))

    @GetMapping("/recipient/{recipientId}/unread-count")
    fun getUnreadCount(@PathVariable recipientId: String): ResponseEntity<UnreadCountDTO> =
        ResponseEntity.ok(notificationService.getUnreadCountDTO(recipientId))

    @PutMapping("/{id}/read")
    fun markNotificationRead(@PathVariable id: String): ResponseEntity<NotificationDTO> {
        val updated = notificationService.markNotificationReadDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    fun markAllNotificationsRead(@PathVariable recipientId: String): ResponseEntity<UnreadCountDTO> {
        notificationService.markAllNotificationsRead(recipientId)
        return ResponseEntity.ok(notificationService.getUnreadCountDTO(recipientId))
    }

    @GetMapping("/preferences/{userId}")
    fun getNotificationPreferences(@PathVariable userId: String): ResponseEntity<List<NotificationPreferenceDTO>> =
        ResponseEntity.ok(notificationService.getPreferencesDTO(userId))

    @PutMapping("/preferences/{userId}")
    fun upsertNotificationPreference(
        @PathVariable userId: String,
        @RequestBody request: UpsertNotificationPreferenceRequest
    ): ResponseEntity<NotificationPreferenceDTO> =
        ResponseEntity.ok(notificationService.upsertPreferenceDTO(userId, request))

    @GetMapping("/push-subscriptions/{userId}")
    fun getPushSubscriptions(@PathVariable userId: String): ResponseEntity<List<WebPushSubscriptionDTO>> =
        ResponseEntity.ok(notificationService.getPushSubscriptionsDTO(userId))

    @PostMapping("/push-subscriptions")
    fun registerPushSubscription(
        @RequestBody request: RegisterWebPushSubscriptionRequest
    ): ResponseEntity<WebPushSubscriptionDTO> =
        ResponseEntity.status(HttpStatus.CREATED).body(notificationService.registerPushSubscriptionDTO(request))

    @DeleteMapping("/push-subscriptions")
    fun unregisterPushSubscription(@RequestParam endpoint: String): ResponseEntity<Void> {
        return if (notificationService.unregisterPushSubscription(endpoint)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(@PathVariable id: String): ResponseEntity<Void> {
        return if (notificationService.deleteNotification(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
