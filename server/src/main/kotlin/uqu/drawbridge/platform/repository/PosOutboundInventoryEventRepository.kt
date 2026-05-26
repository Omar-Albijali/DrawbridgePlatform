package uqu.drawbridge.platform.repository

import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.dto.PosOutboundEventStatus
import uqu.drawbridge.platform.model.PosOutboundInventoryEvent

interface PosOutboundInventoryEventRepository : JpaRepository<PosOutboundInventoryEvent, String> {
    fun findByEventIdAndRetailer_Id(eventId: String, retailerId: String): PosOutboundInventoryEvent?
    fun findTop100ByStatusAndNextRetryAtBeforeOrderByCreatedAtAsc(
        status: PosOutboundEventStatus,
        nextRetryAt: LocalDateTime
    ): List<PosOutboundInventoryEvent>

    fun findTop200ByRetailer_IdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(
        retailerId: String,
        eventTime: LocalDateTime
    ): List<PosOutboundInventoryEvent>

    fun findTop200ByRetailer_IdOrderByEventTimeDesc(retailerId: String): List<PosOutboundInventoryEvent>
}
