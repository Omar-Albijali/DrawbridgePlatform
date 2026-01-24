package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderGroupDTO
import uqu.drawbridge.platform.model.Order
import uqu.drawbridge.platform.model.OrderGroup
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.service.OrderService
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    // ==================== ORDERS ====================

    @GetMapping
    fun getAllOrders(): ResponseEntity<List<OrderDTO>> {
        return ResponseEntity.ok(orderService.getAllOrdersDTO())
    }

    @GetMapping("/{id}")
    fun getOrderById(@PathVariable id: String): ResponseEntity<OrderDTO> {
        val orderDTO = orderService.getOrderDTOById(id)
        return if (orderDTO != null) {
            ResponseEntity.ok(orderDTO)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/group/{groupId}")
    fun getOrdersByGroup(@PathVariable groupId: String): ResponseEntity<List<OrderDTO>> {
        return ResponseEntity.ok(orderService.getOrdersDTOByOrderGroup(groupId))
    }

    @GetMapping("/retailer/{retailerId}")
    fun getOrdersByRetailer(@PathVariable retailerId: String): ResponseEntity<List<OrderDTO>> {
        return ResponseEntity.ok(orderService.getOrdersDTOByRetailer(retailerId))
    }

    @GetMapping("/wholesaler/{wholesalerId}")
    fun getOrdersByWholesaler(@PathVariable wholesalerId: String): ResponseEntity<List<OrderDTO>> {
        return ResponseEntity.ok(orderService.getOrdersDTOByWholesaler(wholesalerId))
    }

    @PatchMapping("/{id}/status")
    fun updateOrderStatus(
        @PathVariable id: String,
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderDTO> {
        val updated = orderService.updateOrderStatusDTO(id, status)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/tracking")
    fun updateOrderTracking(
        @PathVariable id: String,
        @RequestBody request: uqu.drawbridge.platform.UpdateOrderTrackingRequest
    ): ResponseEntity<OrderDTO> {
        // Parsing estimatedDelivery from String to LocalDateTime if provided
        val estimatedDelivery = request.estimatedDelivery?.let { 
            try {
                LocalDateTime.parse(it) 
            } catch (e: Exception) {
                null 
            }
        }
        
        val updated = orderService.updateOrderTrackingDTO(
            id, 
            request.shippingMethod,
            request.trackingNumber, 
            request.trackingUrl, 
            estimatedDelivery
        )
        
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/{id}")
    fun cancelOrder(@PathVariable id: String): ResponseEntity<OrderDTO> {
        val cancelled = orderService.cancelOrderDTO(id)
        return if (cancelled != null) {
            ResponseEntity.ok(cancelled)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== ORDER GROUPS ====================

    @GetMapping("/groups/retailer/{retailerId}")
    fun getOrderGroupsByRetailer(@PathVariable retailerId: String): ResponseEntity<List<OrderGroupDTO>> {
        return ResponseEntity.ok(orderService.getOrderGroupsDTOByRetailer(retailerId))
    }
    
    @GetMapping("/groups/{id}")
    fun getOrderGroupById(@PathVariable id: String): ResponseEntity<OrderGroupDTO> {
        val group = orderService.getOrderGroupDTOById(id)
        return if (group != null) {
            ResponseEntity.ok(group)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
