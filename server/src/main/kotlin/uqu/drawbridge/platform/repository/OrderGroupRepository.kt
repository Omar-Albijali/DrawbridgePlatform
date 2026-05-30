package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.OrderGroup
import uqu.drawbridge.platform.PaymentStatus

interface OrderGroupRepository : JpaRepository<OrderGroup, String> {
    fun findByRetailer_Id(retailerId: String): List<OrderGroup>
    fun findByPaymentStatus(paymentStatus: PaymentStatus): List<OrderGroup>
    fun findByRetailer_IdAndPaymentStatus(retailerId: String, paymentStatus: PaymentStatus): List<OrderGroup>
}
