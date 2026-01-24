package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.ProductDiscount
import java.time.LocalDateTime

interface ProductDiscountRepository : JpaRepository<ProductDiscount, String> {
    fun findByProductId(productId: String): List<ProductDiscount>
    
    @Query("""
        SELECT d FROM ProductDiscount d 
        WHERE d.productId = :productId 
        AND d.isActive = true 
        AND d.startDate <= :now 
        AND d.endDate >= :now
    """)
    fun findActiveDiscountsByProductId(productId: String, now: LocalDateTime = LocalDateTime.now()): List<ProductDiscount>
    
    fun deleteByProductId(productId: String)
}
