package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.dto.InventoryAuditLogPageResponse
import uqu.drawbridge.platform.model.InventoryAuditSourceType
import uqu.drawbridge.platform.model.InventoryStockTargetType
import uqu.drawbridge.platform.service.InventoryAuditService
import uqu.drawbridge.platform.service.InventoryService
import java.time.LocalDateTime
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
    private val inventoryAuditService: InventoryAuditService
) {

    // ==================== INVENTORY ITEMS ====================

    @GetMapping
    fun getAllInventoryItems(): ResponseEntity<List<InventoryItemDTO>> {
        return ResponseEntity.ok(inventoryService.getAllInventoryItemsDTO())
    }

    @GetMapping("/logs")
    fun getInventoryAuditLogs(
        authentication: Authentication,
        @RequestParam(required = false) productId: String?,
        @RequestParam(required = false) inventoryItemId: String?,
        @RequestParam(required = false) stockTargetType: InventoryStockTargetType?,
        @RequestParam(required = false) sourceType: InventoryAuditSourceType?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<InventoryAuditLogPageResponse> {
        val normalizedProductId = productId?.takeIf { it.isNotBlank() }
        val normalizedInventoryItemId = inventoryItemId?.takeIf { it.isNotBlank() }
        if (normalizedProductId == null && normalizedInventoryItemId == null) {
            throw IllegalArgumentException("productId or inventoryItemId is required")
        }

        if (!inventoryAuditService.canAccessLogs(authentication.name, normalizedProductId, normalizedInventoryItemId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        return ResponseEntity.ok(
            inventoryAuditService.getLogs(
                productId = normalizedProductId,
                inventoryItemId = normalizedInventoryItemId,
                stockTargetType = stockTargetType,
                sourceType = sourceType,
                from = parseAuditDateTime(from),
                to = parseAuditDateTime(to),
                page = page,
                size = size
            )
        )
    }

    @GetMapping("/{id}")
    fun getInventoryItemById(@PathVariable id: String): ResponseEntity<InventoryItemDTO> {
        val item = inventoryService.getInventoryItemDTOById(id)
        return if (item != null) {
            ResponseEntity.ok(item)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/retailer/{retailerId}")
    fun getInventoryItemsByRetailer(@PathVariable retailerId: String): ResponseEntity<List<InventoryItemDTO>> {
        return ResponseEntity.ok(inventoryService.getInventoryItemsDTOByRetailer(retailerId))
    }

    @GetMapping("/product/{productId}")
    fun getInventoryItemsByProduct(@PathVariable productId: String): ResponseEntity<List<InventoryItemDTO>> {
        return ResponseEntity.ok(inventoryService.getInventoryItemsDTOByProduct(productId))
    }

    @GetMapping("/low-stock")
    fun getLowStockItems(@RequestParam(defaultValue = "10") threshold: Int): ResponseEntity<List<InventoryItemDTO>> {
        return ResponseEntity.ok(inventoryService.getLowStockItemsDTO(threshold))
    }

    @PostMapping
    fun createInventoryItem(@RequestBody request: CreateInventoryItemRequest): ResponseEntity<InventoryItemDTO> {
        val created = inventoryService.createInventoryItemFromRequest(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateInventoryItem(
        @PathVariable id: String,
        @RequestBody request: CreateInventoryItemRequest
    ): ResponseEntity<InventoryItemDTO> {
        val updated = inventoryService.updateInventoryItemFromRequest(id, request)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/quantity")
    fun updateQuantity(
        @PathVariable id: String,
        @RequestParam quantity: Int
    ): ResponseEntity<InventoryItemDTO> {
        val updated = inventoryService.updateQuantityDTO(id, quantity)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteInventoryItem(@PathVariable id: String): ResponseEntity<Void> {
        return if (inventoryService.deleteInventoryItem(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== AUTO ORDER CONFIG ====================

    @GetMapping("/auto-order/{inventoryId}")
    fun getAutoOrderConfigByInventoryId(@PathVariable inventoryId: String): ResponseEntity<AutoOrderConfigDTO> {
        val config = inventoryService.getAutoOrderConfigDTOByInventoryId(inventoryId)
        return if (config != null) {
            ResponseEntity.ok(config)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{inventoryId}/auto-order")
    fun setAutoOrderConfig(
        @PathVariable inventoryId: String,
        @RequestBody request: UpdateAutoOrderConfigRequest
    ): ResponseEntity<InventoryItemDTO> {
        val updated = inventoryService.setAutoOrderConfigFromRequest(inventoryId, request)
        return if (updated != null) {
            ResponseEntity.status(HttpStatus.CREATED).body(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/auto-order/{inventoryId}")
    fun updateAutoOrderConfig(
        @PathVariable inventoryId: String,
        @RequestBody request: UpdateAutoOrderConfigRequest
    ): ResponseEntity<AutoOrderConfigDTO> {
        val updatedConfig = inventoryService.updateAutoOrderConfigFromRequest(inventoryId, request)
        return if (updatedConfig != null) {
            ResponseEntity.ok(updatedConfig)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/auto-order/{inventoryId}/toggle")
    fun toggleAutoOrderConfig(
        @PathVariable inventoryId: String,
        @RequestParam enabled: Boolean
    ): ResponseEntity<AutoOrderConfigDTO> {
        val updatedConfig = inventoryService.toggleAutoOrderConfigDTO(inventoryId, enabled)
        return if (updatedConfig != null) {
            ResponseEntity.ok(updatedConfig)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun parseAuditDateTime(value: String?): LocalDateTime? {
        val normalized = value?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { LocalDateTime.parse(normalized) }
            .recoverCatching { OffsetDateTime.parse(normalized).toLocalDateTime() }
            .getOrElse { throw IllegalArgumentException("Invalid date filter: $normalized") }
    }
}
