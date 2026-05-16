package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.service.CartService
import uqu.drawbridge.platform.service.OrderService
import uqu.drawbridge.platform.service.UserService
import uqu.drawbridge.platform.validation.RequestValidation

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService,
    private val orderService: OrderService,
    private val userService: UserService
) {

    @GetMapping("/{retailerId}")
    fun getCart(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<ShoppingCartDTO> {
        requireCartOwner(authentication, retailerId)
        val cart = cartService.getOrCreateCartDTO(retailerId)
        return ResponseEntity.ok(cart)
    }

    @GetMapping("/{retailerId}/items")
    fun getCartItems(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<List<CartItemDTO>> {
        requireCartOwner(authentication, retailerId)
        return ResponseEntity.ok(cartService.getCartItemsDTO(retailerId))
    }

    @GetMapping("/{retailerId}/count")
    fun getCartItemCount(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<Map<String, Int>> {
        requireCartOwner(authentication, retailerId)
        return ResponseEntity.ok(mapOf("count" to cartService.getCartItemCount(retailerId)))
    }

    @PostMapping("/{retailerId}/items")
    fun addToCart(
        authentication: Authentication,
        @PathVariable retailerId: String,
        @RequestBody request: AddToCartRequest
    ): ResponseEntity<CartItemDTO> {
        requireCartOwner(authentication, retailerId)
        RequestValidation.requireNotBlank(retailerId, "retailerId")
        RequestValidation.requireNotBlank(request.productId, "productId")
        RequestValidation.requirePositive(request.quantity, "quantity")
        val item = cartService.addToCartDTO(retailerId, request.productId, request.quantity)
            ?: throw IllegalArgumentException("Failed to add item to cart. Product not found?")
        return ResponseEntity.ok(item)

    }

    @PutMapping("/{retailerId}/items/{productId}")
    fun updateCartItemQuantity(
        authentication: Authentication,
        @PathVariable retailerId: String,
        @PathVariable productId: String,
        @RequestParam quantity: Int
    ): ResponseEntity<CartItemDTO> {
        requireCartOwner(authentication, retailerId)
        RequestValidation.requireNotBlank(retailerId, "retailerId")
        RequestValidation.requireNotBlank(productId, "productId")
        RequestValidation.requirePositive(quantity, "quantity")
        val item = cartService.updateCartItemQuantityDTO(retailerId, productId, quantity)
            ?: throw java.util.NoSuchElementException("Item not found in cart")
        return ResponseEntity.ok(item)
    }

    @DeleteMapping("/{retailerId}/items/{productId}")
    fun removeFromCart(
        authentication: Authentication,
        @PathVariable retailerId: String,
        @PathVariable productId: String
    ): ResponseEntity<Void> {
        requireCartOwner(authentication, retailerId)
        if (!cartService.removeFromCart(retailerId, productId)) {
            throw java.util.NoSuchElementException("Item not found in cart")
        }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{retailerId}")
    fun clearCart(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<Void> {
        requireCartOwner(authentication, retailerId)
        cartService.clearCart(retailerId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{retailerId}/checkout")
    fun checkout(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<OrderGroupDTO> {
        requireCartOwner(authentication, retailerId)
        val orderGroup = cartService.checkout(retailerId)
            ?: throw IllegalArgumentException("Checkout failed. Cart might be empty.")
        return ResponseEntity.ok(orderService.mapToDTO(orderGroup))
    }

    private fun requireCartOwner(authentication: Authentication, retailerId: String) {
        val user = userService.getUserByEmail(authentication.name)
            ?: throw AccessDeniedException("Access denied")

        if (user.id != retailerId || user.role != UserRole.RETAILER) {
            throw AccessDeniedException("Access denied")
        }
    }
}
