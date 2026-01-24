package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.ScheduleType
import java.time.LocalDateTime

interface InventoryItemRepository : JpaRepository<InventoryItem, String> {
    fun findByRetailerId(retailerId: String): List<InventoryItem>
    fun findByProductId(productId: String): List<InventoryItem>
    fun findByRetailerIdAndProductId(retailerId: String, productId: String): InventoryItem?
    fun findByCurrentQuantityLessThanEqual(quantity: Int): List<InventoryItem>
    
    fun findByAutoOrderConfigEnabledTrue(): List<InventoryItem>
    fun findByAutoOrderConfigScheduleType(scheduleType: ScheduleType): List<InventoryItem>
    fun findByAutoOrderConfigNextScheduledAtBefore(dateTime: LocalDateTime): List<InventoryItem>
    fun findByAutoOrderConfigEnabledTrueAndAutoOrderConfigNextScheduledAtBefore(dateTime: LocalDateTime): List<InventoryItem>
}
