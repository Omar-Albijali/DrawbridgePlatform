package uqu.drawbridge.platform.service

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.model.CartItem
import uqu.drawbridge.platform.model.Product
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.ShoppingCart
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.CartItemRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.ShoppingCartRepository
import uqu.drawbridge.platform.repository.UserRepository

class CartServiceTest {
    private val shoppingCartRepository = mock(ShoppingCartRepository::class.java)
    private val cartItemRepository = mock(CartItemRepository::class.java)
    private val productRepository = mock(ProductRepository::class.java)
    private val orderService = mock(OrderService::class.java)
    private val userRepository = mock(UserRepository::class.java)

    private val service = CartService(
        shoppingCartRepository = shoppingCartRepository,
        cartItemRepository = cartItemRepository,
        productRepository = productRepository,
        orderService = orderService,
        userRepository = userRepository
    )

    @Test
    fun `add to cart below MOQ fails`() {
        `when`(productRepository.findById("prod-1")).thenReturn(Optional.of(product()))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.addToCart("retailer-1", "prod-1", 6)
        }

        assertEquals("Minimum order quantity for Coca Cola is 12 units.", exception.message)
        verifyNoInteractions(shoppingCartRepository)
    }

    @Test
    fun `update cart quantity below MOQ fails`() {
        val cart = cart()
        val item = cartItem(quantity = 12)
        `when`(shoppingCartRepository.findByRetailer_Id("retailer-1")).thenReturn(Optional.of(cart))
        `when`(cartItemRepository.findByCart_IdAndProduct_Id("cart-1", "prod-1")).thenReturn(item)
        `when`(productRepository.findById("prod-1")).thenReturn(Optional.of(product()))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.updateCartItemQuantity("retailer-1", "prod-1", 6)
        }

        assertEquals("Minimum order quantity for Coca Cola is 12 units.", exception.message)
        assertEquals(12, item.quantity)
    }

    @Test
    fun `checkout fails when cart item is below MOQ`() {
        val cart = cart()
        val item = cartItem(quantity = 6)
        `when`(shoppingCartRepository.findByRetailer_Id("retailer-1")).thenReturn(Optional.of(cart))
        `when`(cartItemRepository.findByCart_Id("cart-1")).thenReturn(listOf(item))
        `when`(productRepository.findById("prod-1")).thenReturn(Optional.of(product()))

        val exception = assertThrows(IllegalArgumentException::class.java) {
            service.checkout("retailer-1")
        }

        assertEquals("Minimum order quantity for Coca Cola is 12 units.", exception.message)
        verifyNoInteractions(orderService)
    }

    private fun cart(): ShoppingCart {
        return ShoppingCart(
            id = "cart-1",
            retailer = retailerUser(),
            updatedAt = LocalDateTime.now()
        )
    }

    private fun cartItem(quantity: Int): CartItem {
        return CartItem(
            id = "cart-item-1",
            cart = ShoppingCart(id = "cart-1", retailer = retailerUser()),
            product = product(),
            wholesaler = wholesalerUser(),
            quantity = quantity
        )
    }

    private fun retailerUser(): User {
        return User(
            id = "retailer-1",
            email = "rt@test.com",
            passwordHash = "hash",
            phoneNumber = "0500000000",
            role = UserRole.RETAILER,
            businessName = "Retailer",
            verificationStatus = true,
            commercialRegistrationNumber = "CR2"
        )
    }

    private fun product(): Product {
        return Product(
            id = "prod-1",
            wholesaler = wholesalerUser(),
            name = "Coca Cola",
            description = "Case of cans",
            category = uqu.drawbridge.platform.model.Category(id = "cat-1", name = "category"),
            price = BigDecimal("20.00"),
            stockQuantity = 20,
            minimumOrderQuantity = 12,
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
