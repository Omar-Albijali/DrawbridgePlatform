@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform

import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
data class PosIntegrationConfigDTO(
    val retailerId: String,
    val integrationExists: Boolean,
    val status: String,
    val apiKeyPrefix: String?,
    val webhookEnabled: Boolean,
    val webhookUrl: String?,
    val webhookSecretConfigured: Boolean,
    val createdAt: String?,
    val rotatedAt: String?
)

@JsExport
@Serializable
data class PosIntegrationConfigUpdateRequest(
    val status: String,
    val webhookEnabled: Boolean,
    val webhookUrl: String?,
    val webhookSecret: String?
)

@JsExport
@Serializable
data class PosIntegrationApiKeyRotateResponse(
    val retailerId: String,
    val apiKey: String,
    val apiKeyPrefix: String,
    val rotatedAt: String
)

@JsExport
@Serializable
data class PosIntegrationEventLogDTO(
    val direction: String,
    val eventId: String,
    val eventType: String,
    val status: String,
    val eventTime: String,
    val sourceType: String?,
    val sourceId: String?,
    val gtin: String?,
    val productId: String?,
    val inventoryItemId: String?,
    val quantityBefore: Int?,
    val quantityAfter: Int?,
    val changeAmount: Int?,
    val reason: String?,
    val attemptCount: Int?,
    val lastError: String?
)
