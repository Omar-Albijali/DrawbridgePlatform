@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class CartItemDTO(
    val id: String?, // Long -> String for JS safety
    val productId: String, // Long -> String
    val wholesalerId: String, // Long -> String
    val quantity: Int,
    val addedAt: String? // LocalDateTime -> String
)

@JsExport
@Serializable
data class ShoppingCartDTO(
    val id: String, // Long -> String for JS safety
    val retailerId: String, // Long -> String
    val updatedAt: String, // LocalDateTime -> String
    val items: Array<CartItemDTO>?
)

@JsExport
@Serializable
data class AddToCartRequest(
    val productId: String, // Long -> String
    val quantity: Int
)
