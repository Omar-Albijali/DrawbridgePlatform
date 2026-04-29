@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport

@JsExport
data class ProductDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val description: String,
    val price: String, // BigDecimal -> String to preserve precision
    val originalPrice: String?,
    val image: String,
    val images: Array<String>,
    val category: String,
    val brand: String,
    val stock: Int,
    val rating: Double,
    val reviews: Int,
    val supplier: String,
    val published: Boolean
)

@JsExport
data class CategoryDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val parentCategoryId: String?
)

@JsExport
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: String,
    val originalPrice: String?,
    val image: String,
    val category: String,
    val categoryId: String, // Long -> String
    val wholesalerId: String, // Long -> String
    val brand: String,
    val stock: Int
)

@JsExport
data class ImageUploadResponse(
    val id: String?, // Long -> String
    val url: String,
    val message: String
)

@JsExport
data class ProductImageResponse(
    val id: String?, // Long -> String
    val url: String,
    val altText: String,
    val sortIndex: Int,
    val productId: String // Long -> String
)
