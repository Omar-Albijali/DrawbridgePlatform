package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.ProductRating
import java.math.BigDecimal

interface ProductRatingRepository : JpaRepository<ProductRating, String> {
    fun findByProductId(productId: String): List<ProductRating>
    fun findByUserId(userId: String): List<ProductRating>
    fun findByProductIdAndUserId(productId: String, userId: String): ProductRating?
    fun countByProductId(productId: String): Int
    
    @Query("SELECT AVG(r.rating) FROM ProductRating r WHERE r.productId = :productId")
    fun getAverageRatingByProductId(productId: String): BigDecimal?
}
