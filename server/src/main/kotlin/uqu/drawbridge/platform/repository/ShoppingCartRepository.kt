package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.ShoppingCart
import java.util.Optional

interface ShoppingCartRepository : JpaRepository<ShoppingCart, String> {
    fun findByRetailer_Id(retailerId: String): Optional<ShoppingCart>
    fun existsByRetailer_Id(retailerId: String): Boolean
}
