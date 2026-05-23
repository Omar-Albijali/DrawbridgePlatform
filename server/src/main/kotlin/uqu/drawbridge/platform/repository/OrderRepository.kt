package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import uqu.drawbridge.platform.model.Order
import uqu.drawbridge.platform.OrderStatus

interface OrderRepository : JpaRepository<Order, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    fun findByIdForUpdate(@Param("id") id: String): Order?

    fun findByWholesaler_Id(wholesalerId: String): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
    fun findByWholesaler_IdAndStatus(wholesalerId: String, status: OrderStatus): List<Order>
    fun findByAutoOrderTrue(): List<Order>
    fun findByOrderGroup_IdIn(orderGroupIds: List<String>): List<Order>
    fun findByOrderGroup_Id(orderGroupId: String): List<Order>
    fun findByRetailer_Id(retailerId: String): List<Order>
    fun findByRetailer_IdAndStatus(retailerId: String, status: OrderStatus): List<Order>
}
