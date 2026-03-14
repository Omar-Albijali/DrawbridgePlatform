package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ProductDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val description: String,
    val price: Double, // BigDecimal -> Double
    val originalPrice: Double?,
    val image: String,
    val images: Array<String>,
    val category: String,
    val brand: String,
    val stock: Int,
    val gtin: Int,
    val rating: Double,
    val reviews: Int,
    val supplier: String,
    val published: Boolean
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CategoryDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val parentCategoryId: String?
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Double,
    val originalPrice: Double?,
    val image: String,
    val category: String,
    val categoryId: String, // Long -> String
    val wholesalerId: String, // Long -> String
    val brand: String,
    val stock: Int,
    val gtin: Int
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ImageUploadResponse(
    val id: String?, // Long -> String
    val url: String,
    val message: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ProductImageResponse(
    val id: String?, // Long -> String
    val url: String,
    val altText: String,
    val sortIndex: Int,
    val productId: String // Long -> String
)
