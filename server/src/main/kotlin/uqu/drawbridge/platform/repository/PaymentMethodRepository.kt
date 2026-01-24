package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.PaymentMethod
import uqu.drawbridge.platform.PaymentMethodType

interface PaymentMethodRepository : JpaRepository<PaymentMethod, String> {
    fun findByOwnerId(ownerId: String): List<PaymentMethod>
    fun findByOwnerIdAndType(ownerId: String, type: PaymentMethodType): List<PaymentMethod>
    fun findByOwnerIdAndIsDefaultTrue(ownerId: String): PaymentMethod?
}
