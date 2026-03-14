package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class OrderStatus {
    PENDING,
    CONFIRMED,


    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,

    RETURNED
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class ShippingMethod {
    STANDARD,
    EXPRESS,
    OVERNIGHT,
    LOCAL_PICKUP,
    CUSTOM
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class TrackingInfoDTO(
    val trackingNumber: String,
    val trackingUrl: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class OrderItemDTO(
    val id: String, // Long -> String for JS safety
    val productId: String, // Long -> String
    val productName: String,
    val productCategory: String,
    val productImageUrl: String?,
    val quantity: Int,
    val unitPrice: Double // BigDecimal -> Double
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class OrderDTO(
    val id: String, // Long -> String
    val orderGroupId: String, // Long -> String
    val wholesalerId: String, // Long -> String
    val retailerId: String, // Long -> String
    val retailerName: String,
    val status: OrderStatus,
    val subtotal: Double, // BigDecimal -> Double
    val autoOrder: Boolean,
    val shippingMethod: ShippingMethod?,
    val trackingNumber: String?,
    val trackingUrl: String?,
    val estimatedDelivery: String?, // LocalDateTime -> String (ISO 8601)
    val shippedAt: String?,
    val deliveredAt: String?,
    val placedAt: String,
    val items: Array<OrderItemDTO> // List -> Array for JS interop
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class OrderGroupDTO(
    val id: String, // Long -> String
    val retailerId: String, // Long -> String
    val groupTotal: Double, // BigDecimal -> Double
    val paymentStatus: PaymentStatus,
    val createdAt: String, // LocalDateTime -> String
    val orders: Array<OrderDTO> // List -> Array
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class UpdateOrderTrackingRequest(
    val shippingMethod: ShippingMethod? = null,
    val trackingNumber: String? = null,
    val trackingUrl: String? = null,
    val estimatedDelivery: String? = null
)
