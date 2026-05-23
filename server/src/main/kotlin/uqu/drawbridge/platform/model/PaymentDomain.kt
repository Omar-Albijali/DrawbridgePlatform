package uqu.drawbridge.platform.model

import com.fasterxml.jackson.annotation.JsonIgnore

import jakarta.persistence.*
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction

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
    var amount: BigDecimal,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: PaymentStatus,

    @Column(nullable = false)
    var transactionRef: String,

    @Column(nullable = false)
    var completedAt: LocalDateTime,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    var owner: User,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    var paymentMethod: PaymentMethod? = null
) {
    val orderId: String
        get() = order.id ?: ""

    val ownerId: String
        get() = owner.id ?: ""

    val paymentMethodId: String?
        get() = paymentMethod?.id
}

@Entity
@Table(name = "invoices")
class Invoice(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order
) {
    val orderId: String
        get() = order.id ?: ""
}

@Entity
@Table(name = "payment_methods")
class PaymentMethod(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: String? = null,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var type: PaymentMethodType,

    @Column(nullable = false)
    var maskedDetails: String,

    @Column(nullable = false)
    var isDefault: Boolean = false,

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var owner: User
) {
    val ownerId: String
        get() = owner.id ?: ""
}
