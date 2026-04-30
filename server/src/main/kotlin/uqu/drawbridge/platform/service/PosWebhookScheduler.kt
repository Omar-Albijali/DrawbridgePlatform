package uqu.drawbridge.platform.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PosWebhookScheduler(
    private val posOutboundInventoryEventService: PosOutboundInventoryEventService
) {
    @Scheduled(fixedDelayString = "\${pos.webhook.processor-delay-ms:5000}")
    fun processOutboundEvents() {
        posOutboundInventoryEventService.processPendingWebhookEvents()
    }
}
