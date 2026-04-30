package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.dto.PaginatedResponse
import uqu.drawbridge.platform.model.Category
import uqu.drawbridge.platform.model.Product
import uqu.drawbridge.platform.service.ProductService

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    // ==================== PRODUCTS ====================

    @GetMapping
    fun getAllProducts(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "12") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false, name = "category") categories: List<String>?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(required = false, name = "brand") brands: List<String>?,
        @RequestParam(required = false) minPrice: Double?,
        @RequestParam(required = false) maxPrice: Double?
    ): ResponseEntity<PaginatedResponse<ProductDTO>> {
        return ResponseEntity.ok(
            productService.getPublishedProductsPageDTO(
                page = page,
                size = size,
                search = search,
                categories = categories,
                brands = brands,
                minPrice = minPrice,
                maxPrice = maxPrice,
                sort = sort
            )
        )
    }

    @GetMapping("/all")
    fun getAllPublishedProducts(): ResponseEntity<List<ProductDTO>> {
        return ResponseEntity.ok(productService.getPublishedProductsDTO())
    }

    @GetMapping("/{id}")
    fun getProductById(@PathVariable id: String): ResponseEntity<ProductDTO> {
        val product = productService.getProductDTOById(id)
        return if (product != null) {
            ResponseEntity.ok(product)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/wholesaler/{wholesalerId}")
    fun getProductsByWholesaler(@PathVariable wholesalerId: String): ResponseEntity<List<ProductDTO>> {
        return ResponseEntity.ok(productService.getProductsDTOByWholesaler(wholesalerId))
    }

    @GetMapping("/category/{categoryId}")
    fun getProductsByCategory(@PathVariable categoryId: String): ResponseEntity<List<ProductDTO>> {
        return ResponseEntity.ok(productService.getPublishedProductsDTOByCategory(categoryId))
    }

    @GetMapping("/search")
    fun searchProducts(@RequestParam q: String): ResponseEntity<List<ProductDTO>> {
        return ResponseEntity.ok(productService.searchPublishedProductsDTO(q))
    }

    @PostMapping
    fun createProduct(@RequestBody request: CreateProductRequest): ResponseEntity<ProductDTO> {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProductFromRequest(request))
    }

    @PutMapping("/{id}")
    fun updateProduct(@PathVariable id: String, @RequestBody request: CreateProductRequest): ResponseEntity<ProductDTO> {
        val updated = productService.updateProductFromRequest(id, request)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteProduct(@PathVariable id: String): ResponseEntity<Void> {
        return if (productService.deleteProduct(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/toggle-published")
    fun togglePublished(@PathVariable id: String): ResponseEntity<ProductDTO> {
        val updated = productService.togglePublished(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== CATEGORIES ====================

    @GetMapping("/categories")
    fun getAllCategories(): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(productService.getAllCategoriesDTO())
    }

    @GetMapping("/brands")
    fun getAllBrands(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(productService.getPublishedBrands())
    }

    @GetMapping("/categories/{id}")
    fun getCategoryById(@PathVariable id: String): ResponseEntity<CategoryDTO> {
        val category = productService.getCategoryDTOById(id)
        return if (category != null) {
            ResponseEntity.ok(category)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/categories/parent/{parentId}")
    fun getCategoriesByParent(@PathVariable parentId: String): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(productService.getCategoriesDTOByParent(parentId))
    }

    @PostMapping("/categories")
    fun createCategory(@RequestBody category: Category): ResponseEntity<CategoryDTO> {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createCategoryDTO(category))
    }

    @DeleteMapping("/categories/{id}")
    fun deleteCategory(@PathVariable id: String): ResponseEntity<Void> {
        return if (productService.deleteCategory(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
