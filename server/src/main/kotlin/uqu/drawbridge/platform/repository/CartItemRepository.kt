package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.CartItem

interface CartItemRepository : JpaRepository<CartItem, String> {
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId")
    fun findByCart_Id(cartId: String): List<CartItem>
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId")
    fun findByCart_IdAndProduct_Id(cartId: String, productId: String): CartItem?
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.wholesaler.id = :wholesalerId")
    fun findByCart_IdAndWholesaler_Id(cartId: String, wholesalerId: String): List<CartItem>
    
    @Query("SELECT DISTINCT ci.wholesaler.id FROM CartItem ci WHERE ci.cart.id = :cartId")
    fun findDistinctWholesalerIdsByCart_Id(cartId: String): List<String>
}
