package uqu.drawbridge.platform.service

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.security.access.AccessDeniedException
import uqu.drawbridge.platform.NotificationEntityType
import uqu.drawbridge.platform.NotificationEventKey
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.model.AutoOrderConfig
import uqu.drawbridge.platform.model.Category
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.model.Order
import uqu.drawbridge.platform.model.OrderGroup
import uqu.drawbridge.platform.model.OrderItem
import uqu.drawbridge.platform.model.Product
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.CategoryRepository
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.OrderGroupRepository
import uqu.drawbridge.platform.repository.OrderItemRepository
import uqu.drawbridge.platform.repository.OrderRepository
import uqu.drawbridge.platform.repository.ProductImageRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository

class OrderServiceTest {
    private val orderRepository = mock(OrderRepository::class.java)
    private val orderItemRepository = mock(OrderItemRepository::class.java)
    private val orderGroupRepository = mock(OrderGroupRepository::class.java)
    private val inventoryItemRepository = mock(InventoryItemRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val productRepository = mock(ProductRepository::class.java)
    private val categoryRepository = mock(CategoryRepository::class.java)
    private val productImageRepository = mock(ProductImageRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val inventoryAuditService = mock(InventoryAuditService::class.java)

    private val service = OrderService(
        orderRepository = orderRepository,
        orderItemRepository = orderItemRepository,
        orderGroupRepository = orderGroupRepository,
        inventoryItemRepository = inventoryItemRepository,
        userRepository = userRepository,
        productRepository = productRepository,
        categoryRepository = categoryRepository,
        productImageRepository = productImageRepository,
        notificationService = notificationService,
        inventoryAuditService = inventoryAuditService
    )

    @Test
    fun `created order notifications are recipient aware`() {
        val orderId = "placed01-0000-0000-0000-000000000000"
        val order = order(id = orderId, status = OrderStatus.PENDING, retailerId = "retailer-1")
        val item = orderItem(quantity = 2)
        stubOrderSave()

        val result = service.createOrderWithItems(order, listOf(item))

        assertEquals(OrderStatus.PENDING, result.status)
        verify(notificationService).sendEventNotification(
            recipientId = "retailer-1",
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_CREATED,
            entityType = NotificationEntityType.ORDER,
            entityId = orderId,
            preferenceKey = NotificationPreferenceKey.ORDER_CONFIRMATION,
            title = "Order placed",
            message = "Your order placed01 was placed and is waiting for approval.",
            deepLink = "/orders/$orderId"
        )
        verify(notificationService).sendEventNotification(
            recipientId = "wholesaler-1",
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_CREATED,
            entityType = NotificationEntityType.ORDER,
            entityId = orderId,
            preferenceKey = NotificationPreferenceKey.ORDER_CONFIRMATION,
            title = "New order received",
            message = "A new order placed01 is waiting for your approval.",
            deepLink = "/orders/$orderId"
        )
        verify(notificationService, never()).sendEventNotification(
            recipientId = "retailer-1",
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_CREATED,
            entityType = NotificationEntityType.ORDER,
            entityId = orderId,
            preferenceKey = NotificationPreferenceKey.ORDER_CONFIRMATION,
            title = "Order confirmed",
            message = "Order $orderId was created successfully.",
            deepLink = "/orders/$orderId"
        )
    }

    @Test
    fun `status update notifications are recipient aware`() {
        listOf(
            StatusNotificationCase(
                status = OrderStatus.CONFIRMED,
                orderId = "confirm1-0000-0000-0000-000000000000",
                title = "Order confirmed",
                retailerMessage = "The wholesaler confirmed your order confirm1.",
                wholesalerMessage = "You confirmed order confirm1."
            ),
            StatusNotificationCase(
                status = OrderStatus.SHIPPED,
                orderId = "shipped1-0000-0000-0000-000000000000",
                title = "Order shipped",
                retailerMessage = "Your order shipped1 has been shipped. Confirm receipt when it arrives.",
                wholesalerMessage = "You marked order shipped1 as shipped."
            ),
            StatusNotificationCase(
                status = OrderStatus.CANCELLED,
                orderId = "cancel01-0000-0000-0000-000000000000",
                title = "Order cancelled",
                retailerMessage = "Your order cancel01 was cancelled.",
                wholesalerMessage = "Order cancel01 was cancelled."
            )
        ).forEach { case ->
            val order = order(id = case.orderId, status = OrderStatus.PENDING, retailerId = "retailer-1")
            `when`(orderRepository.findById(case.orderId)).thenReturn(Optional.of(order))
            stubOrderSave()

            service.updateOrderStatus(case.orderId, case.status)

            verify(notificationService).sendEventNotification(
                recipientId = "retailer-1",
                type = NotificationType.ORDER,
                eventKey = NotificationEventKey.ORDER_STATUS_UPDATED,
                entityType = NotificationEntityType.ORDER,
                entityId = case.orderId,
                preferenceKey = NotificationPreferenceKey.SHIPPING_STATUS,
                title = case.title,
                message = case.retailerMessage,
                deepLink = "/orders/${case.orderId}"
            )
            verify(notificationService).sendEventNotification(
                recipientId = "wholesaler-1",
                type = NotificationType.ORDER,
                eventKey = NotificationEventKey.ORDER_STATUS_UPDATED,
                entityType = NotificationEntityType.ORDER,
                entityId = case.orderId,
                preferenceKey = NotificationPreferenceKey.SHIPPING_STATUS,
                title = case.title,
                message = case.wholesalerMessage,
                deepLink = "/orders/${case.orderId}"
            )
        }
    }

    @Test
    fun `retailer can confirm own shipped order and inventory increases`() {
        val retailer = user(id = "retailer-1", email = "retailer@test.com", role = UserRole.RETAILER)
        val order = order(status = OrderStatus.SHIPPED, retailerId = retailer.id!!)
        val item = orderItem(quantity = 5)
        val inventoryItem = InventoryItem(
            id = "inventory-1",
            retailerId = retailer.id!!,
            productId = item.productId,
            currentQuantity = 3,
            autoOrderConfig = AutoOrderConfig()
        )

        stubRetailer(retailer)
        stubOrderForConfirmation(order)
        stubOrderItems(order.id!!, listOf(item))
        stubInventorySave()
        stubOrderSave()
        stubDtoMapping(order, retailer, item)
        `when`(inventoryItemRepository.findByRetailerIdAndProductId(retailer.id!!, item.productId)).thenReturn(inventoryItem)

        val result = service.confirmDeliveryForRetailer(order.id!!, retailer.email)

        assertEquals(OrderStatus.DELIVERED, result.status)
        assertEquals(OrderStatus.DELIVERED, order.status)
        assertNotNull(order.deliveredAt)
        assertEquals(8, inventoryItem.currentQuantity)
        verify(orderRepository).save(order)
        verify(notificationService).sendEventNotification(
            recipientId = retailer.id!!,
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_STATUS_UPDATED,
            entityType = NotificationEntityType.ORDER,
            entityId = order.id,
            preferenceKey = NotificationPreferenceKey.SHIPPING_STATUS,
            title = "Delivery confirmed",
            message = "You confirmed receipt and inventory was updated.",
            deepLink = "/orders/order-1"
        )
        verify(notificationService).sendEventNotification(
            recipientId = order.wholesalerId,
            type = NotificationType.ORDER,
            eventKey = NotificationEventKey.ORDER_STATUS_UPDATED,
            entityType = NotificationEntityType.ORDER,
            entityId = order.id,
            preferenceKey = NotificationPreferenceKey.SHIPPING_STATUS,
            title = "Delivery confirmed",
            message = "The retailer confirmed receipt of order order-1.",
            deepLink = "/orders/order-1"
        )
    }

    @Test
    fun `confirmation creates inventory item when retailer does not have product yet`() {
        val retailer = user(id = "retailer-1", email = "retailer@test.com", role = UserRole.RETAILER)
        val order = order(status = OrderStatus.SHIPPED, retailerId = retailer.id!!)
        val item = orderItem(quantity = 5)

        stubRetailer(retailer)
        stubOrderForConfirmation(order)
        stubOrderItems(order.id!!, listOf(item))
        stubInventorySave()
        stubOrderSave()
        stubDtoMapping(order, retailer, item)
        `when`(inventoryItemRepository.findByRetailerIdAndProductId(retailer.id!!, item.productId)).thenReturn(null)

        service.confirmDeliveryForRetailer(order.id!!, retailer.email)

        verify(inventoryItemRepository).save(any(InventoryItem::class.java))
    }

    @Test
    fun `second confirmation does not increase inventory again`() {
        val retailer = user(id = "retailer-1", email = "retailer@test.com", role = UserRole.RETAILER)
        val order = order(
            status = OrderStatus.DELIVERED,
            retailerId = retailer.id!!,
            deliveredAt = LocalDateTime.now().minusHours(1)
        )
        val item = orderItem(quantity = 5)

        stubRetailer(retailer)
        stubOrderForConfirmation(order)
        stubDtoMapping(order, retailer, item)

        val result = service.confirmDeliveryForRetailer(order.id!!, retailer.email)

        assertEquals(OrderStatus.DELIVERED, result.status)
        verify(orderRepository, never()).save(any(Order::class.java))
        verifyNoInteractions(orderItemRepository, inventoryItemRepository, inventoryAuditService, notificationService)
    }

    @Test
    fun `non-retailer cannot confirm delivery`() {
        val wholesaler = user(id = "wholesaler-1", email = "wh@test.com", role = UserRole.WHOLESALER)
        stubRetailer(wholesaler)

        assertThrows(AccessDeniedException::class.java) {
            service.confirmDeliveryForRetailer("order-1", wholesaler.email)
        }

        verifyNoInteractions(orderRepository)
    }

    @Test
    fun `retailer cannot confirm another retailer order`() {
        val retailer = user(id = "retailer-2", email = "retailer@test.com", role = UserRole.RETAILER)
        val order = order(status = OrderStatus.SHIPPED, retailerId = "retailer-1")

        stubRetailer(retailer)
        stubOrderForConfirmation(order)

        assertThrows(AccessDeniedException::class.java) {
            service.confirmDeliveryForRetailer(order.id!!, retailer.email)
        }

        verify(orderRepository, never()).save(any(Order::class.java))
        verifyNoInteractions(orderItemRepository, inventoryItemRepository, inventoryAuditService)
    }

    @Test
    fun `only shipped orders can be confirmed`() {
        val retailer = user(id = "retailer-1", email = "retailer@test.com", role = UserRole.RETAILER)
        stubRetailer(retailer)

        listOf(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.CANCELLED,
            OrderStatus.RETURNED
        ).forEach { status ->
            val order = order(id = "order-${status.name}", status = status, retailerId = retailer.id!!)
            stubOrderForConfirmation(order)

            assertThrows(IllegalArgumentException::class.java) {
                service.confirmDeliveryForRetailer(order.id!!, retailer.email)
            }
        }

        verify(orderRepository, never()).save(any(Order::class.java))
        verifyNoInteractions(orderItemRepository, inventoryItemRepository, inventoryAuditService)
    }

    private fun stubRetailer(user: User) {
        `when`(userRepository.findByEmail(user.email)).thenReturn(user)
    }

    private fun stubOrderForConfirmation(order: Order) {
        `when`(orderRepository.findByIdForUpdate(order.id!!)).thenReturn(order)
    }

    private fun stubOrderItems(orderId: String, items: List<OrderItem>) {
        `when`(orderItemRepository.findByOrderId(orderId)).thenReturn(items)
    }

    private fun stubInventorySave() {
        `when`(inventoryItemRepository.save(any(InventoryItem::class.java))).thenAnswer {
            it.arguments[0] as InventoryItem
        }
    }

    private fun stubOrderSave() {
        `when`(orderRepository.save(any(Order::class.java))).thenAnswer {
            it.arguments[0] as Order
        }
    }

    private fun stubDtoMapping(order: Order, retailer: User, item: OrderItem) {
        val product = product(item.productId)
        val category = Category(id = product.categoryId, name = "Beverages")
        val orderGroup = OrderGroup(
            id = order.orderGroupId!!,
            retailerId = retailer.id!!,
            groupTotal = order.subtotal,
            paymentStatus = PaymentStatus.COMPLETED
        )

        order.orderItems = mutableListOf(item)
        `when`(userRepository.findAllById(listOf(retailer.id!!))).thenReturn(listOf(retailer))
        `when`(orderGroupRepository.findAllById(listOf(orderGroup.id!!))).thenReturn(listOf(orderGroup))
        `when`(productRepository.findAllById(listOf(product.id!!))).thenReturn(listOf(product))
        `when`(categoryRepository.findAllById(listOf(category.id!!))).thenReturn(listOf(category))
    }

    private data class StatusNotificationCase(
        val status: OrderStatus,
        val orderId: String,
        val title: String,
        val retailerMessage: String,
        val wholesalerMessage: String
    )

    private fun order(
        id: String = "order-1",
        status: OrderStatus,
        retailerId: String,
        deliveredAt: LocalDateTime? = null
    ): Order {
        return Order(
            id = id,
            orderGroupId = "group-1",
            retailerId = retailerId,
            wholesalerId = "wholesaler-1",
            status = status,
            subtotal = BigDecimal("50.00"),
            deliveredAt = deliveredAt
        )
    }

    private fun orderItem(quantity: Int): OrderItem {
        return OrderItem(
            id = "item-1",
            orderId = "order-1",
            productId = "product-1",
            quantity = quantity,
            unitPrice = BigDecimal("10.00")
        )
    }

    private fun product(id: String): Product {
        return Product(
            id = id,
            wholesaler = user(id = "wholesaler-1", email = "wh@test.com", role = UserRole.WHOLESALER),
            name = "Canned Juice",
            description = "Case of juice cans",
            categoryId = "category-1",
            price = BigDecimal("10.00"),
            stockQuantity = 20,
            published = true
        )
    }

    private fun user(id: String, email: String, role: UserRole): User {
        return User(
            id = id,
            email = email,
            passwordHash = "hash",
            phoneNumber = "0500000000",
            role = role,
            representative = Representative(name = "User"),
            businessName = "Business",
            verificationStatus = true,
            commercialRegistrationNumber = "CR1"
        )
    }
}
