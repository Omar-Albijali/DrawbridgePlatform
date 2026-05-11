package uqu.drawbridge.platform.model


import jakarta.persistence.*

import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
class Category(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true)
    var parentCategoryId: String? = null
)

@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.JOINED)
class Product(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesalerId", nullable = false)
    var wholesaler: User,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false)
    var categoryId: String,

    @Column(nullable = false)
    var price: BigDecimal,

    @Column(nullable = false)
    var stockQuantity: Int,

    @Column(name = "minimum_order_quantity", nullable = false, columnDefinition = "integer default 1")
    var minimumOrderQuantity: Int = 1,

    @Column(nullable = false)
    open var gtin: String = "",

    @Column(nullable = false)
    var published: Boolean,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "productId", nullable = false)
    var images: MutableList<ProductImage> = mutableListOf(),

    // Rating summary fields - auto-calculated by service layer
    @Column(nullable = false)
    var averageRating: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var ratingCount: Int = 0
)


@Entity
@Table(name = "product_images")
class ProductImage(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var url: String,

    @Column(nullable = false)
    var altText: String,

    @Column(nullable = false)
    var sortIndex: Int = 0,

    @Column(name = "productId", insertable = false, updatable = false, nullable = false)
    var productId: String? = null
)


// ===================== PRODUCT RATING =====================

@Entity
@Table(
    name = "product_ratings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["productId", "userId"])]
)
class ProductRating(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var productId: String,

    @Column(nullable = false)
    var userId: String,

    @Column(nullable = false)
    var rating: Int,  // 1-5 stars

    @Column(nullable = true, columnDefinition = "TEXT")
    var review: String? = null,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
