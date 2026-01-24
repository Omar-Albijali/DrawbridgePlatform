package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
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
    fun getAllProducts(): ResponseEntity<List<ProductDTO>> {
        return ResponseEntity.ok(productService.getAllProductsDTO())
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
        return ResponseEntity.ok(productService.getProductsDTOByCategory(categoryId))
    }

    @GetMapping("/search")
    fun searchProducts(@RequestParam q: String): ResponseEntity<List<ProductDTO>> {
        return ResponseEntity.ok(productService.searchProductsDTO(q))
    }

    @PostMapping
    fun createProduct(@RequestBody product: Product): ResponseEntity<ProductDTO> {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProductDTO(product))
    }

    @PutMapping("/{id}")
    fun updateProduct(@PathVariable id: String, @RequestBody product: Product): ResponseEntity<ProductDTO> {
        val updated = productService.updateProductDTO(id, product)
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

    // ==================== CATEGORIES ====================

    @GetMapping("/categories")
    fun getAllCategories(): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(productService.getAllCategoriesDTO())
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
