package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Payment
import uqu.drawbridge.platform.PaymentStatus

interface PaymentRepository : JpaRepository<Payment, String> {
    fun findByOrder_Id(orderId: String): List<Payment>
    fun findByOwner_Id(ownerId: String): List<Payment>
    fun findByStatus(status: PaymentStatus): List<Payment>
    fun findByTransactionRef(transactionRef: String): Payment?
}
