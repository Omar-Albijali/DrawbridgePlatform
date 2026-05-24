package uqu.drawbridge.platform.service

import jakarta.persistence.criteria.Predicate
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.dto.InventoryAuditLogDTO
import uqu.drawbridge.platform.dto.InventoryAuditLogPageResponse
import uqu.drawbridge.platform.model.InventoryAuditChangeType
import uqu.drawbridge.platform.model.InventoryAuditLog
import uqu.drawbridge.platform.dto.InventoryAuditSourceType
import uqu.drawbridge.platform.model.InventoryStockTargetType
import uqu.drawbridge.platform.repository.InventoryAuditLogRepository
import uqu.drawbridge.platform.repository.InventoryItemRepository
import uqu.drawbridge.platform.repository.ProductRepository
import uqu.drawbridge.platform.repository.UserRepository

@Service
class InventoryAuditService(
    private val inventoryAuditLogRepository: InventoryAuditLogRepository,
    private val inventoryItemRepository: InventoryItemRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val posOutboundInventoryEventService: PosOutboundInventoryEventService
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun logStockChange(
        productId: String,
        inventoryItemId: String? = null,
        stockTargetType: InventoryStockTargetType,
        sourceType: InventoryAuditSourceType,
        sourceId: String? = null,
        quantityBefore: Int,
        quantityAfter: Int,
        changedBy: String? = null,
        reason: String? = null
    ): InventoryAuditLog? {
        if (quantityBefore == quantityAfter) {
            return null
        }

        val changeAmount = quantityAfter - quantityBefore
        val changeType = when {
            changeAmount > 0 -> InventoryAuditChangeType.INCREASE
            changeAmount < 0 -> InventoryAuditChangeType.DECREASE
            else -> InventoryAuditChangeType.UPDATE
        }

        val savedLog = inventoryAuditLogRepository.save(
            InventoryAuditLog(
                productId = productId,
                inventoryItemId = inventoryItemId,
                stockTargetType = stockTargetType,
                changeType = changeType,
                sourceType = sourceType,
                sourceId = sourceId,
                quantityBefore = quantityBefore,
                quantityAfter = quantityAfter,
                changeAmount = changeAmount,
                changedBy = changedBy?.takeIf { it.isNotBlank() } ?: currentActor(),
                reason = reason,
                createdAt = LocalDateTime.now()
            )
        )
        runCatching { posOutboundInventoryEventService.captureInventoryAuditChange(savedLog) }
        return savedLog
    }

    @Transactional(readOnly = true)
    fun getLogs(
        productId: String?,
        inventoryItemId: String?,
        stockTargetType: InventoryStockTargetType?,
        sourceType: InventoryAuditSourceType?,
        from: LocalDateTime?,
        to: LocalDateTime?,
        page: Int,
        size: Int
    ): InventoryAuditLogPageResponse {
        if (productId.isNullOrBlank() && inventoryItemId.isNullOrBlank()) {
            throw IllegalArgumentException("productId or inventoryItemId is required")
        }

        val safePage = page.coerceAtLeast(0)
        val safeSize = size.coerceIn(1, 100)
        val pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        val spec = buildLogSearchSpec(
            productId = productId?.takeIf { it.isNotBlank() },
            inventoryItemId = inventoryItemId?.takeIf { it.isNotBlank() },
            stockTargetType = stockTargetType,
            sourceType = sourceType,
            from = from,
            to = to
        )
        val result = inventoryAuditLogRepository.findAll(spec, pageable)

        return InventoryAuditLogPageResponse(
            items = result.content.map { it.toDTO() },
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages
        )
    }

    @Transactional(readOnly = true)
    fun canAccessLogs(authenticationName: String, productId: String?, inventoryItemId: String?): Boolean {
        val user = userRepository.findByEmail(authenticationName) ?: return false
        val userId = user.id ?: return false
        val normalizedProductId = productId?.takeIf { it.isNotBlank() }
        val normalizedInventoryItemId = inventoryItemId?.takeIf { it.isNotBlank() }

        if (normalizedInventoryItemId != null) {
            val inventoryItem = inventoryItemRepository.findById(normalizedInventoryItemId).orElse(null) ?: return false
            if (normalizedProductId != null && inventoryItem.productId != normalizedProductId) {
                return false
            }
            if (inventoryItem.retailerId == userId) {
                return true
            }

            val product = productRepository.findById(inventoryItem.productId).orElse(null)
            return product?.wholesaler?.id == userId
        }

        if (normalizedProductId != null) {
            val product = productRepository.findById(normalizedProductId).orElse(null) ?: return false
            return product.wholesaler.id == userId
        }

        return false
    }

    private fun buildLogSearchSpec(
        productId: String?,
        inventoryItemId: String?,
        stockTargetType: InventoryStockTargetType?,
        sourceType: InventoryAuditSourceType?,
        from: LocalDateTime?,
        to: LocalDateTime?
    ): Specification<InventoryAuditLog> {
        return Specification { root, _, criteriaBuilder ->
            val predicates = mutableListOf<Predicate>()

            productId?.let {
                predicates += criteriaBuilder.equal(root.get<String>("productId"), it)
            }
            inventoryItemId?.let {
                predicates += criteriaBuilder.equal(root.get<String>("inventoryItemId"), it)
            }
            stockTargetType?.let {
                predicates += criteriaBuilder.equal(root.get<InventoryStockTargetType>("stockTargetType"), it)
            }
            sourceType?.let {
                predicates += criteriaBuilder.equal(root.get<InventoryAuditSourceType>("sourceType"), it)
            }
            from?.let {
                predicates += criteriaBuilder.greaterThanOrEqualTo(root.get<LocalDateTime>("createdAt"), it)
            }
            to?.let {
                predicates += criteriaBuilder.lessThanOrEqualTo(root.get<LocalDateTime>("createdAt"), it)
            }

            if (predicates.isEmpty()) {
                criteriaBuilder.conjunction()
            } else {
                criteriaBuilder.and(*predicates.toTypedArray())
            }
        }
    }

    private fun InventoryAuditLog.toDTO(): InventoryAuditLogDTO {
        return InventoryAuditLogDTO(
            id = this.id ?: "",
            productId = this.productId,
            inventoryItemId = this.inventoryItemId,
            stockTargetType = this.stockTargetType,
            changeType = this.changeType,
            sourceType = this.sourceType,
            sourceId = this.sourceId,
            quantityBefore = this.quantityBefore,
            quantityAfter = this.quantityAfter,
            changeAmount = this.changeAmount,
            changedBy = this.changedBy,
            reason = this.reason,
            createdAt = this.createdAt.toString()
        )
    }

    private fun currentActor(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val name = authentication?.name
        if (authentication?.isAuthenticated == true && !name.isNullOrBlank() && name != "anonymousUser") {
            return name
        }
        return "SYSTEM"
    }
}
