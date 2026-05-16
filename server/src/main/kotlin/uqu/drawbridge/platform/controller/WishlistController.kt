package uqu.drawbridge.platform.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.WishlistDTO
import uqu.drawbridge.platform.service.WishlistService
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/wishlist")
class WishlistController(
    private val wishlistService: WishlistService,
    private val userService: UserService,
) {

    @GetMapping("/{userId}")
    fun getWishlist(
        authentication: Authentication,
        @PathVariable userId: String,
    ): ResponseEntity<List<WishlistDTO>> {
        requireWishlistOwner(authentication, userId)
        return ResponseEntity.ok(wishlistService.getWishlist(userId))
    }

    @GetMapping("/{userId}/{productId}/check")
    fun isInWishlist(
        authentication: Authentication,
        @PathVariable userId: String,
        @PathVariable productId: String
    ): ResponseEntity<Map<String, Boolean>> {
        requireWishlistOwner(authentication, userId)
        return ResponseEntity.ok(mapOf("inWishlist" to wishlistService.isInWishlist(userId, productId)))
    }

    @PostMapping("/{userId}/{productId}")
    fun addToWishlist(
        authentication: Authentication,
        @PathVariable userId: String,
        @PathVariable productId: String
    ): ResponseEntity<WishlistDTO> {
        requireWishlistOwner(authentication, userId)
        val item = wishlistService.addToWishlist(userId, productId)
            ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(item)
    }

    @DeleteMapping("/{userId}/{productId}")
    fun removeFromWishlist(
        authentication: Authentication,
        @PathVariable userId: String,
        @PathVariable productId: String
    ): ResponseEntity<Void> {
        requireWishlistOwner(authentication, userId)
        if (!wishlistService.removeFromWishlist(userId, productId)) {
            throw java.util.NoSuchElementException("Item not found in wishlist")
        }
        return ResponseEntity.ok().build()
    }

    private fun requireWishlistOwner(authentication: Authentication, userId: String) {
        val user = userService.getUserByEmail(authentication.name)
            ?: throw AccessDeniedException("Access denied")

        if (user.id != userId || user.role != UserRole.RETAILER) {
            throw AccessDeniedException("Access denied")
        }
    }
}
