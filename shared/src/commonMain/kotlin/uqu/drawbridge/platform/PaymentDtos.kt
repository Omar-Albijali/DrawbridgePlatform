package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    CANCELLED
}

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class PaymentMethodType {
    CREDIT_CARD,
    DEBIT_CARD,
    BANK_TRANSFER,
    DIGITAL_WALLET,
    CASH_ON_DELIVERY
}

@OptIn(ExperimentalJsExport::class)
@JsExport
data class PaymentDTO(
    val id: String, // Long -> String for JS safety
    val orderId: String, // Long -> String
    val ownerId: String, // Long -> String
    val paymentMethodId: String, // Long -> String
    val amount: String, // BigDecimal -> String to preserve precision
    val status: PaymentStatus,
    val transactionRef: String,
    val completedAt: String // LocalDateTime -> String (ISO 8601)
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class InvoiceDTO(
    val id: String, // Long -> String for JS safety
    val orderId: String, // Long -> String
    val invoiceNumber: String,
    val issueDate: String, // LocalDateTime -> String (ISO 8601)
    val dueDate: String, // LocalDateTime -> String (ISO 8601)
    val totalAmount: String, // BigDecimal -> String to preserve precision
    val currency: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class PaymentMethodDTO(
    val id: String, // Long -> String for JS safety
    val ownerId: String, // Long -> String
    val type: PaymentMethodType,
    val maskedDetails: String,
    val isDefault: Boolean
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class CreatePaymentRequest(
    val orderId: String,
    val ownerId: String,
    val paymentMethodId: String,
    val amount: String,
    val transactionRef: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class CreatePaymentMethodRequest(
    val ownerId: String,
    val type: String,
    val maskedDetails: String,
    val isDefault: Boolean
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class CreateInvoiceRequest(
    val orderId: String,
    val invoiceNumber: String,
    val issueDate: String,
    val dueDate: String,
    val totalAmount: String,
    val currency: String
)
