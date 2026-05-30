package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.PaymentMethod
import uqu.drawbridge.platform.PaymentMethodType

interface PaymentMethodRepository : JpaRepository<PaymentMethod, String> {
    fun findByOwner_Id(ownerId: String): List<PaymentMethod>
    fun findByOwner_IdAndType(ownerId: String, type: PaymentMethodType): List<PaymentMethod>
    fun findByOwner_IdAndIsDefaultTrue(ownerId: String): PaymentMethod?
}
