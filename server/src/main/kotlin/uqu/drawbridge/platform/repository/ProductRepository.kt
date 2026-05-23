package uqu.drawbridge.platform.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.Product

interface ProductRepository : JpaRepository<Product, String>, JpaSpecificationExecutor<Product> {
    @EntityGraph(attributePaths = ["wholesaler"])
    override fun findAll(spec: Specification<Product>, pageable: Pageable): Page<Product>

    fun findByWholesaler_Id(wholesalerId: String): List<Product>
    fun findByCategory_Id(categoryId: String): List<Product>
    fun findByPublishedTrue(): List<Product>
    fun findByNameContainingIgnoreCase(name: String): List<Product>
    fun findByGtin(gtin: String): Product?

    @Query(
        """
        SELECT DISTINCT p.wholesaler.businessName
        FROM Product p
        WHERE p.published = true
        ORDER BY p.wholesaler.businessName ASC
        """
    )
    fun findDistinctPublishedBrands(): List<String>
}
