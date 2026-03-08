package uqu.drawbridge.platform.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.WishlistDTO
import uqu.drawbridge.platform.service.WishlistService

@RestController
@RequestMapping("/api/wishlist")
class WishlistController(
    private val wishlistService: WishlistService
) {

    @GetMapping("/{userId}")
    fun getWishlist(@PathVariable userId: String): ResponseEntity<List<WishlistDTO>> {
        return ResponseEntity.ok(wishlistService.getWishlist(userId))
    }

    @GetMapping("/{userId}/{productId}/check")
    fun isInWishlist(
        @PathVariable userId: String,
        @PathVariable productId: String
    ): ResponseEntity<Map<String, Boolean>> {
        return ResponseEntity.ok(mapOf("inWishlist" to wishlistService.isInWishlist(userId, productId)))
    }

    @PostMapping("/{userId}/{productId}")
    fun addToWishlist(
        @PathVariable userId: String,
        @PathVariable productId: String
    ): ResponseEntity<WishlistDTO> {
        val item = wishlistService.addToWishlist(userId, productId)
            ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(item)
    }

    @DeleteMapping("/{userId}/{productId}")
    fun removeFromWishlist(
        @PathVariable userId: String,
        @PathVariable productId: String
    ): ResponseEntity<Void> {
        if (!wishlistService.removeFromWishlist(userId, productId)) {
            throw java.util.NoSuchElementException("Item not found in wishlist")
        }
        return ResponseEntity.ok().build()
    }
}