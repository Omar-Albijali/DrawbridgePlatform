package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.ScheduleType
import java.time.LocalDateTime

interface InventoryItemRepository : JpaRepository<InventoryItem, String> {
    fun findByRetailer_Id(retailerId: String): List<InventoryItem>
    fun findByProduct_Id(productId: String): List<InventoryItem>
    fun findByRetailer_IdAndProduct_Id(retailerId: String, productId: String): InventoryItem?
    fun findByCurrentQuantityLessThanEqual(quantity: Int): List<InventoryItem>
    
    fun findByAutoOrderConfigEnabledTrue(): List<InventoryItem>
    fun findByAutoOrderConfigScheduleType(scheduleType: ScheduleType): List<InventoryItem>
    fun findByAutoOrderConfigNextScheduledAtBefore(dateTime: LocalDateTime): List<InventoryItem>
    fun findByAutoOrderConfigEnabledTrueAndAutoOrderConfigNextScheduledAtBefore(dateTime: LocalDateTime): List<InventoryItem>
}
