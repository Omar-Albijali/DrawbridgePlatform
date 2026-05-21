package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false, unique = true)
    var token: String,

    @Column(nullable = false)
    var userId: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    @Column(nullable = false)
    var used: Boolean = false,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", insertable = false, updatable = false)
    var user: User? = null
)
