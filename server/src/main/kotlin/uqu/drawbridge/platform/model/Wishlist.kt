package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "wishlists",
    uniqueConstraints = [UniqueConstraint(columnNames = ["userId", "productId"])]
)
class Wishlist(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var userId: String,

    @Column(nullable = false)
    var productId: String,

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
)