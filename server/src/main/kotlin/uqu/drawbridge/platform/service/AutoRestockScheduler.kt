package uqu.drawbridge.platform.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AutoRestockScheduler(
    private val inventoryService: InventoryService
) {
    @Scheduled(fixedDelayString = "\${inventory.auto-restock.processor-delay-ms:60000}")
    fun processDueAutoOrders() {
        inventoryService.processDueAutoOrders()
    }
}
