package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime
import uqu.drawbridge.platform.dto.InventoryAuditSourceType

enum class InventoryStockTargetType {
    PRODUCT_CATALOG,
    RETAILER_INVENTORY
}

enum class InventoryAuditChangeType {
    INCREASE,
    DECREASE,
    UPDATE
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
class InventoryAuditLog(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var product: Product? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var inventoryItem: InventoryItem? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_target_type", nullable = false)
    var stockTargetType: InventoryStockTargetType,

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    var changeType: InventoryAuditChangeType,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    var sourceType: InventoryAuditSourceType,

    @Column(name = "source_id", nullable = true)
    var sourceId: String? = null,

    @Column(name = "quantity_before", nullable = false)
    var quantityBefore: Int,

    @Column(name = "quantity_after", nullable = false)
    var quantityAfter: Int,

    @Column(name = "change_amount", nullable = false)
    var changeAmount: Int,

    @Column(name = "changed_by", nullable = false)
    var changedBy: String,

    @Column(nullable = true, columnDefinition = "TEXT")
    var reason: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

) {
    val productId: String?
        get() = product?.id

    val inventoryItemId: String?
        get() = inventoryItem?.id
}
