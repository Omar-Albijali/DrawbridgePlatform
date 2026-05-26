package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uqu.drawbridge.platform.model.OrderItem

interface OrderItemRepository : JpaRepository<OrderItem, String> {
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    fun findByOrder_Id(orderId: String): List<OrderItem>
    
    fun findByProduct_Id(productId: String): List<OrderItem>
}
