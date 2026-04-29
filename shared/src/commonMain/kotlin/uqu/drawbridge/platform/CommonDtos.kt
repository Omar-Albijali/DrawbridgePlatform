@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport

@JsExport
data class ErrorResponse(
    val message: String,
    val status: Int
)
