package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.PosIntegration
import uqu.drawbridge.platform.model.PosIntegrationStatus

interface PosIntegrationRepository : JpaRepository<PosIntegration, String> {
    fun findByRetailerId(retailerId: String): PosIntegration?
    fun findByApiKeyHashAndStatus(apiKeyHash: String, status: PosIntegrationStatus): PosIntegration?
}
