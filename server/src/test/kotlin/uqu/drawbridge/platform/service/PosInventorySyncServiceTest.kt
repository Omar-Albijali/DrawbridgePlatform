package uqu.drawbridge.platform.service

import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.dto.PosInventoryChangeRequest
import uqu.drawbridge.platform.dto.PosInventoryChangeType
import uqu.drawbridge.platform.model.AutoOrderConfig
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.model.PosEventReceipt
import uqu.drawbridge.platform.model.Product
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.PosEventReceiptRepository
import uqu.drawbridge.platform.repository.ProductRepository

class PosInventorySyncServiceTest {
    private val productRepository = mock(ProductRepository::class.java)
    private val inventoryItemRepository = mock(InventoryItemRepository::class.java)
    private val posEventReceiptRepository = mock(PosEventReceiptRepository::class.java)
    private val inventoryService = mock(InventoryService::class.java)

    private val service = PosInventorySyncService(
        productRepository = productRepository,
        inventoryItemRepository = inventoryItemRepository,
        posEventReceiptRepository = posEventReceiptRepository,
        inventoryService = inventoryService
    )

    @Test
    fun `delta request updates inventory and records receipt`() {
        val retailerId = "ret-1"
        val product = product("prod-1", "123456")
        val inventory = inventoryItem("inv-1", retailerId, product.id!!, 10)
        val pendingReceipt = PosEventReceipt(
            id = "receipt-1",
            retailerId = retailerId,
            eventId = "evt-1",
            eventType = "inventory.changed",
            processedAt = LocalDateTime.now()
        )
        val updatedInventory = inventoryItem("inv-1", retailerId, product.id!!, 7)

        `when`(posEventReceiptRepository.existsByRetailerIdAndEventIdAndEventType(retailerId, "evt-1", "inventory.changed"))
            .thenReturn(false)
        `when`(posEventReceiptRepository.saveAndFlush(any(PosEventReceipt::class.java))).thenReturn(pendingReceipt)
        `when`(productRepository.findByGtin("123456")).thenReturn(product)
        `when`(inventoryItemRepository.findByRetailerIdAndProductId(retailerId, "prod-1")).thenReturn(inventory)
        `when`(
            inventoryService.adjustQuantity(
                "inv-1",
                -3,
                uqu.drawbridge.platform.model.InventoryAuditSourceType.POS,
                "evt-1",
                "POS sale"
            )
        ).thenReturn(updatedInventory)

        val response = service.applyIncomingInventoryChange(
            authenticatedRetailerId = retailerId,
            request = PosInventoryChangeRequest(
                eventId = "evt-1",
                retailerId = retailerId,
                gtin = "123456",
                changeType = PosInventoryChangeType.DELTA,
                quantityDelta = -3,
                reason = "POS sale"
            )
        )

        assertFalse(response.alreadyProcessed)
        assertEquals("evt-1", response.eventId)
        assertEquals(10, response.quantityBefore)
        assertEquals(7, response.quantityAfter)
        verify(posEventReceiptRepository, times(1)).save(any(PosEventReceipt::class.java))
    }

    @Test
    fun `duplicate event returns already processed and skips mutation`() {
        val retailerId = "ret-1"
        `when`(posEventReceiptRepository.existsByRetailerIdAndEventIdAndEventType(retailerId, "evt-dup", "inventory.changed"))
            .thenReturn(true)

        val response = service.applyIncomingInventoryChange(
            authenticatedRetailerId = retailerId,
            request = PosInventoryChangeRequest(
                eventId = "evt-dup",
                retailerId = retailerId,
                gtin = "123456",
                changeType = PosInventoryChangeType.SET,
                quantityAfter = 5
            )
        )

        assertTrue(response.alreadyProcessed)
        assertEquals("evt-dup", response.eventId)
        verifyNoInteractions(inventoryService)
    }

    private fun product(id: String, gtin: String): Product {
        return Product(
            id = id,
            wholesaler = User(
                id = "wh-1",
                email = "wh@test.com",
                passwordHash = "hash",
                phoneNumber = "0500000000",
                role = UserRole.WHOLESALER,
                representative = Representative(),
                businessName = "Wholesaler",
                verificationStatus = true,
                commercialRegistrationNumber = "CR1"
            ),
            name = "Beans",
            description = "Desc",
            categoryId = "cat-1",
            price = BigDecimal("20.00"),
            stockQuantity = 10,
            gtin = gtin,
            published = true
        )
    }

    private fun inventoryItem(id: String, retailerId: String, productId: String, quantity: Int): InventoryItem {
        return InventoryItem(
            id = id,
            retailerId = retailerId,
            productId = productId,
            currentQuantity = quantity,
            autoOrderConfig = AutoOrderConfig()
        )
    }
}
