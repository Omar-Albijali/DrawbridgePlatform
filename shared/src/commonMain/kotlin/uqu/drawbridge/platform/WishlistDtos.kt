@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport

@JsExport
data class WishlistDTO(
    val id: String,
    val userId: String,
    val productId: String,
    val productName: String,
    val productPrice: Double,
    val productImage: String,
    val createdAt: String
)

@JsExport
data class AddToWishlistRequest(
    val productId: String
)
