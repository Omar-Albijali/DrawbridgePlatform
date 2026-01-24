package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.CartItem

interface CartItemRepository : JpaRepository<CartItem, String> {
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :cartId")
    fun findByCartId(cartId: String): List<CartItem>
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :cartId AND ci.productId = :productId")
    fun findByCartIdAndProductId(cartId: String, productId: String): CartItem?
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cartId = :cartId AND ci.wholesalerId = :wholesalerId")
    fun findByCartIdAndWholesalerId(cartId: String, wholesalerId: String): List<CartItem>
    
    @Query("SELECT DISTINCT ci.wholesalerId FROM CartItem ci WHERE ci.cartId = :cartId")
    fun findDistinctWholesalerIdsByCartId(cartId: String): List<String>
}
