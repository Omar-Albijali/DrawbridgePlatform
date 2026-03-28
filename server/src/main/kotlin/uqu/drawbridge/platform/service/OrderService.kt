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
    private val notificationService: NotificationService
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
        val retailer = userRepository.findById(this.retailerId).orElseThrow { RuntimeException("Retailer not found") }
        val orderGroup = this.orderGroupId?.let { orderGroupRepository.findById(it).orElse(null) }
        
        val itemDTOs = this.orderItems.map { item ->
            val product = productRepository.findById(item.productId).orElse(null)
            val categoryName = product?.let { 
                categoryRepository.findById(it.categoryId).map { c -> c.name }.orElse("Unknown")
            } ?: "Unknown"
            
            val imageUrl = product?.images?.firstOrNull()?.url

            uqu.drawbridge.platform.OrderItemDTO(
                id = (item.id ?: ""),
                productId = item.productId,
                productName = product?.name ?: "Unknown Product",
                productCategory = categoryName,
                productImageUrl = imageUrl,
                quantity = item.quantity,
                unitPrice = item.unitPrice.toDouble()
            )
        }

        return uqu.drawbridge.platform.OrderDTO(
            id = (this.id ?: ""),
            orderGroupId = (this.orderGroupId ?: ""),
            wholesalerId = this.wholesalerId,
            retailerId = this.retailerId,
            retailerName = retailer.businessName ?: retailer.representative?.name ?: "Unknown",
            status = this.status,
            subtotal = this.subtotal.toDouble(),
            autoOrder = this.autoOrder,
            shippingMethod = this.shippingMethod,
            trackingNumber = this.trackingNumber,
            trackingUrl = this.trackingUrl,
            estimatedDelivery = this.estimatedDelivery?.toString(),
            shippedAt = this.shippedAt?.toString(),
            deliveredAt = this.deliveredAt?.toString(),
            placedAt = orderGroup?.createdAt?.toString() ?: "Unknown",
            items = itemDTOs.toTypedArray()
        )
    }

    fun OrderGroup.toDTO(): uqu.drawbridge.platform.OrderGroupDTO {
        // Fetch orders explicitly to ensure we have them, especially if the orderGroup object is fresh and not refreshed
        val orders = if (this.id != null) orderRepository.findByOrderGroupId(this.id!!) else emptyList()
        
        return uqu.drawbridge.platform.OrderGroupDTO(
            id = (this.id ?: ""),
            retailerId = this.retailerId,
            groupTotal = this.groupTotal.toDouble(),
            paymentStatus = this.paymentStatus,
            createdAt = this.createdAt.toString(),
            orders = orders.map { it.toDTO() }.toTypedArray()
        )
    }

    fun mapToDTO(orderGroup: OrderGroup): uqu.drawbridge.platform.OrderGroupDTO {
        return orderGroup.toDTO()
    }

    @Transactional(readOnly = true)
    fun getAllOrdersDTO(): List<uqu.drawbridge.platform.OrderDTO> {
        return getAllOrders().map { it.toDTO() }
    }

    @Transactional(readOnly = true)
    fun getOrderDTOById(id: String): uqu.drawbridge.platform.OrderDTO? {
        val order = getOrderById(id) ?: return null
        return order.toDTO()
    }

    @Transactional(readOnly = true)
    fun getOrdersDTOByWholesaler(wholesalerId: String): List<uqu.drawbridge.platform.OrderDTO> {
        return getOrdersByWholesaler(wholesalerId).map { it.toDTO() }
    }
    
    @Transactional(readOnly = true)
    fun getOrdersDTOByRetailer(retailerId: String): List<uqu.drawbridge.platform.OrderDTO> {
        return getOrdersByRetailer(retailerId).map { it.toDTO() }
    }
    
    @Transactional(readOnly = true)
    fun getOrdersDTOByOrderGroup(orderGroupId: String): List<uqu.drawbridge.platform.OrderDTO> {
        return getOrdersByOrderGroup(orderGroupId).map { it.toDTO() }
    }

    @Transactional(readOnly = true)
    fun getOrderGroupsDTOByRetailer(retailerId: String): List<uqu.drawbridge.platform.OrderGroupDTO> {
        return getOrderGroupsByRetailer(retailerId).map { it.toDTO() }
    }

    @Transactional(readOnly = true)
    fun getOrderGroupDTOById(id: String): uqu.drawbridge.platform.OrderGroupDTO? {
        return getOrderGroupById(id)?.toDTO()
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

    private fun addDeliveredItemsToRetailInventory(order: Order) {
        val orderId = order.id ?: return
        val now = LocalDateTime.now()
        val quantitiesByProductId = orderItemRepository.findByOrderId(orderId)
            .groupingBy { it.productId }
            .fold(0) { total, item -> total + item.quantity }

        quantitiesByProductId.forEach { (productId, quantity) ->
            val inventoryItem = inventoryItemRepository.findByRetailerIdAndProductId(order.retailerId, productId)
            if (inventoryItem != null) {
                inventoryItem.currentQuantity += quantity
                inventoryItem.lastUpdated = now
                inventoryItemRepository.save(inventoryItem)
            } else {
                inventoryItemRepository.save(
                    InventoryItem(
                        retailerId = order.retailerId,
                        productId = productId,
                        currentQuantity = quantity,
                        lastUpdated = now,
                        autoOrderConfig = AutoOrderConfig()
                    )
                )
            }
        }
    }
}
