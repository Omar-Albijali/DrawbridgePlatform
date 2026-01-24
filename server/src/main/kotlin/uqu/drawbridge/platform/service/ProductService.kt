package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.CategoryRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.*

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val productDiscountService: ProductDiscountService
) {

    // ==================== PRODUCT OPERATIONS ====================

    fun getAllProducts(): List<Product> = productRepository.findAll()

    fun getProductById(id: String): Product? = productRepository.findById(id).orElse(null)

    fun getProductsByWholesaler(wholesalerId: String): List<Product> = 
        productRepository.findByWholesalerId(wholesalerId)

    fun getProductsByCategory(categoryId: String): List<Product> = 
        productRepository.findByCategoryId(categoryId)

    fun searchProducts(query: String): List<Product> = 
        productRepository.findByNameContainingIgnoreCase(query)

    @Transactional
    fun createProduct(product: Product): Product = productRepository.save(product)

    @Transactional
    fun updateProduct(id: String, product: Product): Product? {
        return if (productRepository.existsById(id)) {
            product.id = id
            productRepository.save(product)
        } else {
            null
        }
    }

    @Transactional
    fun deleteProduct(id: String): Boolean {
        return if (productRepository.existsById(id)) {
            productRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== CATEGORY OPERATIONS ====================

    fun getAllCategories(): List<Category> = categoryRepository.findAll()

    fun getCategoryById(id: String): Category? = categoryRepository.findById(id).orElse(null)

    fun getCategoriesByParent(parentId: String): List<Category> = 
        categoryRepository.findByParentCategoryId(parentId)

    @Transactional
    fun createCategory(category: Category): Category = categoryRepository.save(category)

    @Transactional
    fun deleteCategory(id: String): Boolean {
        return if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== DTO MAPPING ====================

    fun Product.toDTO(): ProductDTO {
        val category = categoryRepository.findById(this.categoryId).orElse(null)
        val discount = productDiscountService.getBestActiveDiscount(this.id!!)
        val originalPrice = if (discount != null) {
            this.price.toDouble()
        } else {
            null
        }
        val effectivePrice = if (discount != null) {
            val discountFactor = java.math.BigDecimal.ONE - (discount.discountPercentage / java.math.BigDecimal(100))
            (this.price * discountFactor).toDouble()
        } else {
            this.price.toDouble()
        }
        
        return ProductDTO(
            id = (this.id ?: ""),
            name = this.name,
            description = this.description,
            price = effectivePrice,
            originalPrice = originalPrice,
            image = this.images.firstOrNull()?.url ?: "",
            category = category?.name ?: "",
            brand = "", // Brand is not in the entity
            stock = this.stockQuantity,
            rating = this.averageRating.toDouble(),
            reviews = this.ratingCount,
            supplier = this.wholesaler.businessName
        )
    }

    fun Category.toDTO() = CategoryDTO(
        id = (this.id ?: ""),
        name = this.name,
        parentCategoryId = this.parentCategoryId
    )

    // ==================== DTO-RETURNING METHODS ====================

    fun getAllProductsDTO(): List<ProductDTO> = getAllProducts().map { it.toDTO() }

    fun getProductDTOById(id: String): ProductDTO? = getProductById(id)?.toDTO()

    fun getProductsDTOByWholesaler(wholesalerId: String): List<ProductDTO> = 
        getProductsByWholesaler(wholesalerId).map { it.toDTO() }

    fun getProductsDTOByCategory(categoryId: String): List<ProductDTO> = 
        getProductsByCategory(categoryId).map { it.toDTO() }

    fun searchProductsDTO(query: String): List<ProductDTO> = 
        searchProducts(query).map { it.toDTO() }

    fun createProductDTO(product: Product): ProductDTO = createProduct(product).toDTO()

    fun updateProductDTO(id: String, product: Product): ProductDTO? = updateProduct(id, product)?.toDTO()

    fun getAllCategoriesDTO(): List<CategoryDTO> = getAllCategories().map { it.toDTO() }

    fun getCategoryDTOById(id: String): CategoryDTO? = getCategoryById(id)?.toDTO()

    fun getCategoriesDTOByParent(parentId: String): List<CategoryDTO> = 
        getCategoriesByParent(parentId).map { it.toDTO() }

    fun createCategoryDTO(category: Category): CategoryDTO = createCategory(category).toDTO()
}
