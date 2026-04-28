package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.CategoryRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.*
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val productDiscountService: ProductDiscountService,
    private val userRepository: UserRepository,
    private val inventoryAuditService: InventoryAuditService
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

    fun getPublishedProducts(): List<Product> =
        productRepository.findByPublishedTrue()

    fun searchPublishedProducts(query: String): List<Product> =
        productRepository.findByNameContainingIgnoreCase(query).filter { it.published }

    fun getPublishedProductsByCategory(categoryId: String): List<Product> =
        productRepository.findByCategoryId(categoryId).filter { it.published }

    @Transactional
    fun createProduct(product: Product): Product {
        val savedProduct = productRepository.save(product)
        logProductStockChange(savedProduct, 0, savedProduct.stockQuantity, "Product created")
        return savedProduct
    }

    @Transactional
    fun updateProduct(id: String, product: Product): Product? {
        val existingProduct = productRepository.findById(id).orElse(null)
        return if (existingProduct != null) {
            val previousStockQuantity = existingProduct.stockQuantity
            product.id = id
            val savedProduct = productRepository.save(product)
            logProductStockChange(savedProduct, previousStockQuantity, savedProduct.stockQuantity, "Product stock updated")
            savedProduct
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

    @Transactional
    fun togglePublished(id: String): ProductDTO? {
        val product = getProductById(id) ?: return null
        product.published = !product.published
        return productRepository.save(product).toDTO()
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
        
        val sortedImages = this.images.sortedBy { it.sortIndex }
        
        return ProductDTO(
            id = (this.id ?: ""),
            name = this.name,
            description = this.description,
            price = effectivePrice,
            originalPrice = originalPrice,
            image = sortedImages.firstOrNull()?.url ?: "",
            images = sortedImages.map { it.url }.toTypedArray(),
            category = category?.name ?: "",
            brand = this.wholesaler.businessName,
            stock = this.stockQuantity,
            rating = this.averageRating.toDouble(),
            reviews = this.ratingCount,
            supplier = this.wholesaler.businessName,
            published = this.published
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

    fun getPublishedProductsDTO(): List<ProductDTO> =
        getPublishedProducts().map { it.toDTO() }

    fun searchPublishedProductsDTO(query: String): List<ProductDTO> =
        searchPublishedProducts(query).map { it.toDTO() }

    fun getPublishedProductsDTOByCategory(categoryId: String): List<ProductDTO> =
        getPublishedProductsByCategory(categoryId).map { it.toDTO() }

    @Transactional
    fun createProductDTO(product: Product): ProductDTO = createProduct(product).toDTO()

    @Transactional
    fun updateProductDTO(id: String, product: Product): ProductDTO? = updateProduct(id, product)?.toDTO()

    @Transactional
    fun createProductFromRequest(request: CreateProductRequest): ProductDTO {
        val wholesaler = userRepository.findById(request.wholesalerId).orElseThrow {
            NoSuchElementException("Wholesaler not found: ${request.wholesalerId}")
        }
        val product = Product(
            wholesaler = wholesaler,
            name = request.name,
            description = request.description,
            categoryId = request.categoryId,
            price = BigDecimal.valueOf(request.price),
            stockQuantity = request.stock,
            published = true
        )
        return createProduct(product).toDTO()
    }

    @Transactional
    fun updateProductFromRequest(id: String, request: CreateProductRequest): ProductDTO? {
        val existing = getProductById(id) ?: return null
        val previousStockQuantity = existing.stockQuantity
        existing.name = request.name
        existing.description = request.description
        existing.categoryId = request.categoryId
        existing.price = BigDecimal.valueOf(request.price)
        existing.stockQuantity = request.stock
        val savedProduct = productRepository.save(existing)
        logProductStockChange(savedProduct, previousStockQuantity, savedProduct.stockQuantity, "Product stock updated")
        return savedProduct.toDTO()
    }

    fun getAllCategoriesDTO(): List<CategoryDTO> = getAllCategories().map { it.toDTO() }

    fun getCategoryDTOById(id: String): CategoryDTO? = getCategoryById(id)?.toDTO()

    fun getCategoriesDTOByParent(parentId: String): List<CategoryDTO> = 
        getCategoriesByParent(parentId).map { it.toDTO() }

    fun createCategoryDTO(category: Category): CategoryDTO = createCategory(category).toDTO()

    private fun logProductStockChange(product: Product, quantityBefore: Int, quantityAfter: Int, reason: String) {
        val productId = product.id ?: return
        inventoryAuditService.logStockChange(
            productId = productId,
            inventoryItemId = null,
            stockTargetType = InventoryStockTargetType.PRODUCT_CATALOG,
            sourceType = InventoryAuditSourceType.MANUAL,
            quantityBefore = quantityBefore,
            quantityAfter = quantityAfter,
            reason = reason
        )
    }
}
