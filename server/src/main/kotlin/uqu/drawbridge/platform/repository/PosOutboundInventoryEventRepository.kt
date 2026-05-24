package uqu.drawbridge.platform.repository

import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.dto.PosOutboundEventStatus
import uqu.drawbridge.platform.model.PosOutboundInventoryEvent

interface PosOutboundInventoryEventRepository : JpaRepository<PosOutboundInventoryEvent, String> {
    fun findByEventIdAndRetailerId(eventId: String, retailerId: String): PosOutboundInventoryEvent?
    fun findTop100ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
        status: PosOutboundEventStatus,
        nextRetryAt: LocalDateTime
    ): List<PosOutboundInventoryEvent>

    fun findTop200ByRetailerIdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(
        retailerId: String,
        eventTime: LocalDateTime
    ): List<PosOutboundInventoryEvent>

    fun findTop200ByRetailerIdOrderByEventTimeDesc(retailerId: String): List<PosOutboundInventoryEvent>
}
