package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderGroupDTO
import uqu.drawbridge.platform.model.Order
import uqu.drawbridge.platform.model.OrderGroup
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.service.OrderService
import uqu.drawbridge.platform.service.UserService
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
    private val userService: UserService
) {

    // ==================== ORDERS ====================

    @GetMapping
    fun getAllOrders(authentication: Authentication): ResponseEntity<List<OrderDTO>> {
        val user = currentUser(authentication)
        val orders = when (user.role) {
            UserRole.RETAILER -> orderService.getOrdersDTOByRetailer(user.id!!)
            UserRole.WHOLESALER -> orderService.getOrdersDTOByWholesaler(user.id!!)
        }
        return ResponseEntity.ok(orders)
    }

    @GetMapping("/{id}")
    fun getOrderById(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<OrderDTO> {
        val orderDTO = orderService.getOrderDTOById(id)
        return if (orderDTO != null) {
            requireOrderAccess(authentication, orderDTO)
            ResponseEntity.ok(orderDTO)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/group/{groupId}")
    fun getOrdersByGroup(
        authentication: Authentication,
        @PathVariable groupId: String
    ): ResponseEntity<List<OrderDTO>> {
        val orders = orderService.getOrdersDTOByOrderGroup(groupId)
        val user = currentUser(authentication)
        return ResponseEntity.ok(
            orders.filter { order ->
                when (user.role) {
                    UserRole.RETAILER -> order.retailerId == user.id
                    UserRole.WHOLESALER -> order.wholesalerId == user.id
                }
            }
        )
    }

    @GetMapping("/retailer/{retailerId}")
    fun getOrdersByRetailer(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<List<OrderDTO>> {
        requireRetailerOwner(authentication, retailerId)
        return ResponseEntity.ok(orderService.getOrdersDTOByRetailer(retailerId))
    }

    @GetMapping("/wholesaler/{wholesalerId}")
    fun getOrdersByWholesaler(
        authentication: Authentication,
        @PathVariable wholesalerId: String
    ): ResponseEntity<List<OrderDTO>> {
        requireWholesalerOwner(authentication, wholesalerId)
        return ResponseEntity.ok(orderService.getOrdersDTOByWholesaler(wholesalerId))
    }

    @PatchMapping("/{id}/status")
    fun updateOrderStatus(
        authentication: Authentication,
        @PathVariable id: String,
        @RequestParam status: OrderStatus
    ): ResponseEntity<OrderDTO> {
        val existing = orderService.getOrderDTOById(id) ?: return ResponseEntity.notFound().build()
        requireWholesalerOrderOwner(authentication, existing)
        val updated = orderService.updateOrderStatusDTO(id, status)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/confirm-delivery")
    fun confirmDelivery(
        @PathVariable id: String,
        authentication: Authentication
    ): ResponseEntity<OrderDTO> {
        return ResponseEntity.ok(orderService.confirmDeliveryForRetailer(id, authentication.name))
    }

    @PatchMapping("/{id}/tracking")
    fun updateOrderTracking(
        authentication: Authentication,
        @PathVariable id: String,
        @RequestBody request: uqu.drawbridge.platform.UpdateOrderTrackingRequest
    ): ResponseEntity<OrderDTO> {
        val existing = orderService.getOrderDTOById(id) ?: return ResponseEntity.notFound().build()
        requireWholesalerOrderOwner(authentication, existing)
        val estimatedDelivery = request.estimatedDelivery?.let {
            try {
                LocalDateTime.parse(it)
            } catch (_: DateTimeParseException) {
                throw IllegalArgumentException("estimatedDelivery must be an ISO-8601 datetime")
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
    fun cancelOrder(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<OrderDTO> {
        val existing = orderService.getOrderDTOById(id) ?: return ResponseEntity.notFound().build()
        requireRetailerOrderOwner(authentication, existing)
        val cancelled = orderService.cancelOrderDTO(id)
        return if (cancelled != null) {
            ResponseEntity.ok(cancelled)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== ORDER GROUPS ====================

    @GetMapping("/groups/retailer/{retailerId}")
    fun getOrderGroupsByRetailer(
        authentication: Authentication,
        @PathVariable retailerId: String
    ): ResponseEntity<List<OrderGroupDTO>> {
        requireRetailerOwner(authentication, retailerId)
        return ResponseEntity.ok(orderService.getOrderGroupsDTOByRetailer(retailerId))
    }
    
    @GetMapping("/groups/{id}")
    fun getOrderGroupById(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<OrderGroupDTO> {
        val group = orderService.getOrderGroupDTOById(id)
        return if (group != null) {
            requireRetailerOwner(authentication, group.retailerId)
            ResponseEntity.ok(group)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun currentUser(authentication: Authentication): uqu.drawbridge.platform.model.User {
        return userService.getUserByEmail(authentication.name)
            ?: throw AccessDeniedException("Access denied")
    }

    private fun requireRetailerOwner(authentication: Authentication, retailerId: String) {
        val user = currentUser(authentication)
        if (user.id != retailerId || user.role != UserRole.RETAILER) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireWholesalerOwner(authentication: Authentication, wholesalerId: String) {
        val user = currentUser(authentication)
        if (user.id != wholesalerId || user.role != UserRole.WHOLESALER) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireOrderAccess(authentication: Authentication, order: OrderDTO) {
        val user = currentUser(authentication)
        val allowed = when (user.role) {
            UserRole.RETAILER -> order.retailerId == user.id
            UserRole.WHOLESALER -> order.wholesalerId == user.id
        }
        if (!allowed) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireRetailerOrderOwner(authentication: Authentication, order: OrderDTO) {
        val user = currentUser(authentication)
        if (user.role != UserRole.RETAILER || order.retailerId != user.id) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireWholesalerOrderOwner(authentication: Authentication, order: OrderDTO) {
        val user = currentUser(authentication)
        if (user.role != UserRole.WHOLESALER || order.wholesalerId != user.id) {
            throw AccessDeniedException("Access denied")
        }
    }
}
