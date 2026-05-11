package uqu.drawbridge.platform.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import uqu.drawbridge.platform.ScheduleType
import uqu.drawbridge.platform.UpdateAutoOrderConfigRequest
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.model.AutoOrderConfig
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.model.Product
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.ProductRepository
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.Optional

class InventoryServiceTest {

    private val inventoryItemRepository = mock(InventoryItemRepository::class.java)
    private val productRepository = mock(ProductRepository::class.java)
    private val orderService = mock(OrderService::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val inventoryAuditService = mock(InventoryAuditService::class.java)

    private val service = InventoryService(
        inventoryItemRepository = inventoryItemRepository,
        productRepository = productRepository,
        orderService = orderService,
        notificationService = notificationService,
        inventoryAuditService = inventoryAuditService
    )

    @Test
    fun `daily schedule calculates next scheduled at`() {
        val item = inventoryWithConfig("inv-daily", ScheduleType.THRESHOLD_BASED)
        stubInventoryFindAndSave(item)

        val before = LocalDateTime.now()
        val dto = service.updateAutoOrderConfigFromRequest(
            item.id!!,
            UpdateAutoOrderConfigRequest(
                enabled = true,
                minThreshold = 5,
                reorderQuantity = 10,
                scheduleType = ScheduleType.DAILY,
                intervalDays = null,
                dayOfWeek = null,
                dayOfMonth = null
            )
        )

        val next = LocalDateTime.parse(dto!!.nextScheduledAt)
        assertTrue(next.isAfter(before))
        assertEquals(9, next.hour)
        assertEquals(0, next.minute)
        assertEquals(0, next.second)
    }

    @Test
    fun `weekly schedule supports weekday names from UI`() {
        val item = inventoryWithConfig("inv-weekly", ScheduleType.THRESHOLD_BASED)
        stubInventoryFindAndSave(item)

        val before = LocalDateTime.now()
        val dto = service.updateAutoOrderConfigFromRequest(
            item.id!!,
            UpdateAutoOrderConfigRequest(
                enabled = true,
                minThreshold = 5,
                reorderQuantity = 10,
                scheduleType = ScheduleType.WEEKLY,
                intervalDays = null,
                dayOfWeek = "MONDAY",
                dayOfMonth = null
            )
        )

        val next = LocalDateTime.parse(dto!!.nextScheduledAt)
        assertTrue(next.isAfter(before))
        assertEquals(DayOfWeek.MONDAY, next.dayOfWeek)
    }

    @Test
    fun `monthly schedule calculates day from request`() {
        val item = inventoryWithConfig("inv-monthly", ScheduleType.THRESHOLD_BASED)
        stubInventoryFindAndSave(item)

        val dto = service.updateAutoOrderConfigFromRequest(
            item.id!!,
            UpdateAutoOrderConfigRequest(
                enabled = true,
                minThreshold = 5,
                reorderQuantity = 10,
                scheduleType = ScheduleType.MONTHLY,
                intervalDays = null,
                dayOfWeek = null,
                dayOfMonth = "15"
            )
        )

        val next = LocalDateTime.parse(dto!!.nextScheduledAt)
        assertEquals(15, next.dayOfMonth)
        assertEquals(9, next.hour)
    }

    @Test
    fun `interval days schedule advances by interval`() {
        val item = inventoryWithConfig("inv-interval", ScheduleType.THRESHOLD_BASED)
        stubInventoryFindAndSave(item)

        val before = LocalDateTime.now()
        val dto = service.updateAutoOrderConfigFromRequest(
            item.id!!,
            UpdateAutoOrderConfigRequest(
                enabled = true,
                minThreshold = 5,
                reorderQuantity = 10,
                scheduleType = ScheduleType.INTERVAL_DAYS,
                intervalDays = 3,
                dayOfWeek = null,
                dayOfMonth = null
            )
        )

        val next = LocalDateTime.parse(dto!!.nextScheduledAt)
        val days = java.time.Duration.between(before.toLocalDate().atStartOfDay(), next.toLocalDate().atStartOfDay()).toDays()
        assertTrue(days in 3..4)
    }

    @Test
    fun `auto order reorder quantity below product MOQ fails`() {
        val item = inventoryWithConfig("inv-moq", ScheduleType.THRESHOLD_BASED)
        stubInventoryFindAndSave(item)
        val product = product(minimumOrderQuantity = 12, stockQuantity = 100)
        `when`(productRepository.findById("prod-1")).thenReturn(Optional.of(product))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.updateAutoOrderConfigFromRequest(
                item.id!!,
                UpdateAutoOrderConfigRequest(
                    enabled = true,
                    minThreshold = 5,
                    reorderQuantity = 10,
                    scheduleType = ScheduleType.DAILY,
                    intervalDays = null,
                    dayOfWeek = null,
                    dayOfMonth = null
                )
            )
        }

        assertEquals("Minimum order quantity for Beans is 12 units.", exception.message)
    }

