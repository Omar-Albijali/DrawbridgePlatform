package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

// ===================== SHOPPING CART =====================
// One cart per retailer for collecting items before checkout

@Entity
@Table(name = "shopping_carts")
open class ShoppingCart(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false, unique = true)
    open var retailerId: String,

    @Column(nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "cart_id", nullable = false)
    open var items: MutableList<CartItem> = mutableListOf()
)


// ===================== CART ITEM =====================

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["cart_id", "productId"])]
)
open class CartItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(name = "cart_id", insertable = false, updatable = false, nullable = false)
    open var cartId: String? = null,

    @Column(nullable = false)
    open var productId: String,

    // Denormalized for easier grouping during checkout
    @Column(nullable = false)
    open var wholesalerId: String,

    @Column(nullable = false)
    open var quantity: Int,

    @Column(nullable = false)
    open var addedAt: LocalDateTime = LocalDateTime.now()
)
