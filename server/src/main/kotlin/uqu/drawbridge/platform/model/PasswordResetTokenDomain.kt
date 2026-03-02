package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens")
open class PasswordResetToken(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false, unique = true)
    open var token: String,

    @Column(nullable = false)
    open var userId: String,

    @Column(nullable = false)
    open var expiresAt: LocalDateTime,

    @Column(nullable = false)
    open var used: Boolean = false
)