    @Test
    fun `processDueAutoOrders ignores threshold and processes non-threshold`() {
        val serviceSpy = spy(service)
        val dueDaily = inventoryWithConfig("inv-due-daily", ScheduleType.DAILY).apply {
            autoOrderConfig.reorderQuantity = 7
            autoOrderConfig.enabled = true
            autoOrderConfig.nextScheduledAt = LocalDateTime.now().minusMinutes(1)
        }
        val dueThreshold = inventoryWithConfig("inv-due-threshold", ScheduleType.THRESHOLD_BASED).apply {
            autoOrderConfig.reorderQuantity = 11
            autoOrderConfig.enabled = true
            autoOrderConfig.nextScheduledAt = LocalDateTime.now().minusMinutes(1)
        }

        doReturn(listOf(dueDaily, dueThreshold)).`when`(serviceSpy).getAutoOrderConfigsDueForProcessing()
        `when`(inventoryItemRepository.findById("inv-due-daily")).thenReturn(Optional.of(dueDaily))
        `when`(inventoryItemRepository.save(any(InventoryItem::class.java))).thenAnswer { it.arguments[0] as InventoryItem }

        val product = product(stockQuantity = 10)
        `when`(productRepository.findById("prod-1")).thenReturn(Optional.of(product))
        `when`(
            orderService.createAutoRestockOrder(
                retailerId = "retailer-1",
                wholesalerId = "wh-1",
                productId = "prod-1",
                quantity = 7,
                unitPrice = BigDecimal("20.00")
            )
        ).thenReturn(
            uqu.drawbridge.platform.model.Order(
                id = "ord-1",
                retailerId = "retailer-1",
                wholesalerId = "wh-1",
                subtotal = BigDecimal("140.00")
            )
        )

        val processed = serviceSpy.processDueAutoOrders()

        assertEquals(1, processed)
        verify(orderService, times(1)).createAutoRestockOrder(
            retailerId = "retailer-1",
            wholesalerId = "wh-1",
            productId = "prod-1",
            quantity = 7,
            unitPrice = BigDecimal("20.00")
        )
        assertNotNull(dueDaily.autoOrderConfig.lastTriggeredAt)
    }

    private fun inventoryWithConfig(id: String, scheduleType: ScheduleType): InventoryItem {
        return InventoryItem(
            id = id,
            retailerId = "retailer-1",
            productId = "prod-1",
            currentQuantity = 25,
            lastUpdated = LocalDateTime.now(),
            autoOrderConfig = AutoOrderConfig(
                enabled = true,
                minThreshold = 5,
                reorderQuantity = 10,
                scheduleType = scheduleType
            )
        )
    }

    private fun stubInventoryFindAndSave(item: InventoryItem) {
        `when`(inventoryItemRepository.findById(item.id!!)).thenReturn(Optional.of(item))
        `when`(inventoryItemRepository.save(any(InventoryItem::class.java))).thenAnswer { it.arguments[0] as InventoryItem }
        `when`(productRepository.findById(item.productId)).thenReturn(Optional.of(product()))
    }

    private fun product(minimumOrderQuantity: Int = 1, stockQuantity: Int = 100): Product {
        return Product(
            id = "prod-1",
            wholesaler = wholesalerUser(),
            name = "Beans",
            description = "Desc",
            categoryId = "cat-1",
            price = BigDecimal("20.00"),
            stockQuantity = stockQuantity,
            minimumOrderQuantity = minimumOrderQuantity,
            published = true
        )
    }

    private fun wholesalerUser(): User {
        return User(
            id = "wh-1",
            email = "wh@test.com",
            passwordHash = "hash",
            phoneNumber = "0500000000",
            role = UserRole.WHOLESALER,
            representative = Representative(),
            businessName = "Wholesaler",
            verificationStatus = true,
            commercialRegistrationNumber = "CR1"
        )
    }
}
