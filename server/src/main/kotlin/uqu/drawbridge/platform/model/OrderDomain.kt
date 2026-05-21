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
class OrderGroup(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var retailerId: String,

    @Column(nullable = false)
    var groupTotal: BigDecimal,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_group_id", nullable = false)
    var orders: MutableList<Order> = mutableListOf(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailerId", insertable = false, updatable = false)
    var retailer: User? = null
)


// ===================== ORDER (PER WHOLESALER) =====================
// Each order is for one wholesaler, with its own status & tracking

@Entity
@Table(name = "orders")
class Order(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "order_group_id", insertable = false, updatable = false)
    var orderGroupId: String? = null,

    @Column(nullable = false)
    var retailerId: String,

    @Column(nullable = false)
    var wholesalerId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: OrderStatus = OrderStatus.PENDING,

    @Column(nullable = false)
    var subtotal: BigDecimal,

    @Column(nullable = false)
    var autoOrder: Boolean = false,

    // Wholesaler-specific shipping & tracking
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    var shippingMethod: ShippingMethod? = null,

    @Column(nullable = true)
    var trackingNumber: String? = null,

    @Column(nullable = true)
    var trackingUrl: String? = null,

    @Column(nullable = true)
    var estimatedDelivery: LocalDateTime? = null,

    @Column(nullable = true)
    var shippedAt: LocalDateTime? = null,

    @Column(nullable = true)
    var deliveredAt: LocalDateTime? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    var orderItems: MutableList<OrderItem> = mutableListOf(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_group_id", insertable = false, updatable = false)
    var orderGroup: OrderGroup? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailerId", insertable = false, updatable = false)
    var retailer: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesalerId", insertable = false, updatable = false)
    var wholesaler: User? = null
)


// ===================== ORDER ITEM =====================

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "order_id", insertable = false, updatable = false, nullable = false)
    var orderId: String? = null,

    @Column(nullable = false)
    var productId: String,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var unitPrice: BigDecimal,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    var order: Order? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    var product: Product? = null
)
