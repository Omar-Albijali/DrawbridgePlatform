package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.PasswordResetToken

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, String> {
    fun findByToken(token: String): PasswordResetToken?
    fun deleteAllByUserId(userId: String)
}


