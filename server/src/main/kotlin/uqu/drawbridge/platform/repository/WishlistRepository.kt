package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uqu.drawbridge.platform.model.Wishlist

@Repository
interface WishlistRepository : JpaRepository<Wishlist, String> {
    fun findAllByUser_Id(userId: String): List<Wishlist>
    fun findByUser_IdAndProduct_Id(userId: String, productId: String): Wishlist?
    fun deleteByUser_IdAndProduct_Id(userId: String, productId: String)
    fun existsByUser_IdAndProduct_Id(userId: String, productId: String): Boolean
}