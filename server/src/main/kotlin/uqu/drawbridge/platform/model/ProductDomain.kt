package uqu.drawbridge.platform.model


import jakarta.persistence.*

import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
open class Category(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var name: String,

    @Column(nullable = true)
    open var parentCategoryId: String? = null
)

@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.JOINED)
open class Product(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wholesalerId", nullable = false)
    open var wholesaler: User,

    @Column(nullable = false)
    open var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    open var description: String,

    @Column(nullable = false)
    open var categoryId: String,

    @Column(nullable = false)
    open var price: BigDecimal,

    @Column(nullable = false)
    open var stockQuantity: Int,

    @Column(nullable = false)
    open var published: Boolean,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "productId", nullable = false)
    open var images: MutableList<ProductImage> = mutableListOf(),

    // Rating summary fields - auto-calculated by service layer
    @Column(nullable = false)
    open var averageRating: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    open var ratingCount: Int = 0
)


@Entity
@Table(name = "product_images")
open class ProductImage(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var url: String,

    @Column(nullable = false)
    open var altText: String,

    @Column(nullable = false)
    open var sortIndex: Int = 0,

    @Column(name = "productId", insertable = false, updatable = false, nullable = false)
    open var productId: String? = null
)


// ===================== PRODUCT RATING =====================

@Entity
@Table(
    name = "product_ratings",
    uniqueConstraints = [UniqueConstraint(columnNames = ["productId", "userId"])]
)
open class ProductRating(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var productId: String,

    @Column(nullable = false)
    open var userId: String,

    @Column(nullable = false)
    open var rating: Int,  // 1-5 stars

    @Column(nullable = true, columnDefinition = "TEXT")
    open var review: String? = null,

    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now()
)


// ===================== PRODUCT DISCOUNT =====================

@Entity
@Table(name = "product_discounts")
open class ProductDiscount(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var productId: String,

    @Column(nullable = false)
    open var name: String,

    @Column(nullable = true, columnDefinition = "TEXT")
    open var description: String? = null,

    // Percentage discount value (e.g., 20 for 20% off)
    @Column(nullable = false)
    open var discountPercentage: BigDecimal,

    @Column(nullable = false)
    open var startDate: LocalDateTime,

    @Column(nullable = false)
    open var endDate: LocalDateTime,

    @Column(nullable = false)
    open var isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)