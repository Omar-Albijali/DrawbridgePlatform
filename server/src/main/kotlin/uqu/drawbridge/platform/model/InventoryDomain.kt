package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import java.time.LocalDateTime
import uqu.drawbridge.platform.ScheduleType

@Entity
@Table(name = "inventory_items")
class InventoryItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var retailerId: String,

    @Column(nullable = false)
    var productId: String,

    @Column(nullable = false)
    var currentQuantity: Int,

    @Column(nullable = false)
    var lastUpdated: LocalDateTime = LocalDateTime.now(),

    @Embedded
    var autoOrderConfig: AutoOrderConfig,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailerId", insertable = false, updatable = false)
    var retailer: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "productId", insertable = false, updatable = false)
    var product: Product? = null
)



@Embeddable
class AutoOrderConfig(
    @Column(name = "auto_order_enabled", nullable = false)
    var enabled: Boolean = false,

    @Column(name = "auto_order_min_threshold", nullable = false)
    var minThreshold: Int = 0,

    @Column(name = "auto_order_reorder_quantity", nullable = false)
    var reorderQuantity: Int = 0,

    // Scheduling configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "auto_order_schedule_type", nullable = false)
    var scheduleType: ScheduleType = ScheduleType.THRESHOLD_BASED,

    // For INTERVAL_DAYS: number of days between orders
    @Column(name = "auto_order_interval_days", nullable = true)
    var intervalDays: Int? = null,

    // For WEEKLY: day of week (1=Monday, 7=Sunday) - can store comma-separated for multiple days
    @Column(name = "auto_order_day_of_week", nullable = true)
    var dayOfWeek: String? = null,

    // For MONTHLY: day of month (1-31) - can store comma-separated for multiple days
    @Column(name = "auto_order_day_of_month", nullable = true)
    var dayOfMonth: String? = null,

    // Next scheduled order date (calculated based on schedule type)
    @Column(name = "auto_order_next_scheduled_at", nullable = true)
    var nextScheduledAt: LocalDateTime? = null,

    @Column(name = "auto_order_last_triggered_at", nullable = true)
    var lastTriggeredAt: LocalDateTime? = null

)
