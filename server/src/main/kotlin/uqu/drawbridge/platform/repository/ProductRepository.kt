package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Product

interface ProductRepository : JpaRepository<Product, String> {
    fun findByWholesalerId(wholesalerId: String): List<Product>
    fun findByCategoryId(categoryId: String): List<Product>
    fun findByPublishedTrue(): List<Product>
    fun findByNameContainingIgnoreCase(name: String): List<Product>
}
