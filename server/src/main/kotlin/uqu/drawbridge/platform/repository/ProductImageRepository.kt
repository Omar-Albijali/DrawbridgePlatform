package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.ProductImage

interface ProductImageRepository : JpaRepository<ProductImage, String> {
    fun findByProductId(productId: String): List<ProductImage>
    fun deleteByProductId(productId: String)
}
