package uqu.drawbridge.platform.service

import java.security.MessageDigest
import org.springframework.stereotype.Service
import uqu.drawbridge.platform.model.PosIntegration
import uqu.drawbridge.platform.model.PosIntegrationStatus
import uqu.drawbridge.platform.repository.PosIntegrationRepository

@Service
class PosApiKeyService(
    private val posIntegrationRepository: PosIntegrationRepository
) {
    fun authenticate(rawApiKey: String): PosIntegration? {
        val normalized = rawApiKey.trim()
        if (normalized.isEmpty()) {
            return null
        }
        val hash = sha256Hex(normalized)
        return posIntegrationRepository.findByApiKeyHashAndStatus(hash, PosIntegrationStatus.ACTIVE)
    }

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
