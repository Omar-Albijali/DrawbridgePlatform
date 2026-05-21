package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore

import jakarta.persistence.*

import java.math.BigDecimal
import java.time.LocalDateTime
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.PaymentMethodType

@Entity
@Table(name = "payments")
class Payment(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var orderId: String,

    @Column(nullable = false)
    var ownerId: String,

    @Column(nullable = false)
    var paymentMethodId: String,

    @Column(nullable = false)
    var amount: BigDecimal,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus,

    @Column(nullable = false)
    var transactionRef: String,

    @Column(nullable = false)
    var completedAt: LocalDateTime,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", insertable = false, updatable = false)
    var order: Order? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", insertable = false, updatable = false)
    var owner: User? = null,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paymentMethodId", insertable = false, updatable = false)
    var paymentMethod: PaymentMethod? = null
)

@Entity
@Table(name = "invoices")
class Invoice(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var orderId: String,

    @Column(nullable = false, unique = true)
    var invoiceNumber: String,

    @Column(nullable = false)
    var issueDate: LocalDateTime,

    @Column(nullable = false)
    var dueDate: LocalDateTime,

    @Column(nullable = false)
    var totalAmount: BigDecimal,

    @Column(nullable = false)
    var currency: String,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", insertable = false, updatable = false)
    var order: Order? = null
)

@Entity
@Table(name = "payment_methods")
class PaymentMethod(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    var ownerId: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: PaymentMethodType,

    @Column(nullable = false)
    var maskedDetails: String,

    @Column(nullable = false)
    var isDefault: Boolean = false,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownerId", insertable = false, updatable = false)
    var owner: User? = null
)
