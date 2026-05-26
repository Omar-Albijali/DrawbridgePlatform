package uqu.drawbridge.platform.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.model.InventoryAuditLog

interface InventoryAuditLogRepository : JpaRepository<InventoryAuditLog, String>, JpaSpecificationExecutor<InventoryAuditLog> {
    fun findByProduct_IdOrderByCreatedAtDesc(productId: String, pageable: Pageable): Page<InventoryAuditLog>
    fun findByInventoryItem_IdOrderByCreatedAtDesc(inventoryItemId: String, pageable: Pageable): Page<InventoryAuditLog>
    fun findFirstBySourceTypeAndSourceIdOrderByCreatedAtDesc(
        sourceType: InventoryAuditSourceType,
        sourceId: String
    ): InventoryAuditLog?
}
