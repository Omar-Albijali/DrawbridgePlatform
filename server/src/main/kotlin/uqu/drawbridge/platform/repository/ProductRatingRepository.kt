package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.ProductRating
import java.math.BigDecimal

interface ProductRatingRepository : JpaRepository<ProductRating, String> {
    fun findByProduct_Id(productId: String): List<ProductRating>
    fun findByUser_Id(userId: String): List<ProductRating>
    fun findByProduct_IdAndUser_Id(productId: String, userId: String): ProductRating?
    fun countByProduct_Id(productId: String): Int
    
    @Query("SELECT AVG(r.rating) FROM ProductRating r WHERE r.product.id = :productId")
    fun getAverageRatingByProductId(productId: String): BigDecimal?
}
