package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.PosEventReceipt

interface PosEventReceiptRepository : JpaRepository<PosEventReceipt, String> {
    fun existsByRetailer_IdAndEventIdAndEventType(retailerId: String, eventId: String, eventType: String): Boolean
    fun findTop200ByRetailer_IdOrderByProcessedAtDesc(retailerId: String): List<PosEventReceipt>
}
