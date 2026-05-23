package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.ShippingMethod
import uqu.drawbridge.platform.PaymentStatus
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

// ===================== ORDER GROUP =====================
// Represents a single checkout action by the retailer (can contain orders from multiple wholesalers)
// Note: PaymentStatus enum is defined in PaymentDomain.kt

@Entity
@Table(name = "order_groups")
class OrderGroup(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var groupTotal: BigDecimal,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "orderGroup")
    var orders: MutableList<Order> = mutableListOf(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var retailer: User? = null
) {
    val retailerId: String? get() = retailer?.id
}


// ===================== ORDER (PER WHOLESALER) =====================
// Each order is for one wholesaler, with its own status & tracking

@Entity
@Table(name = "orders")
class Order(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

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

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "order")
    var orderItems: MutableList<OrderItem> = mutableListOf(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_group_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var orderGroup: OrderGroup? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var retailer: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesaler_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var wholesaler: User? = null
) {
    val orderGroupId: String? get() = orderGroup?.id
    val retailerId: String? get() = retailer?.id
    val wholesalerId: String? get() = wholesaler?.id
}


// ===================== ORDER ITEM =====================

@Entity
@Table(name = "order_items")
class OrderItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var unitPrice: BigDecimal,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var order: Order,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var product: Product? = null
) {
    val orderId: String? get() = order.id
    val productId: String? get() = product?.id
}
