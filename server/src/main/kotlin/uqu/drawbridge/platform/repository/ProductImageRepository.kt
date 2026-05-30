package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.ProductImage

interface ProductImageRepository : JpaRepository<ProductImage, String> {
    fun findByProduct_Id(productId: String): List<ProductImage>
    fun deleteByProduct_Id(productId: String)
}
