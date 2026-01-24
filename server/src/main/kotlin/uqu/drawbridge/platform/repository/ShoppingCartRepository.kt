package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.ShoppingCart
import java.util.Optional

interface ShoppingCartRepository : JpaRepository<ShoppingCart, String> {
    fun findByRetailerId(retailerId: String): Optional<ShoppingCart>
    fun existsByRetailerId(retailerId: String): Boolean
}
