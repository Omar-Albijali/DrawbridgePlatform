package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
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

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class AddToWishlistRequest(
    val productId: String
)