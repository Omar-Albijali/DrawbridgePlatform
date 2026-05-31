package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import uqu.drawbridge.platform.model.OrderItem

interface OrderItemRepository : JpaRepository<OrderItem, String> {
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    fun findByOrder_Id(orderId: String): List<OrderItem>

    fun findByProduct_Id(productId: String): List<OrderItem>

    @Query("""
SELECT oi.product.id, SUM(oi.quantity) as orderCount
FROM OrderItem oi
WHERE oi.order.retailer.id = :retailerId
GROUP BY oi.product.id
ORDER BY orderCount DESC
""")
    fun findMostOrderedProductIdsByRetailer(
        @Param("retailerId") retailerId: String,
        @Param("minCount") minCount: Long = 3
    ): List<Array<Any>>
}
