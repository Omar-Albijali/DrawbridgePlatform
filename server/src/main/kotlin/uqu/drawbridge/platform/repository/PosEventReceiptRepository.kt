package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.PosEventReceipt

interface PosEventReceiptRepository : JpaRepository<PosEventReceipt, String> {
    fun existsByRetailerIdAndEventIdAndEventType(retailerId: String, eventId: String, eventType: String): Boolean
    fun findTop200ByRetailerIdOrderByProcessedAtDesc(retailerId: String): List<PosEventReceipt>
}
