package uqu.drawbridge.platform.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uqu.drawbridge.platform.PosIntegrationApiKeyRotateResponse
import uqu.drawbridge.platform.PosIntegrationConfigDTO
import uqu.drawbridge.platform.PosIntegrationConfigUpdateRequest
import uqu.drawbridge.platform.PosIntegrationEventLogDTO
import uqu.drawbridge.platform.service.PosIntegrationManagementService

@RestController
@RequestMapping("/api/retailer/pos-integration")
class PosIntegrationController(
    private val posIntegrationManagementService: PosIntegrationManagementService
) {

    @GetMapping
    fun getConfig(authentication: Authentication): ResponseEntity<PosIntegrationConfigDTO> {
        return ResponseEntity.ok(posIntegrationManagementService.getCurrentRetailerConfig(authentication.name))
    }

    @PutMapping
    fun updateConfig(
        authentication: Authentication,
        @RequestBody request: PosIntegrationConfigUpdateRequest
    ): ResponseEntity<PosIntegrationConfigDTO> {
        return ResponseEntity.ok(
            posIntegrationManagementService.updateCurrentRetailerConfig(authentication.name, request)
        )
    }

    @PostMapping("/api-key/rotate")
    fun rotateApiKey(authentication: Authentication): ResponseEntity<PosIntegrationApiKeyRotateResponse> {
        return ResponseEntity.ok(posIntegrationManagementService.rotateCurrentRetailerApiKey(authentication.name))
    }

    @GetMapping("/events")
    fun getEventLogs(
        authentication: Authentication,
        @RequestParam(required = false) since: String?,
        @RequestParam(defaultValue = "100") limit: Int
    ): ResponseEntity<List<PosIntegrationEventLogDTO>> {
        return ResponseEntity.ok(
            posIntegrationManagementService.getCurrentRetailerEventLogs(authentication.name, since, limit)
        )
    }
}
