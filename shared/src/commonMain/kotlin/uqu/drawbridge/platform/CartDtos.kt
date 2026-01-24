package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
data class CartItemDTO(
    val id: String?, // Long -> String for JS safety
    val productId: String, // Long -> String
    val wholesalerId: String, // Long -> String
    val quantity: Int,
    val addedAt: String? // LocalDateTime -> String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class ShoppingCartDTO(
    val id: String, // Long -> String for JS safety
    val retailerId: String, // Long -> String
    val updatedAt: String, // LocalDateTime -> String
    val items: Array<CartItemDTO>?
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class AddToCartRequest(
    val productId: String, // Long -> String
    val quantity: Int
)
