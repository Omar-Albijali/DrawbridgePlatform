package uqu.drawbridge.platform.service

import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import uqu.drawbridge.platform.CreateProductRequest
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.model.Category
import uqu.drawbridge.platform.model.Product
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.CategoryRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository

class ProductServiceTest {
    private val productRepository = mock(ProductRepository::class.java)
    private val categoryRepository = mock(CategoryRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val inventoryAuditService = mock(InventoryAuditService::class.java)

    private val service = ProductService(
        productRepository = productRepository,
        categoryRepository = categoryRepository,
        userRepository = userRepository,
        inventoryAuditService = inventoryAuditService
    )

    @Test
    fun `product creation with MOQ succeeds`() {
        `when`(userRepository.findById("wh-1")).thenReturn(Optional.of(wholesalerUser()))
        `when`(categoryRepository.findById("cat-1")).thenReturn(Optional.of(Category(id = "cat-1", name = "Drinks")))
        `when`(productRepository.save(any(Product::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as Product).apply { id = "prod-1" }
        }

        val dto = service.createProductFromRequest(productRequest(minimumOrderQuantity = 12))

        assertEquals("prod-1", dto.id)
        assertEquals(12, dto.minimumOrderQuantity)
    }

    @Test
    fun `product creation with MOQ below one fails`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.createProductFromRequest(productRequest(minimumOrderQuantity = 0))
        }

        assertEquals("minimumOrderQuantity must be greater than zero", exception.message)
    }

    @Test
    fun `product update with MOQ below one fails`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.updateProductFromRequest("prod-1", productRequest(minimumOrderQuantity = 0))
        }

        assertEquals("minimumOrderQuantity must be greater than zero", exception.message)
    }

    private fun productRequest(minimumOrderQuantity: Int): CreateProductRequest {
        return CreateProductRequest(
            name = "Coca Cola",
            description = "Case of cans",
            price = 25.0,
            image = "",
            category = "Drinks",
            categoryId = "cat-1",
            wholesalerId = "wh-1",
            brand = "",
            stock = 100,
            minimumOrderQuantity = minimumOrderQuantity,
            gtin = "123456789"
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
