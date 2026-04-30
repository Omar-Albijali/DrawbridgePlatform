@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@JsExport
@Serializable
enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}

@JsExport
@Serializable
enum class PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    DIGITAL_WALLET,
    CASH_ON_DELIVERY
}

@JsExport
@Serializable
data class PaymentDTO(
    val id: String, // Long -> String for JS safety
    val orderId: String, // Long -> String
    val ownerId: String, // Long -> String
    val paymentMethodId: String, // Long -> String
    val amount: Double, // BigDecimal -> Double
    val status: PaymentStatus,
    val transactionRef: String,
    val completedAt: String // LocalDateTime -> String (ISO 8601)
)

@JsExport
@Serializable
data class InvoiceDTO(
    val id: String, // Long -> String for JS safety
    val orderId: String, // Long -> String
    val invoiceNumber: String,
    val issueDate: String, // LocalDateTime -> String (ISO 8601)
    val dueDate: String, // LocalDateTime -> String (ISO 8601)
    val totalAmount: Double, // BigDecimal -> Double
    val currency: String
)

@JsExport
@Serializable
data class PaymentMethodDTO(
    val id: String, // Long -> String for JS safety
    val ownerId: String, // Long -> String
    val type: PaymentMethodType,
    val maskedDetails: String,
    val isDefault: Boolean
)

@JsExport
@Serializable
data class CreatePaymentRequest(
    val orderId: String,
    val ownerId: String,
    val paymentMethodId: String,
    val amount: Double,
    val transactionRef: String
)

@JsExport
@Serializable
data class CreatePaymentMethodRequest(
    val ownerId: String,
    val type: String,
    val maskedDetails: String,
    val isDefault: Boolean
)

@JsExport
@Serializable
data class CreateInvoiceRequest(
    val orderId: String,
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val totalAmount: Double,
    val currency: String
)
