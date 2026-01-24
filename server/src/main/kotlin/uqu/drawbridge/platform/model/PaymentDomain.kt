package uqu.drawbridge.platform.model


import jakarta.persistence.*

import java.math.BigDecimal
import java.time.LocalDateTime
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.PaymentMethodType

@Entity
@Table(name = "payments")
open class Payment(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var orderId: String,

    @Column(nullable = false)
    open var ownerId: String,

    @Column(nullable = false)
    open var paymentMethodId: String,

    @Column(nullable = false)
    open var amount: BigDecimal,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    open var status: PaymentStatus,

    @Column(nullable = false)
    open var transactionRef: String,

    @Column(nullable = false)
    open var completedAt: LocalDateTime
)

@Entity
@Table(name = "invoices")
open class Invoice(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var orderId: String,

    @Column(nullable = false, unique = true)
    open var invoiceNumber: String,

    @Column(nullable = false)
    open var issueDate: LocalDateTime,

    @Column(nullable = false)
    open var dueDate: LocalDateTime,

    @Column(nullable = false)
    open var totalAmount: BigDecimal,

    @Column(nullable = false)
    open var currency: String
)

@Entity
@Table(name = "payment_methods")
open class PaymentMethod(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    open var id: String? = null,

    @Column(nullable = false)
    open var ownerId: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    open var type: PaymentMethodType,

    @Column(nullable = false)
    open var maskedDetails: String,

    @Column(nullable = false)
    open var isDefault: Boolean = false
)