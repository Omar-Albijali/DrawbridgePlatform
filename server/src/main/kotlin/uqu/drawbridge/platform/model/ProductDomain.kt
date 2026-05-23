package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore

import jakarta.persistence.*

import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "categories")
class Category(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var name: String,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentCategoryId", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var parentCategory: Category? = null
) {
    val parentCategoryId: String?
        get() = parentCategory?.id
}

@Entity
@Table(name = "products")
@Inheritance(strategy = InheritanceType.JOINED)
class Product(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wholesalerId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var wholesaler: User,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoryId", nullable = false)
    // No @OnDelete for Category (RESTRICT by default)
    var category: Category,

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

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "product")
    var images: MutableList<ProductImage> = mutableListOf(),

    // Rating summary fields - auto-calculated by service layer
    @Column(nullable = false)
    var averageRating: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false)
    var ratingCount: Int = 0
) {
    val categoryId: String
        get() = category.id ?: ""
}


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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var product: Product
) {
    val productId: String?
        get() = product.id
}


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
    var rating: Int,  // 1-5 stars

    @Column(nullable = true, columnDefinition = "TEXT")
    var review: String? = null,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var product: Product,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var user: User
) {
    val productId: String
        get() = product.id ?: ""

    val userId: String
        get() = user.id ?: ""
}
