package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.InventoryItemDTO
import uqu.drawbridge.platform.InventoryStatus
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.CreateInventoryItemRequest
import uqu.drawbridge.platform.UpdateAutoOrderConfigRequest
import uqu.drawbridge.platform.AutoOrderConfigDTO
import uqu.drawbridge.platform.ScheduleType
import uqu.drawbridge.platform.model.AutoOrderConfig
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.model.InventoryStockTargetType
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.validation.RequestValidation
import java.time.DayOfWeek
import java.time.LocalDateTime

@Service
class InventoryService(
    private val inventoryItemRepository: InventoryItemRepository,
    private val productRepository: ProductRepository,
    private val orderService: OrderService,
    private val notificationService: NotificationService,
    private val inventoryAuditService: InventoryAuditService
) {

    // ==================== INVENTORY ITEM OPERATIONS ====================

    fun getAllInventoryItems(): List<InventoryItem> {
        return inventoryItemRepository.findAll()
    }

    fun getInventoryItemById(id: String): InventoryItem? {
        return inventoryItemRepository.findById(id).orElse(null)
    }

    fun getInventoryItemsByRetailer(retailerId: String): List<InventoryItem> {
        return inventoryItemRepository.findByRetailerId(retailerId)
    }

    fun getInventoryItemsByProduct(productId: String): List<InventoryItem> {
        return inventoryItemRepository.findByProductId(productId)
    }

    fun getInventoryItemByRetailerAndProduct(retailerId: String, productId: String): InventoryItem? {
        return inventoryItemRepository.findByRetailerIdAndProductId(retailerId, productId)
    }

    fun getLowStockItems(threshold: Int): List<InventoryItem> {
        return inventoryItemRepository.findByCurrentQuantityLessThanEqual(threshold)
    }

    @Transactional
    fun createInventoryItem(inventoryItem: InventoryItem): InventoryItem {
        inventoryItem.lastUpdated = LocalDateTime.now()
        calculateNextScheduledAt(inventoryItem.autoOrderConfig)
        val savedItem = inventoryItemRepository.save(inventoryItem)
        logInventoryStockChange(savedItem, 0, savedItem.currentQuantity, InventoryAuditSourceType.MANUAL, reason = "Inventory item created")
        return savedItem
    }

    @Transactional
    fun updateInventoryItem(id: String, updatedItem: InventoryItem): InventoryItem? {
        val existingItem = inventoryItemRepository.findById(id).orElse(null) ?: return null
        val previousQuantity = existingItem.currentQuantity

        existingItem.currentQuantity = updatedItem.currentQuantity
        existingItem.lastUpdated = LocalDateTime.now()
        
        // If there's an updated config, we might want to update it too or handle it separately
        val config = existingItem.autoOrderConfig
        updateConfigFields(config, updatedItem.autoOrderConfig)
        calculateNextScheduledAt(config)
        existingItem.autoOrderConfig = config

        val savedItem = inventoryItemRepository.save(existingItem)
        logInventoryStockChange(savedItem, previousQuantity, savedItem.currentQuantity, InventoryAuditSourceType.MANUAL, reason = "Inventory item updated")
        return savedItem
    }

    @Transactional
    fun updateQuantity(
        id: String,
        newQuantity: Int,
        sourceType: InventoryAuditSourceType = InventoryAuditSourceType.MANUAL,
        sourceId: String? = null,
        reason: String? = "Inventory quantity updated"
    ): InventoryItem? {
        val item = inventoryItemRepository.findById(id).orElse(null) ?: return null
        val previousQuantity = item.currentQuantity
        item.currentQuantity = newQuantity
        item.lastUpdated = LocalDateTime.now()
        val savedItem = inventoryItemRepository.save(item)
        logInventoryStockChange(savedItem, previousQuantity, savedItem.currentQuantity, sourceType, sourceId, reason)
        maybeNotifyLowStock(savedItem)
        maybeTriggerThresholdAutoRestock(savedItem, previousQuantity)
        return savedItem
    }

    @Transactional
    fun adjustQuantity(
        id: String,
        adjustment: Int,
        sourceType: InventoryAuditSourceType = InventoryAuditSourceType.MANUAL,
        sourceId: String? = null,
        reason: String? = "Inventory quantity adjusted"
    ): InventoryItem? {
        val item = inventoryItemRepository.findById(id).orElse(null) ?: return null
        val previousQuantity = item.currentQuantity
        item.currentQuantity += adjustment
        item.lastUpdated = LocalDateTime.now()
        val savedItem = inventoryItemRepository.save(item)
        logInventoryStockChange(savedItem, previousQuantity, savedItem.currentQuantity, sourceType, sourceId, reason)
        maybeNotifyLowStock(savedItem)
        maybeTriggerThresholdAutoRestock(savedItem, previousQuantity)
        return savedItem
    }

    @Transactional
    fun deleteInventoryItem(id: String): Boolean {
        return if (inventoryItemRepository.existsById(id)) {
            inventoryItemRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== AUTO ORDER CONFIG OPERATIONS ====================

    fun getAutoOrderConfigByInventoryId(inventoryItemId: String): AutoOrderConfig? {
        return inventoryItemRepository.findById(inventoryItemId).orElse(null)?.autoOrderConfig
    }

    fun getEnabledAutoOrderConfigs(): List<InventoryItem> {
        return inventoryItemRepository.findByAutoOrderConfigEnabledTrue()
    }

    fun getAutoOrderConfigsByScheduleType(scheduleType: ScheduleType): List<InventoryItem> {
        return inventoryItemRepository.findByAutoOrderConfigScheduleType(scheduleType)
    }

    fun getAutoOrderConfigsDueForProcessing(): List<InventoryItem> {
        return inventoryItemRepository.findByAutoOrderConfigEnabledTrueAndAutoOrderConfigNextScheduledAtBefore(LocalDateTime.now())
    }

    @Transactional
    fun setAutoOrderConfig(inventoryItemId: String, config: AutoOrderConfig): InventoryItem? {
        val item = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return null
        calculateNextScheduledAt(config)
        item.autoOrderConfig = config
        return inventoryItemRepository.save(item)
    }

    @Transactional
    fun updateAutoOrderConfig(inventoryItemId: String, updatedConfig: AutoOrderConfig): InventoryItem? {
        val item = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return null
        val config = item.autoOrderConfig

        updateConfigFields(config, updatedConfig)
        calculateNextScheduledAt(config)
        item.autoOrderConfig = config

        return inventoryItemRepository.save(item)
    }

    @Transactional
    fun toggleAutoOrderConfig(inventoryItemId: String, enabled: Boolean): InventoryItem? {
        val item = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return null
        val config = item.autoOrderConfig
        config.enabled = enabled
        if (enabled) {
            calculateNextScheduledAt(config)
        }
        return inventoryItemRepository.save(item)
    }

    @Transactional
    fun markAutoOrderTriggered(inventoryItemId: String): InventoryItem? {
        val item = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return null
        val config = item.autoOrderConfig
        config.lastTriggeredAt = LocalDateTime.now()
        calculateNextScheduledAt(config)
        val savedItem = inventoryItemRepository.save(item)
        notificationService.sendEventNotification(
            recipientId = savedItem.retailerId,
            type = NotificationType.STOCK,
            eventKey = NotificationEventKey.AUTO_RESTOCK_TRIGGERED,
            entityType = NotificationEntityType.INVENTORY_ITEM,
            entityId = savedItem.id,
            preferenceKey = NotificationPreferenceKey.AUTO_RESTOCK_CONFIRMATION,
            title = "Auto-restock triggered",
            message = "Auto-restock was triggered for product ${savedItem.productId}.",
            deepLink = "/inventory"
        )
        return savedItem
    }

    @Transactional
    fun processDueAutoOrders(): Int {
        val dueItems = getAutoOrderConfigsDueForProcessing()
            .filter { it.autoOrderConfig.scheduleType != ScheduleType.THRESHOLD_BASED }

        var processed = 0
        dueItems.forEach { item ->
            if (triggerAutoRestock(item)) {
                processed += 1
            }
        }
        return processed
    }


    // ==================== DTO CONVERSION ====================

    private fun InventoryItem.toDTO(): InventoryItemDTO {
        val product = productRepository.findById(this.productId).orElse(null)
        val supplierName = product?.wholesaler?.businessName ?: "Unknown Supplier"
        
        val status = when {
            this.currentQuantity == 0 -> InventoryStatus.OUT_OF_STOCK
            this.currentQuantity <= this.autoOrderConfig.minThreshold -> InventoryStatus.LOW_STOCK
            else -> InventoryStatus.OPTIMAL
        }

        return InventoryItemDTO(
            id = (this.id ?: ""),
            name = product?.name ?: "Unknown Product",
            currentStock = this.currentQuantity,
            autoRestock = this.autoOrderConfig.enabled,
            autoOrderConfig = this.autoOrderConfig.toDTO(),
            status = status,
            supplier = supplierName,
            lastRestocked = this.lastUpdated.toString(),
            reorderQuantity = this.autoOrderConfig.reorderQuantity,
            minimumOrderQuantity = product?.minimumOrderQuantity ?: 1,
            imageUrl = product?.images?.sortedBy { it.sortIndex }?.firstOrNull()?.url
        )
    }

    private fun AutoOrderConfig.toDTO(): uqu.drawbridge.platform.AutoOrderConfigDTO {
        return uqu.drawbridge.platform.AutoOrderConfigDTO(
            enabled = this.enabled,
            minThreshold = this.minThreshold,
            reorderQuantity = this.reorderQuantity,
            scheduleType = this.scheduleType,
            intervalDays = this.intervalDays,
            dayOfWeek = this.dayOfWeek,
            dayOfMonth = this.dayOfMonth,
            lastTriggeredAt = this.lastTriggeredAt?.toString(),
            nextScheduledAt = this.nextScheduledAt?.toString()
        )
    }

    // ==================== PUBLIC DTO METHODS ====================

    @Transactional(readOnly = true)
    fun getAllInventoryItemsDTO(): List<uqu.drawbridge.platform.InventoryItemDTO> {
        return itemsToDTOs(inventoryItemRepository.findAll())
    }

    @Transactional(readOnly = true)
    fun getInventoryItemDTOById(id: String): uqu.drawbridge.platform.InventoryItemDTO? {
        val item = inventoryItemRepository.findById(id).orElse(null) ?: return null
        return itemsToDTOs(listOf(item)).firstOrNull()
    }

    @Transactional(readOnly = true)
    fun getInventoryItemsDTOByRetailer(retailerId: String): List<uqu.drawbridge.platform.InventoryItemDTO> {
        return itemsToDTOs(inventoryItemRepository.findByRetailerId(retailerId))
    }

    @Transactional(readOnly = true)
    fun getInventoryItemsDTOByProduct(productId: String): List<uqu.drawbridge.platform.InventoryItemDTO> {
        return itemsToDTOs(inventoryItemRepository.findByProductId(productId))
    }

    @Transactional(readOnly = true)
    fun getLowStockItemsDTO(threshold: Int): List<uqu.drawbridge.platform.InventoryItemDTO> {
        return itemsToDTOs(inventoryItemRepository.findByCurrentQuantityLessThanEqual(threshold))
    }

    @Transactional
    fun createInventoryItemDTO(inventoryItem: InventoryItem): uqu.drawbridge.platform.InventoryItemDTO {
        return createInventoryItem(inventoryItem).toDTO()
    }

    @Transactional
    fun updateInventoryItemDTO(id: String, updatedItem: InventoryItem): uqu.drawbridge.platform.InventoryItemDTO? {
        return updateInventoryItem(id, updatedItem)?.toDTO()
    }

    @Transactional
    fun updateQuantityDTO(id: String, newQuantity: Int): uqu.drawbridge.platform.InventoryItemDTO? {
        return updateQuantity(id, newQuantity)?.toDTO()
    }

    // ==================== AUTO ORDER CONFIG DTO METHODS ====================

    @Transactional(readOnly = true)
    fun getAutoOrderConfigDTOByInventoryId(inventoryItemId: String): uqu.drawbridge.platform.AutoOrderConfigDTO? {
        return getAutoOrderConfigByInventoryId(inventoryItemId)?.toDTO()
    }

    @Transactional
    fun setAutoOrderConfigDTO(inventoryItemId: String, config: AutoOrderConfig): uqu.drawbridge.platform.InventoryItemDTO? {
        return setAutoOrderConfig(inventoryItemId, config)?.toDTO()
    }

    @Transactional
    fun updateAutoOrderConfigDTO(inventoryItemId: String, updatedConfig: AutoOrderConfig): uqu.drawbridge.platform.AutoOrderConfigDTO? {
        // Need to return updated Config DTO, but updateAutoOrderConfig returns InventoryItem
        val item = updateAutoOrderConfig(inventoryItemId, updatedConfig)
        return item?.autoOrderConfig?.toDTO()
    }

    @Transactional
    fun toggleAutoOrderConfigDTO(inventoryItemId: String, enabled: Boolean): uqu.drawbridge.platform.AutoOrderConfigDTO? {
         val item = toggleAutoOrderConfig(inventoryItemId, enabled)
         return item?.autoOrderConfig?.toDTO()
    }

    // ==================== DTO REQUEST METHODS ====================

    @Transactional
    fun createInventoryItemFromRequest(request: CreateInventoryItemRequest): InventoryItemDTO {
        RequestValidation.requireNotBlank(request.productId, "productId")
        RequestValidation.requireNotBlank(request.retailerId, "retailerId")
        RequestValidation.requireNonNegative(request.currentStock, "currentStock")
        RequestValidation.requireNonNegative(request.minThreshold, "minThreshold")
        val autoOrderConfig = AutoOrderConfig(
            enabled = request.autoRestock,
            minThreshold = request.minThreshold
        )
        val inventoryItem = InventoryItem(
            productId = request.productId,
            retailerId = request.retailerId,
            currentQuantity = request.currentStock,
            lastUpdated = LocalDateTime.now(),
            autoOrderConfig = autoOrderConfig
        )
        return createInventoryItem(inventoryItem).toDTO()
    }

    @Transactional
    fun updateInventoryItemFromRequest(id: String, request: CreateInventoryItemRequest): InventoryItemDTO? {
        RequestValidation.requireNotBlank(id, "id")
        RequestValidation.requireNonNegative(request.currentStock, "currentStock")
        RequestValidation.requireNonNegative(request.minThreshold, "minThreshold")
        val existingItem = inventoryItemRepository.findById(id).orElse(null) ?: return null
        val previousQuantity = existingItem.currentQuantity
        existingItem.currentQuantity = request.currentStock
        existingItem.lastUpdated = LocalDateTime.now()
        existingItem.autoOrderConfig.enabled = request.autoRestock
        existingItem.autoOrderConfig.minThreshold = request.minThreshold
        val savedItem = inventoryItemRepository.save(existingItem)
        logInventoryStockChange(savedItem, previousQuantity, savedItem.currentQuantity, InventoryAuditSourceType.MANUAL, reason = "Inventory item updated")
        return savedItem.toDTO()
    }

    @Transactional
    fun setAutoOrderConfigFromRequest(inventoryItemId: String, request: UpdateAutoOrderConfigRequest): InventoryItemDTO? {
        RequestValidation.requireNotBlank(inventoryItemId, "inventoryItemId")
        RequestValidation.requireNonNegative(request.minThreshold, "minThreshold")
        RequestValidation.requirePositive(request.reorderQuantity, "reorderQuantity")
        val item = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return null
        validateAutoOrderReorderQuantity(item, request.reorderQuantity)
        val config = item.autoOrderConfig
        
        updateConfigFromRequest(config, request)
        
        item.autoOrderConfig = config
        return inventoryItemRepository.save(item).toDTO()
    }

    @Transactional
    fun updateAutoOrderConfigFromRequest(inventoryItemId: String, request: UpdateAutoOrderConfigRequest): AutoOrderConfigDTO? {
        RequestValidation.requireNotBlank(inventoryItemId, "inventoryItemId")
        RequestValidation.requireNonNegative(request.minThreshold, "minThreshold")
        RequestValidation.requirePositive(request.reorderQuantity, "reorderQuantity")
        val item = inventoryItemRepository.findById(inventoryItemId).orElse(null) ?: return null
        validateAutoOrderReorderQuantity(item, request.reorderQuantity)
        val config = item.autoOrderConfig
        
        updateConfigFromRequest(config, request)
        
        item.autoOrderConfig = config
        inventoryItemRepository.save(item)
        return config.toDTO()
    }

    // ==================== HELPER METHODS ====================

    private fun updateConfigFields(existing: AutoOrderConfig, updated: AutoOrderConfig) {
        existing.enabled = updated.enabled
        existing.minThreshold = updated.minThreshold
        existing.reorderQuantity = updated.reorderQuantity
        existing.scheduleType = updated.scheduleType
        existing.intervalDays = updated.intervalDays
        existing.dayOfWeek = updated.dayOfWeek
        existing.dayOfMonth = updated.dayOfMonth
    }

    private fun logInventoryStockChange(
        item: InventoryItem,
        quantityBefore: Int,
        quantityAfter: Int,
        sourceType: InventoryAuditSourceType,
        sourceId: String? = null,
        reason: String? = null
    ) {
        inventoryAuditService.logStockChange(
            productId = item.productId,
            inventoryItemId = item.id,
            stockTargetType = InventoryStockTargetType.RETAILER_INVENTORY,
            sourceType = sourceType,
            sourceId = sourceId,
            quantityBefore = quantityBefore,
            quantityAfter = quantityAfter,
            reason = reason
        )
    }

    private fun updateConfigFromRequest(config: AutoOrderConfig, request: UpdateAutoOrderConfigRequest) {
        config.enabled = request.enabled
        config.minThreshold = request.minThreshold
        config.reorderQuantity = request.reorderQuantity
        config.scheduleType = request.scheduleType
        config.intervalDays = request.intervalDays
        config.dayOfWeek = request.dayOfWeek
        config.dayOfMonth = request.dayOfMonth
        calculateNextScheduledAt(config)
    }

    private fun validateAutoOrderReorderQuantity(item: InventoryItem, reorderQuantity: Int) {
        val product = productRepository.findById(item.productId).orElse(null)
            ?: throw IllegalArgumentException("Product not found for inventory item ${item.id ?: item.productId}.")

        if (reorderQuantity < product.minimumOrderQuantity) {
            throw IllegalArgumentException("Minimum order quantity for ${product.name} is ${product.minimumOrderQuantity} units.")
        }

        if (reorderQuantity > product.stockQuantity) {
            throw IllegalArgumentException("Only ${product.stockQuantity} units available for ${product.name}.")
        }
    }

    private fun calculateNextScheduledAt(config: AutoOrderConfig) {
        val now = LocalDateTime.now()
        config.nextScheduledAt = when (config.scheduleType) {
            ScheduleType.THRESHOLD_BASED -> null // No scheduled time, triggers on threshold
            ScheduleType.DAILY -> now.plusDays(1).withHour(9).withMinute(0).withSecond(0)
            ScheduleType.WEEKLY -> {
                val targetDay = parseDayOfWeek(config.dayOfWeek) ?: DayOfWeek.MONDAY.value
                var next = now.plusDays(1)
                while (next.dayOfWeek.value != targetDay) {
                    next = next.plusDays(1)
                }
                next.withHour(9).withMinute(0).withSecond(0)
            }
            ScheduleType.MONTHLY -> {
                val targetDay = config.dayOfMonth?.split(",")?.firstOrNull()?.trim()?.toIntOrNull() ?: 1
                var next = now.plusMonths(1).withDayOfMonth(1)
                next = next.withDayOfMonth(minOf(targetDay, next.month.length(next.toLocalDate().isLeapYear)))
                next.withHour(9).withMinute(0).withSecond(0)
            }
            ScheduleType.INTERVAL_DAYS -> {
                val interval = config.intervalDays ?: 7
                now.plusDays(interval.toLong()).withHour(9).withMinute(0).withSecond(0)
            }
        }
    }

    private fun parseDayOfWeek(dayOfWeek: String?): Int? {
        val token = dayOfWeek
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.uppercase()
            ?: return null

        return token.toIntOrNull()
            ?.takeIf { it in 1..7 }
            ?: when (token) {
                "MONDAY" -> DayOfWeek.MONDAY.value
                "TUESDAY" -> DayOfWeek.TUESDAY.value
                "WEDNESDAY" -> DayOfWeek.WEDNESDAY.value
                "THURSDAY" -> DayOfWeek.THURSDAY.value
                "FRIDAY" -> DayOfWeek.FRIDAY.value
                "SATURDAY" -> DayOfWeek.SATURDAY.value
                "SUNDAY" -> DayOfWeek.SUNDAY.value
                else -> null
            }
    }


    private fun itemsToDTOs(items: List<InventoryItem>): List<uqu.drawbridge.platform.InventoryItemDTO> {
        if (items.isEmpty()) return emptyList()

        val productIds = items.map { it.productId }.distinct()
        val products = productRepository.findAllById(productIds).associateBy { it.id }

        return items.map { item ->
            val product = products[item.productId]
            val supplierName = product?.wholesaler?.businessName ?: "Unknown Supplier"
            val productName = product?.name ?: "Unknown Product"

            val status = when {
                item.currentQuantity == 0 -> InventoryStatus.OUT_OF_STOCK
                item.currentQuantity <= item.autoOrderConfig.minThreshold -> InventoryStatus.LOW_STOCK
                else -> InventoryStatus.OPTIMAL
            }

            uqu.drawbridge.platform.InventoryItemDTO(
                id = (item.id ?: ""),
                name = productName,
                currentStock = item.currentQuantity,
                autoRestock = item.autoOrderConfig.enabled,
                autoOrderConfig = item.autoOrderConfig.toDTO(),
                status = status,
                supplier = supplierName,
                lastRestocked = item.lastUpdated.toString(),
                reorderQuantity = item.autoOrderConfig.reorderQuantity,
                minimumOrderQuantity = product?.minimumOrderQuantity ?: 1,
                imageUrl = product?.images?.sortedBy { it.sortIndex }?.firstOrNull()?.url
            )
        }
    }

    private fun maybeNotifyLowStock(item: InventoryItem) {
        val minThreshold = item.autoOrderConfig.minThreshold
        if (item.currentQuantity <= minThreshold) {
            notificationService.sendEventNotification(
                recipientId = item.retailerId,
                type = NotificationType.STOCK,
                eventKey = NotificationEventKey.LOW_STOCK_ALERT,
                entityType = NotificationEntityType.INVENTORY_ITEM,
                entityId = item.id,
                preferenceKey = NotificationPreferenceKey.LOW_STOCK_WARNING,
                title = "Low stock alert",
                message = "Product ${item.productId} dropped to ${item.currentQuantity}, below threshold $minThreshold.",
                deepLink = "/inventory"
            )
        }
    }

    private fun maybeTriggerThresholdAutoRestock(item: InventoryItem, previousQuantity: Int) {
        val config = item.autoOrderConfig
        if (!config.enabled || config.scheduleType != ScheduleType.THRESHOLD_BASED) return

        val threshold = config.minThreshold
        val crossedThreshold = previousQuantity > threshold && item.currentQuantity <= threshold
        if (crossedThreshold) {
            triggerAutoRestock(item)
        }
    }

    private fun triggerAutoRestock(item: InventoryItem): Boolean {
        val itemId = item.id ?: return false
        val config = item.autoOrderConfig
        val reorderQuantity = config.reorderQuantity
        if (!config.enabled || reorderQuantity <= 0) return false

        val product = productRepository.findById(item.productId).orElse(null) ?: return false
        if (reorderQuantity < product.minimumOrderQuantity || reorderQuantity > product.stockQuantity) return false
        val wholesalerId = product.wholesaler.id ?: return false
        val createdOrder = orderService.createAutoRestockOrder(
            retailerId = item.retailerId,
            wholesalerId = wholesalerId,
            productId = item.productId,
            quantity = reorderQuantity,
            unitPrice = product.price
        ) ?: return false

        if (createdOrder.id == null && createdOrder.orderGroupId == null) {
            // Guard for unexpected persistence behavior; avoid marking as triggered if order wasn't persisted.
            return false
        }

        markAutoOrderTriggered(itemId)
        return true
    }
}
