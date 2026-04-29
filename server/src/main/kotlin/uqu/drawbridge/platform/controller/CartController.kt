package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.service.CartService
import uqu.drawbridge.platform.service.OrderService
import uqu.drawbridge.platform.validation.RequestValidation

@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService,
    private val orderService: OrderService
) {

    @GetMapping("/{retailerId}")
    fun getCart(@PathVariable retailerId: String): ResponseEntity<ShoppingCartDTO> {
        val cart = cartService.getOrCreateCartDTO(retailerId)
        return ResponseEntity.ok(cart)
    }

    @GetMapping("/{retailerId}/items")
    fun getCartItems(@PathVariable retailerId: String): ResponseEntity<List<CartItemDTO>> {
        return ResponseEntity.ok(cartService.getCartItemsDTO(retailerId))
    }

    @GetMapping("/{retailerId}/count")
    fun getCartItemCount(@PathVariable retailerId: String): ResponseEntity<Map<String, Int>> {
        return ResponseEntity.ok(mapOf("count" to cartService.getCartItemCount(retailerId)))
    }

    @PostMapping("/{retailerId}/items")
    fun addToCart(
        @PathVariable retailerId: String,
        @RequestBody request: AddToCartRequest
    ): ResponseEntity<CartItemDTO> {
        RequestValidation.requireNotBlank(retailerId, "retailerId")
        RequestValidation.requireNotBlank(request.productId, "productId")
        RequestValidation.requirePositive(request.quantity, "quantity")
        val item = cartService.addToCartDTO(retailerId, request.productId, request.quantity)
            ?: throw IllegalArgumentException("Failed to add item to cart. Product not found?")
        return ResponseEntity.ok(item)

    }

    @PutMapping("/{retailerId}/items/{productId}")
    fun updateCartItemQuantity(
        @PathVariable retailerId: String,
        @PathVariable productId: String,
        @RequestParam quantity: Int
    ): ResponseEntity<CartItemDTO> {
        RequestValidation.requireNotBlank(retailerId, "retailerId")
        RequestValidation.requireNotBlank(productId, "productId")
        RequestValidation.requirePositive(quantity, "quantity")
        val item = cartService.updateCartItemQuantityDTO(retailerId, productId, quantity)
            ?: throw java.util.NoSuchElementException("Item not found in cart")
        return ResponseEntity.ok(item)
    }

    @DeleteMapping("/{retailerId}/items/{productId}")
    fun removeFromCart(
        @PathVariable retailerId: String,
        @PathVariable productId: String
    ): ResponseEntity<Void> {
        if (!cartService.removeFromCart(retailerId, productId)) {
            throw java.util.NoSuchElementException("Item not found in cart")
        }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{retailerId}")
    fun clearCart(@PathVariable retailerId: String): ResponseEntity<Void> {
        cartService.clearCart(retailerId)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{retailerId}/checkout")
    fun checkout(@PathVariable retailerId: String): ResponseEntity<OrderGroupDTO> {
        val orderGroup = cartService.checkout(retailerId)
            ?: throw IllegalArgumentException("Checkout failed. Cart might be empty.")
        return ResponseEntity.ok(orderService.mapToDTO(orderGroup))
    }
}
