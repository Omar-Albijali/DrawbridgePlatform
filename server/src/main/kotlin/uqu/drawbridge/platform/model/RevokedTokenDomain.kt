package uqu.drawbridge.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "revoked_tokens")
class RevokedToken(
    @Id
    @Column(nullable = false, length = 64)
    var tokenId: String,

    @Column(nullable = false)
    var expiresAt: LocalDateTime,

    @Column(nullable = false)
    var revokedAt: LocalDateTime = LocalDateTime.now()
)

