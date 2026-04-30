@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
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
@Serializable
data class AddToWishlistRequest(
    val productId: String
)
