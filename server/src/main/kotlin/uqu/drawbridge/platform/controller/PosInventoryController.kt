package uqu.drawbridge.platform.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uqu.drawbridge.platform.dto.PosInventoryChangeRequest
import uqu.drawbridge.platform.dto.PosInventoryChangeResponse
import uqu.drawbridge.platform.dto.PosOutboundInventoryEventDTO
import uqu.drawbridge.platform.security.PosApiKeyAuthenticationFilter
import uqu.drawbridge.platform.service.PosInventorySyncService
import uqu.drawbridge.platform.service.PosOutboundInventoryEventService

@RestController
@RequestMapping("/api/pos")
class PosInventoryController(
    private val posInventorySyncService: PosInventorySyncService,
    private val posOutboundInventoryEventService: PosOutboundInventoryEventService
) {

    @PostMapping("/inventory/changes")
    fun ingestInventoryChange(
        request: HttpServletRequest,
        @RequestBody body: PosInventoryChangeRequest
    ): ResponseEntity<PosInventoryChangeResponse> {
        val response = posInventorySyncService.applyIncomingInventoryChange(retailerId(request), body)
        val status = if (response.alreadyProcessed) HttpStatus.OK else HttpStatus.ACCEPTED
        return ResponseEntity.status(status).body(response)
    }

    @PostMapping("/webhooks/inventory.changed")
    fun ingestInventoryWebhook(
        request: HttpServletRequest,
        @RequestBody body: PosInventoryChangeRequest
    ): ResponseEntity<PosInventoryChangeResponse> {
        return ingestInventoryChange(request, body)
    }

    @GetMapping("/inventory/events/{eventId}")
    fun getEvent(
        request: HttpServletRequest,
        @PathVariable eventId: String
    ): ResponseEntity<PosOutboundInventoryEventDTO> {
        val event = posOutboundInventoryEventService.getRetailerEvent(retailerId(request), eventId)
        return if (event != null) ResponseEntity.ok(event) else ResponseEntity.notFound().build()
    }

    @GetMapping("/inventory/events")
    fun getEvents(
        request: HttpServletRequest,
        @RequestParam(required = false) since: String?,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<PosOutboundInventoryEventDTO>> {
        return ResponseEntity.ok(
            posOutboundInventoryEventService.getRetailerEvents(
                retailerId = retailerId(request),
                since = since,
                limit = limit
            )
        )
    }

    private fun retailerId(request: HttpServletRequest): String {
        return request.getAttribute(PosApiKeyAuthenticationFilter.RETAILER_ID_ATTR) as? String
            ?: throw IllegalStateException("POS auth context missing retailer scope")
    }
}
