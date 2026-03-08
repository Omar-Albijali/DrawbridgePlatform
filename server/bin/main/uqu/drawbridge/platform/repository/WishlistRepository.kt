package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uqu.drawbridge.platform.model.Wishlist

@Repository
interface WishlistRepository : JpaRepository<Wishlist, String> {
    fun findAllByUserId(userId: String): List<Wishlist>
    fun findByUserIdAndProductId(userId: String, productId: String): Wishlist?
    fun deleteByUserIdAndProductId(userId: String, productId: String)
    fun existsByUserIdAndProductId(userId: String, productId: String): Boolean
}