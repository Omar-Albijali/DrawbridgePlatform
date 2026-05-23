package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.model.InventoryItem
import uqu.drawbridge.platform.dto.InventoryAuditLogPageResponse
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.model.InventoryStockTargetType
import uqu.drawbridge.platform.service.InventoryAuditService
import uqu.drawbridge.platform.service.InventoryService
import uqu.drawbridge.platform.service.ProductService
import uqu.drawbridge.platform.service.UserService
import java.time.LocalDateTime
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/inventory")
class InventoryController(
    private val inventoryService: InventoryService,
    private val inventoryAuditService: InventoryAuditService,
    private val productService: ProductService,
    private val userService: UserService
) {

    // ==================== INVENTORY ITEMS ====================

    @GetMapping
    fun getAllInventoryItems(authentication: Authentication): ResponseEntity<List<InventoryItemDTO>> {
        val user = currentUser(authentication)
        if (user.role != UserRole.RETAILER || user.id == null) {
            throw AccessDeniedException("Access denied")
        }
        return ResponseEntity.ok(inventoryService.getInventoryItemsDTOByRetailer(user.id!!))
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
    fun getInventoryItemById(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<InventoryItemDTO> {
        val existing = inventoryService.getInventoryItemById(id) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        val item = inventoryService.getInventoryItemDTOById(id)
        return if (item != null) {
            ResponseEntity.ok(item)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/retailer/{retailerId}")
    fun getInventoryItemsByRetailer(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<List<InventoryItemDTO>> {
        requireRetailerOwner(authentication, retailerId)
        return ResponseEntity.ok(inventoryService.getInventoryItemsDTOByRetailer(retailerId))
    }

    @GetMapping("/product/{productId}")
    fun getInventoryItemsByProduct(
        authentication: Authentication,
        @PathVariable productId: String
    ): ResponseEntity<List<InventoryItemDTO>> {
        val product = productService.getProductById(productId) ?: return ResponseEntity.notFound().build()
        requireProductOwner(authentication, product)
        return ResponseEntity.ok(inventoryService.getInventoryItemsDTOByProduct(productId))
    }

    @GetMapping("/low-stock")
    fun getLowStockItems(
        authentication: Authentication,
        @RequestParam(defaultValue = "10") threshold: Int
    ): ResponseEntity<List<InventoryItemDTO>> {
        val user = currentUser(authentication)
        if (user.role != UserRole.RETAILER || user.id == null) {
            throw AccessDeniedException("Access denied")
        }
        return ResponseEntity.ok(
            inventoryService.getInventoryItemsDTOByRetailer(user.id!!)
                .filter { it.currentStock <= threshold }
        )
    }

    @PostMapping
    fun createInventoryItem(
        authentication: Authentication,
        @RequestBody request: CreateInventoryItemRequest
    ): ResponseEntity<InventoryItemDTO> {
        requireRetailerOwner(authentication, request.retailerId)
        val created = inventoryService.createInventoryItemFromRequest(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateInventoryItem(
        authentication: Authentication,
        @PathVariable id: String,
        @RequestBody request: CreateInventoryItemRequest
    ): ResponseEntity<InventoryItemDTO> {
        val existing = inventoryService.getInventoryItemById(id) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        requireRetailerOwner(authentication, request.retailerId)
        val updated = inventoryService.updateInventoryItemFromRequest(id, request)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/quantity")
    fun updateQuantity(
        authentication: Authentication,
        @PathVariable id: String,
        @RequestParam quantity: Int
    ): ResponseEntity<InventoryItemDTO> {
        val existing = inventoryService.getInventoryItemById(id) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        val updated = inventoryService.updateQuantityDTO(id, quantity)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun deleteInventoryItem(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val existing = inventoryService.getInventoryItemById(id) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        return if (inventoryService.deleteInventoryItem(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== AUTO ORDER CONFIG ====================

    @GetMapping("/auto-order/{inventoryId}")
    fun getAutoOrderConfigByInventoryId(
        authentication: Authentication,
        @PathVariable inventoryId: String
    ): ResponseEntity<AutoOrderConfigDTO> {
        val existing = inventoryService.getInventoryItemById(inventoryId) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        val config = inventoryService.getAutoOrderConfigDTOByInventoryId(inventoryId)
        return if (config != null) {
            ResponseEntity.ok(config)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{inventoryId}/auto-order")
    fun setAutoOrderConfig(
        authentication: Authentication,
        @PathVariable inventoryId: String,
        @RequestBody request: UpdateAutoOrderConfigRequest
    ): ResponseEntity<InventoryItemDTO> {
        val existing = inventoryService.getInventoryItemById(inventoryId) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        val updated = inventoryService.setAutoOrderConfigFromRequest(inventoryId, request)
        return if (updated != null) {
            ResponseEntity.status(HttpStatus.CREATED).body(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/auto-order/{inventoryId}")
    fun updateAutoOrderConfig(
        authentication: Authentication,
        @PathVariable inventoryId: String,
        @RequestBody request: UpdateAutoOrderConfigRequest
    ): ResponseEntity<AutoOrderConfigDTO> {
        val existing = inventoryService.getInventoryItemById(inventoryId) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
        val updatedConfig = inventoryService.updateAutoOrderConfigFromRequest(inventoryId, request)
        return if (updatedConfig != null) {
            ResponseEntity.ok(updatedConfig)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/auto-order/{inventoryId}/toggle")
    fun toggleAutoOrderConfig(
        authentication: Authentication,
        @PathVariable inventoryId: String,
        @RequestParam enabled: Boolean
    ): ResponseEntity<AutoOrderConfigDTO> {
        val existing = inventoryService.getInventoryItemById(inventoryId) ?: return ResponseEntity.notFound().build()
        requireInventoryOwner(authentication, existing)
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

    private fun currentUser(authentication: Authentication): uqu.drawbridge.platform.model.User {
        return userService.getUserByEmail(authentication.name)
            ?: throw AccessDeniedException("Access denied")
    }

    private fun requireRetailerOwner(authentication: Authentication, retailerId: String) {
        val user = currentUser(authentication)
        if (user.role != UserRole.RETAILER || user.id != retailerId) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireInventoryOwner(authentication: Authentication, item: InventoryItem) {
        requireRetailerOwner(authentication, item.retailerId ?: "")
    }

    private fun requireProductOwner(authentication: Authentication, product: uqu.drawbridge.platform.model.Product) {
        val user = currentUser(authentication)
        if (user.role != UserRole.WHOLESALER || product.wholesaler.id != user.id) {
            throw AccessDeniedException("Access denied")
        }
    }
}
