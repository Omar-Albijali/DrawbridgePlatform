@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform

import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class MostOrderedProductDTO(
    val productId: String,
    val productName: String,
    val productImageUrl: String?,
    val orderCount: Int,
    val price: Double
)