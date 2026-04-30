package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.ShippingMethod
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.repository.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderGroupRepository: OrderGroupRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val productImageRepository: ProductImageRepository,
    private val notificationService: NotificationService,
    private val inventoryAuditService: InventoryAuditService
) {

    // ==================== ORDER GROUP OPERATIONS ====================

    fun getAllOrderGroups(): List<OrderGroup> {
        return orderGroupRepository.findAll()
    }

    fun getOrderGroupById(id: String): OrderGroup? {
        return orderGroupRepository.findById(id).orElse(null)
    }

    fun getOrderGroupsByRetailer(retailerId: String): List<OrderGroup> {
        return orderGroupRepository.findByRetailerId(retailerId)
    }

    fun getOrderGroupsByPaymentStatus(paymentStatus: PaymentStatus): List<OrderGroup> {
        return orderGroupRepository.findByPaymentStatus(paymentStatus)
    }

    fun getOrderGroupsByRetailerAndPaymentStatus(retailerId: String, paymentStatus: PaymentStatus): List<OrderGroup> {
        return orderGroupRepository.findByRetailerIdAndPaymentStatus(retailerId, paymentStatus)
    }

    @Transactional
    fun createOrderGroup(orderGroup: OrderGroup): OrderGroup {
        return orderGroupRepository.save(orderGroup)
    }

    @Transactional
    fun updateOrderGroupPaymentStatus(id: String, paymentStatus: PaymentStatus): OrderGroup? {
        val orderGroup = orderGroupRepository.findById(id).orElse(null) ?: return null
        orderGroup.paymentStatus = paymentStatus
        return orderGroupRepository.save(orderGroup)
    }

    @Transactional
    fun deleteOrderGroup(id: String): Boolean {
        return if (orderGroupRepository.existsById(id)) {
            orderGroupRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== ORDER OPERATIONS ====================

    fun getAllOrders(): List<Order> {
        return orderRepository.findAll()
    }

    fun getOrderById(id: String): Order? {
        return orderRepository.findById(id).orElse(null)
    }

    fun getOrdersByOrderGroup(orderGroupId: String): List<Order> {
        return orderRepository.findByOrderGroupId(orderGroupId)
    }

    fun getOrdersByRetailer(retailerId: String): List<Order> {
        return orderRepository.findByRetailerId(retailerId)
    }

    fun getOrdersByWholesaler(wholesalerId: String): List<Order> {
        return orderRepository.findByWholesalerId(wholesalerId)
    }

    fun getOrdersByStatus(status: OrderStatus): List<Order> {
        return orderRepository.findByStatus(status)
    }

    fun getOrdersByRetailerAndStatus(retailerId: String, status: OrderStatus): List<Order> {
        return orderRepository.findByRetailerIdAndStatus(retailerId, status)
    }

    fun getOrdersByWholesalerAndStatus(wholesalerId: String, status: OrderStatus): List<Order> {
        return orderRepository.findByWholesalerIdAndStatus(wholesalerId, status)
    }

    fun getAutoOrders(): List<Order> {
        return orderRepository.findByAutoOrderTrue()
    }

    @Transactional
    fun createOrder(order: Order): Order {
        return orderRepository.save(order)
    }

    @Transactional
    fun createOrderWithItems(order: Order, items: List<OrderItem>): Order {
        val savedOrder = orderRepository.save(order)
        savedOrder.orderItems = items.toMutableList()
        val persisted = orderRepository.save(savedOrder)

        notificationService.sendEventNotification(
            recipientId = persisted.retailerId,
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_CREATED,
            entityType = NotificationEntityType.ORDER,
            entityId = persisted.id,
            preferenceKey = NotificationPreferenceKey.ORDER_CONFIRMATION,
            title = "Order confirmed",
            message = "Order ${persisted.id ?: ""} was created successfully.",
            deepLink = "/orders/${persisted.id ?: ""}"
        )

        notificationService.sendEventNotification(
            recipientId = persisted.wholesalerId,
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_CREATED,
            entityType = NotificationEntityType.ORDER,
            entityId = persisted.id,
            preferenceKey = NotificationPreferenceKey.ORDER_CONFIRMATION,
            title = "New order received",
            message = "A new order ${persisted.id ?: ""} was placed in your store.",
            deepLink = "/orders/${persisted.id ?: ""}"
        )

        return persisted
    }

    @Transactional
    fun createAutoRestockOrder(
        retailerId: String,
        wholesalerId: String,
        productId: String,
        quantity: Int,
        unitPrice: BigDecimal
    ): Order? {
        if (quantity <= 0) return null

        val subtotal = unitPrice.multiply(BigDecimal(quantity))
        val order = Order(
            retailerId = retailerId,
            wholesalerId = wholesalerId,
            status = OrderStatus.PENDING,
            subtotal = subtotal,
            autoOrder = true,
            orderItems = mutableListOf(
                OrderItem(
                    productId = productId,
                    quantity = quantity,
                    unitPrice = unitPrice
                )
            )
        )

        val orderGroup = OrderGroup(
            retailerId = retailerId,
            groupTotal = subtotal,
            paymentStatus = PaymentStatus.PENDING,
            orders = mutableListOf(order)
        )

        return orderGroupRepository.save(orderGroup).orders.firstOrNull()
    }

    @Transactional
    fun updateOrderStatus(id: String, status: OrderStatus): Order? {
        val order = orderRepository.findById(id).orElse(null) ?: return null
        val isFirstDelivery = status == OrderStatus.DELIVERED && order.deliveredAt == null
        order.status = status
        
        // Update timestamps based on status
        when (status) {
            OrderStatus.SHIPPED -> order.shippedAt = LocalDateTime.now()
            OrderStatus.DELIVERED -> {
                order.deliveredAt = LocalDateTime.now()
                if (isFirstDelivery) {
                    addDeliveredItemsToRetailInventory(order)
                }
            }
            else -> {}
        }
        
        val savedOrder = orderRepository.save(order)

        notificationService.sendEventNotification(
            recipientId = savedOrder.retailerId,
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_STATUS_UPDATED,
            entityType = NotificationEntityType.ORDER,
            entityId = savedOrder.id,
            preferenceKey = NotificationPreferenceKey.SHIPPING_STATUS,
            title = "Order status updated",
            message = "Order ${savedOrder.id ?: ""} is now ${savedOrder.status.name}.",
            deepLink = "/orders/${savedOrder.id ?: ""}"
        )

        notificationService.sendEventNotification(
            recipientId = savedOrder.wholesalerId,
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_STATUS_UPDATED,
            entityType = NotificationEntityType.ORDER,
            entityId = savedOrder.id,
            preferenceKey = NotificationPreferenceKey.SHIPPING_STATUS,
            title = "Order status updated",
            message = "Order ${savedOrder.id ?: ""} is now ${savedOrder.status.name}.",
            deepLink = "/orders/${savedOrder.id ?: ""}"
        )

        return savedOrder
    }

    @Transactional
    fun updateOrderTracking(
        id: String,
        shippingMethod: ShippingMethod?,
        trackingNumber: String?,
        trackingUrl: String?,
        estimatedDelivery: LocalDateTime?
    ): Order? {
        val order = orderRepository.findById(id).orElse(null) ?: return null
        
        order.shippingMethod = shippingMethod
        order.trackingNumber = trackingNumber
        order.trackingUrl = trackingUrl
        order.estimatedDelivery = estimatedDelivery
        
        return orderRepository.save(order)
    }

    @Transactional
    fun updateOrder(id: String, updatedOrder: Order): Order? {
        val existingOrder = orderRepository.findById(id).orElse(null) ?: return null

        existingOrder.status = updatedOrder.status
        existingOrder.subtotal = updatedOrder.subtotal
        existingOrder.shippingMethod = updatedOrder.shippingMethod
        existingOrder.trackingNumber = updatedOrder.trackingNumber
        existingOrder.trackingUrl = updatedOrder.trackingUrl
        existingOrder.estimatedDelivery = updatedOrder.estimatedDelivery

        return orderRepository.save(existingOrder)
    }

    @Transactional
    fun cancelOrder(id: String): Order? {
        return updateOrderStatus(id, OrderStatus.CANCELLED)
    }

    @Transactional
    fun deleteOrder(id: String): Boolean {
        return if (orderRepository.existsById(id)) {
            orderRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== ORDER ITEM OPERATIONS ====================

    fun getOrderItemsByOrderId(orderId: String): List<OrderItem> {
        return orderItemRepository.findByOrderId(orderId)
    }

    fun getOrderItemsByProductId(productId: String): List<OrderItem> {
        return orderItemRepository.findByProductId(productId)
    }

    @Transactional
    fun addOrderItem(orderId: String, orderItem: OrderItem): OrderItem? {
        val order = orderRepository.findById(orderId).orElse(null) ?: return null
        order.orderItems.add(orderItem)
        val savedOrder = orderRepository.save(order)
        
        // Find the saved item to return it (it should be in the list)
        val savedItem = savedOrder.orderItems.last()
        
        // Update order subtotal
        recalculateOrderSubtotal(savedOrder)
        
        return savedItem
    }

    @Transactional
    fun removeOrderItem(id: String): Boolean {
        val item = orderItemRepository.findById(id).orElse(null)
        return if (item != null) {
            val order = orderRepository.findById(item.orderId!!).orElse(null)
            orderItemRepository.deleteById(id)
            
            // Update order subtotal
            if (order != null) {
                recalculateOrderSubtotal(order)
            }
            
            true
        } else {
            false
        }
    }

    @Transactional
    fun updateOrderItemQuantity(id: String, quantity: Int): OrderItem? {
        val item = orderItemRepository.findById(id).orElse(null) ?: return null
        item.quantity = quantity
        val savedItem = orderItemRepository.save(item)
        
        // Update order subtotal
        val order = orderRepository.findById(item.orderId!!).orElse(null)
        if (order != null) {
            recalculateOrderSubtotal(order)
        }
        
        return savedItem
    }

    // ==================== HELPER METHODS ====================

    // ==================== DTO CONVERSION & ENRICHED GETTERS ====================

    fun Order.toDTO(): uqu.drawbridge.platform.OrderDTO {
        return mapOrdersToDTOs(listOf(this)).first()
    }

    fun OrderGroup.toDTO(): uqu.drawbridge.platform.OrderGroupDTO {
        return mapOrderGroupsToDTOs(listOf(this)).first()
    }

    fun mapToDTO(orderGroup: OrderGroup): uqu.drawbridge.platform.OrderGroupDTO {
        return orderGroup.toDTO()
    }

    @Transactional(readOnly = true)
    fun getAllOrdersDTO(): List<uqu.drawbridge.platform.OrderDTO> {
        return mapOrdersToDTOs(getAllOrders())
    }

    @Transactional(readOnly = true)
    fun getOrderDTOById(id: String): uqu.drawbridge.platform.OrderDTO? {
        val order = getOrderById(id) ?: return null
        return mapOrdersToDTOs(listOf(order)).firstOrNull()
    }

    @Transactional(readOnly = true)
    fun getOrdersDTOByWholesaler(wholesalerId: String): List<uqu.drawbridge.platform.OrderDTO> {
        return mapOrdersToDTOs(getOrdersByWholesaler(wholesalerId))
    }
    
    @Transactional(readOnly = true)
    fun getOrdersDTOByRetailer(retailerId: String): List<uqu.drawbridge.platform.OrderDTO> {
        return mapOrdersToDTOs(getOrdersByRetailer(retailerId))
    }
    
    @Transactional(readOnly = true)
    fun getOrdersDTOByOrderGroup(orderGroupId: String): List<uqu.drawbridge.platform.OrderDTO> {
        return mapOrdersToDTOs(getOrdersByOrderGroup(orderGroupId))
    }

    @Transactional(readOnly = true)
    fun getOrderGroupsDTOByRetailer(retailerId: String): List<uqu.drawbridge.platform.OrderGroupDTO> {
        return mapOrderGroupsToDTOs(getOrderGroupsByRetailer(retailerId))
    }

    @Transactional(readOnly = true)
    fun getOrderGroupDTOById(id: String): uqu.drawbridge.platform.OrderGroupDTO? {
        val group = getOrderGroupById(id) ?: return null
        return mapOrderGroupsToDTOs(listOf(group)).firstOrNull()
    }

    @Transactional
    fun updateOrderStatusDTO(id: String, status: OrderStatus): uqu.drawbridge.platform.OrderDTO? {
        return updateOrderStatus(id, status)?.toDTO()
    }

    @Transactional
    fun updateOrderTrackingDTO(
        id: String,
        shippingMethod: ShippingMethod?,
        trackingNumber: String?,
        trackingUrl: String?,
        estimatedDelivery: LocalDateTime?
    ): uqu.drawbridge.platform.OrderDTO? {
        return updateOrderTracking(id, shippingMethod, trackingNumber, trackingUrl, estimatedDelivery)?.toDTO()
    }

    @Transactional
    fun cancelOrderDTO(id: String): uqu.drawbridge.platform.OrderDTO? {
        return cancelOrder(id)?.toDTO()
    }

    // Reuse existing helper methods
    private fun recalculateOrderSubtotal(order: Order) {
        val items = orderItemRepository.findByOrderId(order.id!!)
        val subtotal = items.fold(BigDecimal.ZERO) { acc, item ->
            acc + (item.unitPrice * BigDecimal(item.quantity))
        }
        order.subtotal = subtotal
        orderRepository.save(order)
        
        // Also update the order group total
        val orderGroup = orderGroupRepository.findById(order.orderGroupId!!).orElse(null)
        if (orderGroup != null) {
            recalculateOrderGroupTotal(orderGroup)
        }
    }

    private fun recalculateOrderGroupTotal(orderGroup: OrderGroup) {
        val orders = orderRepository.findByOrderGroupId(orderGroup.id!!)
        val total = orders.fold(BigDecimal.ZERO) { acc, order ->
            acc + order.subtotal
        }
        orderGroup.groupTotal = total
        orderGroupRepository.save(orderGroup)
    }

    private fun mapOrderGroupsToDTOs(orderGroups: List<OrderGroup>): List<uqu.drawbridge.platform.OrderGroupDTO> {
        if (orderGroups.isEmpty()) {
            return emptyList()
        }

        val groupIds = orderGroups.mapNotNull { it.id }.distinct()
        val ordersByGroupId = if (groupIds.isEmpty()) {
            emptyMap()
        } else {
            orderRepository.findByOrderGroupIdIn(groupIds).groupBy { it.orderGroupId.orEmpty() }
        }
        val allOrders = ordersByGroupId.values.flatten()
        val orderDTOsById = mapOrdersToDTOs(allOrders).associateBy { it.id }

        return orderGroups.map { group ->
            val orders = group.id?.let { groupId ->
                ordersByGroupId[groupId].orEmpty().mapNotNull { order -> order.id?.let(orderDTOsById::get) }
            }.orEmpty()

            uqu.drawbridge.platform.OrderGroupDTO(
                id = group.id.orEmpty(),
                retailerId = group.retailerId,
                groupTotal = group.groupTotal.toDouble(),
                paymentStatus = group.paymentStatus,
                createdAt = group.createdAt.toString(),
                orders = orders.toTypedArray()
            )
        }
    }

    private fun mapOrdersToDTOs(orders: List<Order>): List<uqu.drawbridge.platform.OrderDTO> {
        if (orders.isEmpty()) {
            return emptyList()
        }

        val retailersById = userRepository.findAllById(orders.map { it.retailerId }.distinct())
            .associateBy { it.id.orEmpty() }
        val orderGroupsById = orderGroupRepository.findAllById(orders.mapNotNull { it.orderGroupId }.distinct())
            .associateBy { it.id.orEmpty() }
        val productIds = orders.flatMap { order -> order.orderItems.map { item -> item.productId } }.distinct()
        val productsById = productRepository.findAllById(productIds).associateBy { it.id.orEmpty() }
        val categoriesById = categoryRepository.findAllById(productsById.values.map { it.categoryId }.distinct())
            .associateBy { it.id.orEmpty() }

        return orders.map { order ->
            val retailer = retailersById[order.retailerId]
            val orderGroup = order.orderGroupId?.let(orderGroupsById::get)
            val itemDTOs = order.orderItems.map { item ->
                val product = productsById[item.productId]
                val categoryName = product?.categoryId?.let { categoriesById[it]?.name } ?: "Unknown"
                val imageUrl = product?.images?.firstOrNull()?.url

                uqu.drawbridge.platform.OrderItemDTO(
                    id = item.id.orEmpty(),
                    productId = item.productId,
                    productName = product?.name ?: "Unknown Product",
                    productCategory = categoryName,
                    productImageUrl = imageUrl,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice.toDouble()
                )
            }

            uqu.drawbridge.platform.OrderDTO(
                id = order.id.orEmpty(),
                orderGroupId = order.orderGroupId.orEmpty(),
                wholesalerId = order.wholesalerId,
                retailerId = order.retailerId,
                retailerName = retailer?.businessName ?: retailer?.representative?.name ?: "Unknown",
                status = order.status,
                subtotal = order.subtotal.toDouble(),
                autoOrder = order.autoOrder,
                shippingMethod = order.shippingMethod,
                trackingNumber = order.trackingNumber,
                trackingUrl = order.trackingUrl,
                estimatedDelivery = order.estimatedDelivery?.toString(),
                shippedAt = order.shippedAt?.toString(),
                deliveredAt = order.deliveredAt?.toString(),
                placedAt = orderGroup?.createdAt?.toString() ?: "Unknown",
                items = itemDTOs.toTypedArray()
            )
        }
    }

    private fun addDeliveredItemsToRetailInventory(order: Order) {
        val orderId = order.id ?: return
        val now = LocalDateTime.now()
        val sourceType = if (order.autoOrder) InventoryAuditSourceType.RESTOCK else InventoryAuditSourceType.ORDER
        val reason = if (order.autoOrder) "Auto-restock order delivered" else "Order delivered"
        val quantitiesByProductId = orderItemRepository.findByOrderId(orderId)
            .groupingBy { it.productId }
            .fold(0) { total, item -> total + item.quantity }

        quantitiesByProductId.forEach { (productId, quantity) ->
            val inventoryItem = inventoryItemRepository.findByRetailerIdAndProductId(order.retailerId, productId)
            if (inventoryItem != null) {
                val previousQuantity = inventoryItem.currentQuantity
                inventoryItem.currentQuantity = previousQuantity + quantity
                inventoryItem.lastUpdated = now
                val savedItem = inventoryItemRepository.save(inventoryItem)
                inventoryAuditService.logStockChange(
                    productId = productId,
                    inventoryItemId = savedItem.id,
                    stockTargetType = InventoryStockTargetType.RETAILER_INVENTORY,
                    sourceType = sourceType,
                    sourceId = orderId,
                    quantityBefore = previousQuantity,
                    quantityAfter = savedItem.currentQuantity,
                    reason = reason
                )
            } else {
                val savedItem = inventoryItemRepository.save(
                    InventoryItem(
                        retailerId = order.retailerId,
                        productId = productId,
                        currentQuantity = quantity,
                        lastUpdated = now,
                        autoOrderConfig = AutoOrderConfig()
                    )
                )
                inventoryAuditService.logStockChange(
                    productId = productId,
                    inventoryItemId = savedItem.id,
                    stockTargetType = InventoryStockTargetType.RETAILER_INVENTORY,
                    sourceType = sourceType,
                    sourceId = orderId,
                    quantityBefore = 0,
                    quantityAfter = savedItem.currentQuantity,
                    reason = reason
                )
            }
        }
    }
}
