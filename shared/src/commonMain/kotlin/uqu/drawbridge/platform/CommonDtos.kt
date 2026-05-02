@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class ErrorResponse(
    val message: String,
    val status: Int
)
