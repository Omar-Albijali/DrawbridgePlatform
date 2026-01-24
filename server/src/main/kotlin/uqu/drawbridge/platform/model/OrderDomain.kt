package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.ShippingMethod
import uqu.drawbridge.platform.PaymentStatus

// ===================== ORDER GROUP =====================
// Represents a single checkout action by the retailer (can contain orders from multiple wholesalers)
// Note: PaymentStatus enum is defined in PaymentDomain.kt

@Entity
@Table(name = "order_groups")
open class OrderGroup(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var retailerId: String,

    @Column(nullable = false)
    open var groupTotal: BigDecimal,

    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_group_id", nullable = false)
    open var orders: MutableList<Order> = mutableListOf()
)


// ===================== ORDER (PER WHOLESALER) =====================
// Each order is for one wholesaler, with its own status & tracking

@Entity
@Table(name = "orders")
open class Order(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(name = "order_group_id", insertable = false, updatable = false)
    open var orderGroupId: String? = null,

    @Column(nullable = false)
    open var retailerId: String,

    @Column(nullable = false)
    open var wholesalerId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    open var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false)
    open var subtotal: BigDecimal,

    @Column(nullable = false)
    open var autoOrder: Boolean = false,

    // Wholesaler-specific shipping & tracking
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    open var shippingMethod: ShippingMethod? = null,

    @Column(nullable = true)
    open var trackingNumber: String? = null,

    @Column(nullable = true)
    open var trackingUrl: String? = null,

    @Column(nullable = true)
    open var estimatedDelivery: LocalDateTime? = null,

    @Column(nullable = true)
    open var shippedAt: LocalDateTime? = null,

    @Column(nullable = true)
    open var deliveredAt: LocalDateTime? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    open var orderItems: MutableList<OrderItem> = mutableListOf()
)


// ===================== ORDER ITEM =====================

@Entity
@Table(name = "order_items")
open class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(name = "order_id", insertable = false, updatable = false, nullable = false)
    open var orderId: String? = null,

    @Column(nullable = false)
    open var productId: String,

    @Column(nullable = false)
    open var quantity: Int,

    @Column(nullable = false)
    open var unitPrice: BigDecimal
)