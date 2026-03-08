package uqu.drawbridge.platform.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.model.RevokedToken
import uqu.drawbridge.platform.repository.RevokedTokenRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val expiration: Long,
    private val revokedTokenRepository: RevokedTokenRepository
) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateToken(user: User): String {
        val now = Date()
        val expiryDate = Date(now.time + expiration)
        val tokenId = UUID.randomUUID().toString()

        return Jwts.builder()
            .subject(user.email)
            .id(tokenId)
            .claim("userId", user.id)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(secretKey)
            .compact()
    }

    fun extractUsername(token: String): String? {
        return try {
            extractAllClaims(token).subject
        } catch (e: Exception) {
            null
        }
    }

    fun extractTokenId(token: String): String? {
        return try {
            extractAllClaims(token).id
        } catch (e: Exception) {
            null
        }
    }

    fun extractExpiration(token: String): Date? {
        return try {
            extractAllClaims(token).expiration
        } catch (e: Exception) {
            null
        }
    }

    fun revokeToken(token: String) {
        val tokenId = extractTokenId(token) ?: throw IllegalArgumentException("Token ID missing")
        val expiryDate = extractExpiration(token) ?: throw IllegalArgumentException("Token expiration missing")

        if (revokedTokenRepository.existsById(tokenId)) return

        val expiresAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(expiryDate.time), ZoneOffset.UTC)
        revokedTokenRepository.save(RevokedToken(tokenId = tokenId, expiresAt = expiresAt))
    }

    fun validateToken(token: String, userDetails: UserDetails): Boolean {
        val username = extractUsername(token)
        val tokenId = extractTokenId(token)
        if (tokenId == null) return false
        if (revokedTokenRepository.existsById(tokenId)) return false
        return username == userDetails.username && !isTokenExpired(token)
    }

    private fun isTokenExpired(token: String): Boolean {
        return extractAllClaims(token).expiration.before(Date())
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
