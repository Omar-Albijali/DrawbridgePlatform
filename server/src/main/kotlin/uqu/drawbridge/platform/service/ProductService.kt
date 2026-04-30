package uqu.drawbridge.platform.service

import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.CategoryRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.dto.PaginatedResponse
import uqu.drawbridge.platform.validation.RequestValidation
import java.math.BigDecimal

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
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

    fun getPublishedBrands(): List<String> =
        productRepository.findDistinctPublishedBrands()

    fun getPublishedProductsPageDTO(
        page: Int = 0,
        size: Int = 12,
        search: String? = null,
        categories: List<String>? = null,
        brands: List<String>? = null,
        minPrice: Double? = null,
        maxPrice: Double? = null,
        sort: String? = null
    ): PaginatedResponse<ProductDTO> {
        val normalizedPage = page.coerceAtLeast(0)
        val normalizedSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(normalizedPage, normalizedSize, getMarketplaceSort(sort))
        val normalizedCategories = normalizeValues(categories)
        val normalizedBrands = normalizeValues(brands)
        val normalizedSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val minBound = minPrice?.coerceAtLeast(0.0)
        val maxBound = maxPrice?.coerceAtLeast(0.0)
        val effectiveMinPrice = when {
            minBound != null && maxBound != null -> minOf(minBound, maxBound)
            else -> minBound
        }
        val effectiveMaxPrice = when {
            minBound != null && maxBound != null -> maxOf(minBound, maxBound)
            else -> maxBound
        }

        var specification = Specification.where(publishedOnly())

        if (normalizedSearch != null) {
            specification = specification.and(matchesSearch(normalizedSearch))
        }
        if (normalizedCategories.isNotEmpty()) {
            specification = specification.and(matchesCategories(normalizedCategories))
        }
        if (normalizedBrands.isNotEmpty()) {
            specification = specification.and(matchesBrands(normalizedBrands))
        }
        if (effectiveMinPrice != null) {
            specification = specification.and(minimumPrice(BigDecimal.valueOf(effectiveMinPrice)))
        }
        if (effectiveMaxPrice != null) {
            specification = specification.and(maximumPrice(BigDecimal.valueOf(effectiveMaxPrice)))
        }

        val productPage = productRepository.findAll(specification, pageable)
        val content = mapProductsToDTOs(productPage.content)

        return PaginatedResponse(
            content = content,
            currentPage = productPage.number,
            pageSize = productPage.size,
            totalPages = productPage.totalPages,
            totalElements = productPage.totalElements,
            first = productPage.isFirst,
            last = productPage.isLast
        )
    }

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
        return toDTO(
            product = this,
            categoryName = category?.name
        )
    }

    fun Category.toDTO() = CategoryDTO(
        id = (this.id ?: ""),
        name = this.name,
        parentCategoryId = this.parentCategoryId
    )

    private fun toDTO(
        product: Product,
        categoryName: String?
    ): ProductDTO {
        val sortedImages = product.images.sortedBy { it.sortIndex }

        return ProductDTO(
            id = (product.id ?: ""),
            name = product.name,
            description = product.description,
            price = product.price.toDouble(),
            image = sortedImages.firstOrNull()?.url ?: "",
            images = sortedImages.map { it.url }.toTypedArray(),
            category = categoryName ?: "",
            brand = product.wholesaler.businessName,
            stock = product.stockQuantity,
            rating = product.averageRating.toDouble(),
            reviews = product.ratingCount,
            supplier = product.wholesaler.businessName,
            published = product.published,
            gtin = product.gtin
        )
    }

    private fun mapProductsToDTOs(products: List<Product>): List<ProductDTO> {
        if (products.isEmpty()) {
            return emptyList()
        }

        val categoriesById = categoryRepository.findAllById(products.map { it.categoryId }.distinct())
            .associateBy { it.id ?: "" }

        return products.map { product ->
            toDTO(
                product = product,
                categoryName = categoriesById[product.categoryId]?.name
            )
        }
    }

    private fun normalizeValues(values: List<String>?): List<String> =
        values.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

    private fun publishedOnly(): Specification<Product> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.isTrue(root.get("published"))
        }

    private fun matchesSearch(search: String): Specification<Product> =
        Specification { root, _, criteriaBuilder ->
            val searchPattern = "%${search.lowercase()}%"
            criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern)
            )
        }

    private fun matchesCategories(categories: List<String>): Specification<Product> =
        Specification { root, _, _ ->
            root.get<String>("categoryId").`in`(categories)
        }

    private fun matchesBrands(brands: List<String>): Specification<Product> =
        Specification { root, _, criteriaBuilder ->
            val normalizedBrands = brands.map { it.lowercase() }
            val wholesalerJoin = root.join<Product, User>("wholesaler", JoinType.INNER)
            criteriaBuilder.lower(wholesalerJoin.get("businessName")).`in`(normalizedBrands)
        }

    private fun minimumPrice(minPrice: BigDecimal): Specification<Product> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice)
        }

    private fun maximumPrice(maxPrice: BigDecimal): Specification<Product> =
        Specification { root, _, criteriaBuilder ->
            criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice)
        }

    private fun getMarketplaceSort(sort: String?): Sort =
        when (sort?.trim()?.lowercase()) {
            "price-low" -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("name"))
            "price-high" -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"))
            "rating" -> Sort.by(Sort.Order.desc("averageRating"), Sort.Order.desc("ratingCount"), Sort.Order.asc("name"))
            "newest" -> Sort.by(Sort.Order.desc("id"))
            else -> Sort.by(Sort.Order.desc("averageRating"), Sort.Order.desc("ratingCount"), Sort.Order.asc("name"))
        }

    // ==================== DTO-RETURNING METHODS ====================

    fun getAllProductsDTO(): List<ProductDTO> = mapProductsToDTOs(getAllProducts())

    fun getProductDTOById(id: String): ProductDTO? = getProductById(id)?.toDTO()

    fun getProductsDTOByWholesaler(wholesalerId: String): List<ProductDTO> = 
        mapProductsToDTOs(getProductsByWholesaler(wholesalerId))

    fun getProductsDTOByCategory(categoryId: String): List<ProductDTO> = 
        mapProductsToDTOs(getProductsByCategory(categoryId))

    fun searchProductsDTO(query: String): List<ProductDTO> = 
        mapProductsToDTOs(searchProducts(query))

    fun getPublishedProductsDTO(): List<ProductDTO> =
        mapProductsToDTOs(getPublishedProducts())

    fun searchPublishedProductsDTO(query: String): List<ProductDTO> =
        mapProductsToDTOs(searchPublishedProducts(query))

    fun getPublishedProductsDTOByCategory(categoryId: String): List<ProductDTO> =
        mapProductsToDTOs(getPublishedProductsByCategory(categoryId))

    @Transactional
    fun createProductDTO(product: Product): ProductDTO = createProduct(product).toDTO()

    @Transactional
    fun updateProductDTO(id: String, product: Product): ProductDTO? = updateProduct(id, product)?.toDTO()

    @Transactional
    fun createProductFromRequest(request: CreateProductRequest): ProductDTO {
        RequestValidation.requireNotBlank(request.wholesalerId, "wholesalerId")
        RequestValidation.requireNotBlank(request.name, "name")
        RequestValidation.requireNotBlank(request.description, "description")
        RequestValidation.requireNotBlank(request.categoryId, "categoryId")
        RequestValidation.requirePositive(request.stock, "stock")
        val parsedPrice = RequestValidation.parsePositiveBigDecimal(request.price.toString(), "price")
        val wholesaler = userRepository.findById(request.wholesalerId).orElseThrow {
            NoSuchElementException("Wholesaler not found: ${request.wholesalerId}")
        }
        val product = Product(
            wholesaler = wholesaler,
            name = request.name,
            description = request.description,
            categoryId = request.categoryId,
            price = parsedPrice,
            stockQuantity = request.stock,
            gtin = request.gtin,
            published = true
        )
        return createProduct(product).toDTO()
    }

    @Transactional
    fun updateProductFromRequest(id: String, request: CreateProductRequest): ProductDTO? {
        RequestValidation.requireNotBlank(id, "id")
        RequestValidation.requireNotBlank(request.name, "name")
        RequestValidation.requireNotBlank(request.description, "description")
        RequestValidation.requireNotBlank(request.categoryId, "categoryId")
        RequestValidation.requirePositive(request.stock, "stock")
        val parsedPrice = RequestValidation.parsePositiveBigDecimal(request.price.toString(), "price")
        val existing = getProductById(id) ?: return null
        val previousStockQuantity = existing.stockQuantity
        existing.name = request.name
        existing.description = request.description
        existing.categoryId = request.categoryId
        existing.price = parsedPrice
        existing.stockQuantity = request.stock
        existing.gtin = request.gtin
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
