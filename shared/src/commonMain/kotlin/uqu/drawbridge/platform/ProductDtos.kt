@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class ProductDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val description: String,
    val price: Double, // BigDecimal -> Double
    val image: String,
    val images: Array<String>,
    val category: String,
    val brand: String,
    val stock: Int,
    val minimumOrderQuantity: Int = 1,
    val gtin: String,
    val rating: Double,
    val reviews: Int,
    val supplier: String,
    val published: Boolean
)

@JsExport
@Serializable
data class CategoryDTO(
    val id: String, // Long -> String for JS safety
    val name: String,
    val parentCategoryId: String?
)

@JsExport
@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: Double,
    val image: String,
    val category: String,
    val categoryId: String, // Long -> String
    val wholesalerId: String, // Long -> String
    val brand: String,
    val stock: Int,
    val minimumOrderQuantity: Int = 1,
    val gtin: String
)

@JsExport
@Serializable
data class ImageUploadResponse(
    val id: String?, // Long -> String
    val url: String,
    val message: String
)

@JsExport
@Serializable
data class ProductImageResponse(
    val id: String?, // Long -> String
    val url: String,
    val altText: String,
    val sortIndex: Int,
    val productId: String // Long -> String
)

@Serializable
data class PaginatedProductResponse(
    val content: List<ProductDTO>,
    val currentPage: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalElements: Long,
    @SerialName("isFirst")
    val isFirst: Boolean,
    @SerialName("isLast")
    val isLast: Boolean,
)

data class MarketplaceProductQuery(
    val page: Int = 0,
    val size: Int = 12,
    val search: String? = null,
    val categoryIds: List<String> = emptyList(),
    val brands: List<String> = emptyList(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sort: String = "featured",
)
