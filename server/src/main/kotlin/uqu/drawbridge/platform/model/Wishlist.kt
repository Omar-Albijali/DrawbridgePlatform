package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "wishlists",
    uniqueConstraints = [UniqueConstraint(columnNames = ["userId", "productId"])]
)
open class Wishlist(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var userId: String,

    @Column(nullable = false)
    open var productId: String,

    @Column(nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)