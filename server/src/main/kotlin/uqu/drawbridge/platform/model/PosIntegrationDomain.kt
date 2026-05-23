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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.LocalDateTime
import uqu.drawbridge.platform.dto.PosOutboundEventStatus
import uqu.drawbridge.platform.dto.InventoryAuditSourceType

enum class PosIntegrationStatus {
    ACTIVE,
    DISABLED
}

enum class PosEventReceiptStatus {
    PROCESSING,
    PROCESSED
}


@Entity
@Table(
    name = "pos_integrations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_pos_integrations_retailer", columnNames = ["retailer_id"]),
        UniqueConstraint(name = "uk_pos_integrations_api_key_hash", columnNames = ["api_key_hash"])
    ]
)
class PosIntegration(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "api_key_hash", nullable = false, length = 64)
    var apiKeyHash: String,

    @Column(name = "api_key_prefix", nullable = false, length = 24)
    var apiKeyPrefix: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PosIntegrationStatus = PosIntegrationStatus.ACTIVE,

    @Column(name = "webhook_enabled", nullable = false)
    var webhookEnabled: Boolean = false,

    @Column(name = "webhook_url", nullable = true, length = 2048)
    var webhookUrl: String? = null,

    @Column(name = "webhook_secret", nullable = true, length = 512)
    var webhookSecret: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "rotated_at", nullable = true)
    var rotatedAt: LocalDateTime? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var retailer: User
) {
    val retailerId: String
        get() = retailer.id ?: ""
}

@Entity
@Table(
    name = "pos_event_receipts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_pos_event_receipts_retailer_event_type",
            columnNames = ["retailer_id", "event_id", "event_type"]
        )
    ],
    indexes = [
        Index(name = "idx_pos_receipts_retailer_processed", columnList = "retailer_id, processed_at")
    ]
)
class PosEventReceipt(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "event_id", nullable = false, length = 128)
    var eventId: String,

    @Column(name = "event_type", nullable = false, length = 64)
    var eventType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PosEventReceiptStatus = PosEventReceiptStatus.PROCESSING,

    @Column(name = "processed_at", nullable = false)
    var processedAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var retailer: User
) {
    val retailerId: String
        get() = retailer.id ?: ""
}

@Entity
@Table(
    name = "pos_outbound_inventory_events",
    uniqueConstraints = [UniqueConstraint(name = "uk_pos_outbound_event_id", columnNames = ["event_id"])],
    indexes = [
        Index(name = "idx_pos_outbound_retailer_created", columnList = "retailer_id, created_at"),
        Index(name = "idx_pos_outbound_status_retry", columnList = "status, next_retry_at")
    ]
)
class PosOutboundInventoryEvent(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(name = "event_id", nullable = false, length = 128)
    var eventId: String,

    @Column(name = "event_type", nullable = false, length = 64)
    var eventType: String = "inventory.changed",

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    var sourceType: InventoryAuditSourceType,

    @Column(name = "source_id", nullable = true)
    var sourceId: String? = null,

    @Column(name = "gtin", nullable = false)
    var gtin: String,

    @Column(name = "quantity_before", nullable = false)
    var quantityBefore: Int,

    @Column(name = "quantity_after", nullable = false)
    var quantityAfter: Int,

    @Column(name = "change_amount", nullable = false)
    var changeAmount: Int,

    @Column(name = "reason", nullable = true, columnDefinition = "TEXT")
    var reason: String? = null,

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    var payload: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PosOutboundEventStatus = PosOutboundEventStatus.STORED,

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0,

    @Column(name = "next_retry_at", nullable = true)
    var nextRetryAt: LocalDateTime? = null,

    @Column(name = "last_attempt_at", nullable = true)
    var lastAttemptAt: LocalDateTime? = null,

    @Column(name = "delivered_at", nullable = true)
    var deliveredAt: LocalDateTime? = null,

    @Column(name = "last_error", nullable = true, columnDefinition = "TEXT")
    var lastError: String? = null,

    @Column(name = "event_time", nullable = false)
    var eventTime: LocalDateTime,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retailer_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var retailer: User,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var product: Product,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var inventoryItem: InventoryItem
) {
    val retailerId: String
        get() = retailer.id ?: ""

    val productId: String
        get() = product.id ?: ""
        
    val inventoryItemId: String
        get() = inventoryItem.id ?: ""
}
