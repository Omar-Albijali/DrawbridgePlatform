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

    fun findByWholesalerId(wholesalerId: String): List<Order>
    fun findByStatus(status: OrderStatus): List<Order>
    fun findByWholesalerIdAndStatus(wholesalerId: String, status: OrderStatus): List<Order>
    fun findByAutoOrderTrue(): List<Order>
    fun findByOrderGroupId(orderGroupId: String): List<Order>
    fun findByOrderGroupIdIn(orderGroupIds: List<String>): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.retailerId = :retailerId")
    fun findByRetailerId(retailerId: String): List<Order>
    
    @Query("SELECT o FROM Order o WHERE o.retailerId = :retailerId AND o.status = :status")
    fun findByRetailerIdAndStatus(retailerId: String, status: OrderStatus): List<Order>
}
