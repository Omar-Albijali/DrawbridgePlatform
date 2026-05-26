package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.WishlistDTO
import uqu.drawbridge.platform.model.Wishlist
import uqu.drawbridge.platform.repository.WishlistRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository

@Service
class WishlistService(
    private val wishlistRepository: WishlistRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository
) {

    fun getWishlist(userId: String): List<WishlistDTO> {
        return wishlistRepository.findAllByUser_Id(userId).mapNotNull { wishlistItem ->
            val product = productRepository.findById(wishlistItem.productId).orElse(null) ?: return@mapNotNull null
            WishlistDTO(
                id = wishlistItem.id ?: "",
                userId = wishlistItem.userId,
                productId = wishlistItem.productId,
                productName = product.name,
                productPrice = product.price.toDouble(),
                productImage = product.images.minByOrNull { it.sortIndex }?.url ?: "",
                createdAt = wishlistItem.createdAt.toString()
            )
        }
    }

    fun isInWishlist(userId: String, productId: String): Boolean {
        return wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId)
    }

    @Transactional
    fun addToWishlist(userId: String, productId: String): WishlistDTO? {
        if (wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            return null
        }
        val product = productRepository.findById(productId).orElse(null) ?: return null
        val user = userRepository.getReferenceById(userId)
        val wishlist = wishlistRepository.save(
            Wishlist(user = user, product = product)
        )
        return WishlistDTO(
            id = wishlist.id ?: "",
            userId = wishlist.userId,
            productId = wishlist.productId,
            productName = product.name,
            productPrice = product.price.toDouble(),
            productImage = product.images.minByOrNull { it.sortIndex }?.url ?: "",
            createdAt = wishlist.createdAt.toString()
        )
    }

    @Transactional
    fun removeFromWishlist(userId: String, productId: String): Boolean {
        if (!wishlistRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            return false
        }
        wishlistRepository.deleteByUser_IdAndProduct_Id(userId, productId)
        return true
    }
}