package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.service.OrderService
import uqu.drawbridge.platform.service.PaymentService
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService,
    private val orderService: OrderService,
    private val userService: UserService
) {

    // ==================== PAYMENTS ====================

    @GetMapping
    fun getAllPayments(authentication: Authentication): ResponseEntity<List<PaymentDTO>> {
        val user = currentUser(authentication)
        val payments = when (user.role) {
            UserRole.RETAILER -> paymentService.getPaymentsDTOByOwner(user.id!!)
            UserRole.WHOLESALER -> orderService.getOrdersDTOByWholesaler(user.id!!)
                .flatMap { order -> paymentService.getPaymentsDTOByOrderId(order.id) }
        }
        return ResponseEntity.ok(payments)
    }

    @GetMapping("/{id}")
    fun getPaymentById(authentication: Authentication, @PathVariable id: String): ResponseEntity<PaymentDTO> {
        val payment = paymentService.getPaymentDTOById(id)
        return if (payment != null) {
            requirePaymentAccess(authentication, payment)
            ResponseEntity.ok(payment)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/order/{orderId}")
    fun getPaymentsByOrder(authentication: Authentication, @PathVariable orderId: String): ResponseEntity<List<PaymentDTO>> {
        if (!requireOrderAccess(authentication, orderId)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(paymentService.getPaymentsDTOByOrderId(orderId))
    }

    @PostMapping
    fun createPayment(authentication: Authentication, @RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentDTO> {
        requireCurrentUser(authentication, request.ownerId)
        if (!requireOrderAccess(authentication, request.orderId)) {
            return ResponseEntity.notFound().build()
        }
        val created = paymentService.createPaymentDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/{id}/process")
    fun processPayment(authentication: Authentication, @PathVariable id: String): ResponseEntity<PaymentDTO> {
        if (!requirePaymentOwner(authentication, id)) {
            return ResponseEntity.notFound().build()
        }
        val updated = paymentService.processPaymentDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/complete")
    fun completePayment(authentication: Authentication, @PathVariable id: String): ResponseEntity<PaymentDTO> {
        if (!requirePaymentOwner(authentication, id)) {
            return ResponseEntity.notFound().build()
        }
        val updated = paymentService.completePaymentDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== INVOICES ====================

    @GetMapping("/invoices")
    fun getAllInvoices(authentication: Authentication): ResponseEntity<List<InvoiceDTO>> {
        val user = currentUser(authentication)
        val invoices = paymentService.getAllInvoicesDTO()
            .filter { invoice -> canAccessOrder(user, invoice.orderId) }
        return ResponseEntity.ok(invoices)
    }

    @GetMapping("/invoices/{id}")
    fun getInvoiceById(authentication: Authentication, @PathVariable id: String): ResponseEntity<InvoiceDTO> {
        val invoice = paymentService.getInvoiceDTOById(id)
        return if (invoice != null) {
            if (!requireOrderAccess(authentication, invoice.orderId)) {
                return ResponseEntity.notFound().build()
            }
            ResponseEntity.ok(invoice)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/invoices/order/{orderId}")
    fun getInvoiceByOrder(authentication: Authentication, @PathVariable orderId: String): ResponseEntity<InvoiceDTO> {
        if (!requireOrderAccess(authentication, orderId)) {
            return ResponseEntity.notFound().build()
        }
        val invoice = paymentService.getInvoiceDTOByOrderId(orderId)
        return if (invoice != null) {
            ResponseEntity.ok(invoice)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/invoices")
    fun createInvoice(authentication: Authentication, @RequestBody request: CreateInvoiceRequest): ResponseEntity<InvoiceDTO> {
        if (!requireOrderAccess(authentication, request.orderId)) {
            return ResponseEntity.notFound().build()
        }
        val created = paymentService.createInvoiceDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // ==================== PAYMENT METHODS ====================

    @GetMapping("/methods/owner/{ownerId}")
    fun getPaymentMethods(authentication: Authentication, @PathVariable ownerId: String): ResponseEntity<List<PaymentMethodDTO>> {
        requireCurrentUser(authentication, ownerId)
        return ResponseEntity.ok(paymentService.getPaymentMethodsDTOByOwner(ownerId))
    }

    @PostMapping("/methods")
    fun addPaymentMethod(authentication: Authentication, @RequestBody request: CreatePaymentMethodRequest): ResponseEntity<PaymentMethodDTO> {
        requireCurrentUser(authentication, request.ownerId)
        val created = paymentService.addPaymentMethodDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @DeleteMapping("/methods/{id}")
    fun deletePaymentMethod(authentication: Authentication, @PathVariable id: String): ResponseEntity<Void> {
        if (!requirePaymentMethodOwner(authentication, id)) {
            return ResponseEntity.notFound().build()
        }
        return if (paymentService.deletePaymentMethod(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/methods/{id}/default")
    fun setDefaultPaymentMethod(authentication: Authentication, @PathVariable id: String): ResponseEntity<PaymentMethodDTO> {
        if (!requirePaymentMethodOwner(authentication, id)) {
            return ResponseEntity.notFound().build()
        }
        val updated = paymentService.setDefaultPaymentMethodDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    private fun currentUser(authentication: Authentication): User =
        userService.getUserByEmail(authentication.name)
            ?: throw AccessDeniedException("Access denied")

    private fun currentUserId(authentication: Authentication): String =
        currentUser(authentication).id
            ?: throw AccessDeniedException("Access denied")

    private fun requireCurrentUser(authentication: Authentication, ownerId: String) {
        if (currentUserId(authentication) != ownerId) {
            throw AccessDeniedException("Cannot access another user's payment methods")
        }
    }

    private fun requirePaymentMethodOwner(authentication: Authentication, paymentMethodId: String): Boolean {
        val method = paymentService.getPaymentMethodById(paymentMethodId)
            ?: return false
        requireCurrentUser(authentication, method.ownerId)
        return true
    }

    private fun requirePaymentOwner(authentication: Authentication, paymentId: String): Boolean {
        val payment = paymentService.getPaymentById(paymentId)
            ?: return false
        requireCurrentUser(authentication, payment.ownerId)
        return true
    }

    private fun requirePaymentAccess(authentication: Authentication, payment: PaymentDTO) {
        val user = currentUser(authentication)
        val allowed = payment.ownerId == user.id || canAccessOrder(user, payment.orderId)
        if (!allowed) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireOrderAccess(authentication: Authentication, orderId: String): Boolean {
        val user = currentUser(authentication)
        val allowed = canAccessOrder(user, orderId)
        if (!allowed && orderService.getOrderDTOById(orderId) != null) {
            throw AccessDeniedException("Access denied")
        }
        return allowed
    }

    private fun canAccessOrder(user: User, orderId: String): Boolean {
        val order = orderService.getOrderDTOById(orderId) ?: return false
        return when (user.role) {
            UserRole.RETAILER -> order.retailerId == user.id
            UserRole.WHOLESALER -> order.wholesalerId == user.id
        }
    }
}
