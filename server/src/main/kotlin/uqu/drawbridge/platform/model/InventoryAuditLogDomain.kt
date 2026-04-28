package uqu.drawbridge.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class InventoryStockTargetType {
    PRODUCT_CATALOG,
    RETAILER_INVENTORY
}

enum class InventoryAuditChangeType {
    INCREASE,
    DECREASE,
    UPDATE
}

enum class InventoryAuditSourceType {
    MANUAL,
    ORDER,
    RESTOCK,
    POS,
    SYSTEM
}

@Entity
@Table(
    name = "inventory_audit_logs",
    indexes = [
        Index(name = "idx_inventory_audit_product_created", columnList = "product_id, created_at"),
        Index(name = "idx_inventory_audit_item_created", columnList = "inventory_item_id, created_at"),
        Index(name = "idx_inventory_audit_source_created", columnList = "source_type, created_at")
    ]
)
open class InventoryAuditLog(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(name = "product_id", nullable = false)
    open var productId: String,

    @Column(name = "inventory_item_id", nullable = true)
    open var inventoryItemId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_target_type", nullable = false)
    open var stockTargetType: InventoryStockTargetType,

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    open var changeType: InventoryAuditChangeType,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    open var sourceType: InventoryAuditSourceType,

    @Column(name = "source_id", nullable = true)
    open var sourceId: String? = null,

    @Column(name = "quantity_before", nullable = false)
    open var quantityBefore: Int,

    @Column(name = "quantity_after", nullable = false)
    open var quantityAfter: Int,

    @Column(name = "change_amount", nullable = false)
    open var changeAmount: Int,

    @Column(name = "changed_by", nullable = false)
    open var changedBy: String,

    @Column(nullable = true, columnDefinition = "TEXT")
    open var reason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now()
)
