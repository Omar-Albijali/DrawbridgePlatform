package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.EmailVerificationToken

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, String> {
    fun findByToken(token: String): EmailVerificationToken?
    fun deleteAllByUserId(userId: String)
}

