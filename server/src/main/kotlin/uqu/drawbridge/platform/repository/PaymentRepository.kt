package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Payment
import uqu.drawbridge.platform.PaymentStatus

interface PaymentRepository : JpaRepository<Payment, String> {
    fun findByOrderId(orderId: String): List<Payment>
    fun findByOwnerId(ownerId: String): List<Payment>
    fun findByStatus(status: PaymentStatus): List<Payment>
    fun findByTransactionRef(transactionRef: String): Payment?
}
