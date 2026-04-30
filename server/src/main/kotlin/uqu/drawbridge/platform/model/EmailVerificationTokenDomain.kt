package uqu.drawbridge.platform.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_verification_tokens")
class EmailVerificationToken(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false, unique = true)
    var token: String,

    @Column(nullable = false)
    var userId: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    @Column(nullable = false)
    var used: Boolean = false
)

