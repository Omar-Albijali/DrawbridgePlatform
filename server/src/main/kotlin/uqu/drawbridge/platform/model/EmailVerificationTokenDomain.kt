package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification_tokens")
open class EmailVerificationToken(
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

