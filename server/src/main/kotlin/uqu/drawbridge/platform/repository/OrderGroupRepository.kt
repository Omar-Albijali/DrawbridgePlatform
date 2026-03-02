package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.OrderGroup
import uqu.drawbridge.platform.PaymentStatus

interface OrderGroupRepository : JpaRepository<OrderGroup, String> {
    fun findByRetailerId(retailerId: String): List<OrderGroup>

    fun findTopByRetailerIdOrderByCreatedAtDesc(retailerId: String): OrderGroup?
    fun findByPaymentStatus(paymentStatus: PaymentStatus): List<OrderGroup>
    fun findByRetailerIdAndPaymentStatus(retailerId: String, paymentStatus: PaymentStatus): List<OrderGroup>
}
