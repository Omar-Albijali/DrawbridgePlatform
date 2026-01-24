package uqu.drawbridge.platform.model


import jakarta.persistence.*
import java.time.LocalDateTime
import uqu.drawbridge.platform.ScheduleType

@Entity
@Table(name = "inventory_items")
open class InventoryItem(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var retailerId: String,
    
    @Column(nullable = false)
    open var productId: String,

    @Column(nullable = false)
    open var currentQuantity: Int,

    @Column(nullable = false)
    open var lastUpdated: LocalDateTime = LocalDateTime.now(),

    @Embedded
    open var autoOrderConfig: AutoOrderConfig
)



@Embeddable
open class AutoOrderConfig(
    @Column(name = "auto_order_enabled", nullable = false)
    open var enabled: Boolean = false,

    @Column(name = "auto_order_min_threshold", nullable = false)
    open var minThreshold: Int = 0,

    @Column(name = "auto_order_reorder_quantity", nullable = false)
    open var reorderQuantity: Int = 0,

    // Scheduling configuration
    @Enumerated(EnumType.STRING)
    @Column(name = "auto_order_schedule_type", nullable = false)
    open var scheduleType: ScheduleType = ScheduleType.THRESHOLD_BASED,

    // For INTERVAL_DAYS: number of days between orders
    @Column(name = "auto_order_interval_days", nullable = true)
    open var intervalDays: Int? = null,

    // For WEEKLY: day of week (1=Monday, 7=Sunday) - can store comma-separated for multiple days
    @Column(name = "auto_order_day_of_week", nullable = true)
    open var dayOfWeek: String? = null,

    // For MONTHLY: day of month (1-31) - can store comma-separated for multiple days
    @Column(name = "auto_order_day_of_month", nullable = true)
    open var dayOfMonth: String? = null,

    // Next scheduled order date (calculated based on schedule type)
    @Column(name = "auto_order_next_scheduled_at", nullable = true)
    open var nextScheduledAt: LocalDateTime? = null,

    @Column(name = "auto_order_last_triggered_at", nullable = true)
    open var lastTriggeredAt: LocalDateTime? = null

)