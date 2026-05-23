package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

// ===================== SHOPPING CART =====================
// One cart per retailer for collecting items before checkout

@Entity
@Table(name = "shopping_carts")
class ShoppingCart(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "cart")
    var items: MutableList<CartItem> = mutableListOf(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var retailer: User
) {
    val retailerId: String?
        get() = retailer.id
}


// ===================== CART ITEM =====================

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = [UniqueConstraint(columnNames = ["cart_id", "product_id"])]
)
class CartItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var addedAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var cart: ShoppingCart,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var product: Product,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wholesaler_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var wholesaler: User
) {
    val cartId: String?
        get() = cart.id

    val productId: String
        get() = product.id ?: ""

    val wholesalerId: String
        get() = wholesaler.id ?: ""
}
