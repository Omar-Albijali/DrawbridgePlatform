package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.service.PaymentService

@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val paymentService: PaymentService
) {

    // ==================== PAYMENTS ====================

    @GetMapping
    fun getAllPayments(): ResponseEntity<List<PaymentDTO>> {
        return ResponseEntity.ok(paymentService.getAllPaymentsDTO())
    }

    @GetMapping("/{id}")
    fun getPaymentById(@PathVariable id: String): ResponseEntity<PaymentDTO> {
        val payment = paymentService.getPaymentDTOById(id)
        return if (payment != null) {
            ResponseEntity.ok(payment)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/order/{orderId}")
    fun getPaymentsByOrder(@PathVariable orderId: String): ResponseEntity<List<PaymentDTO>> {
        return ResponseEntity.ok(paymentService.getPaymentsDTOByOrderId(orderId))
    }

    @PostMapping
    fun createPayment(@RequestBody request: CreatePaymentRequest): ResponseEntity<PaymentDTO> {
        val created = paymentService.createPaymentDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PostMapping("/{id}/process")
    fun processPayment(@PathVariable id: String): ResponseEntity<PaymentDTO> {
        val updated = paymentService.processPaymentDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/{id}/complete")
    fun completePayment(@PathVariable id: String): ResponseEntity<PaymentDTO> {
        val updated = paymentService.completePaymentDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    // ==================== INVOICES ====================

    @GetMapping("/invoices")
    fun getAllInvoices(): ResponseEntity<List<InvoiceDTO>> {
        return ResponseEntity.ok(paymentService.getAllInvoicesDTO())
    }

    @GetMapping("/invoices/{id}")
    fun getInvoiceById(@PathVariable id: String): ResponseEntity<InvoiceDTO> {
        val invoice = paymentService.getInvoiceDTOById(id)
        return if (invoice != null) {
            ResponseEntity.ok(invoice)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/invoices/order/{orderId}")
    fun getInvoiceByOrder(@PathVariable orderId: String): ResponseEntity<InvoiceDTO> {
        val invoice = paymentService.getInvoiceDTOByOrderId(orderId)
        return if (invoice != null) {
            ResponseEntity.ok(invoice)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/invoices")
    fun createInvoice(@RequestBody request: CreateInvoiceRequest): ResponseEntity<InvoiceDTO> {
        val created = paymentService.createInvoiceDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    // ==================== PAYMENT METHODS ====================

    @GetMapping("/methods/owner/{ownerId}")
    fun getPaymentMethods(@PathVariable ownerId: String): ResponseEntity<List<PaymentMethodDTO>> {
        return ResponseEntity.ok(paymentService.getPaymentMethodsDTOByOwner(ownerId))
    }

    @PostMapping("/methods")
    fun addPaymentMethod(@RequestBody request: CreatePaymentMethodRequest): ResponseEntity<PaymentMethodDTO> {
        val created = paymentService.addPaymentMethodDTO(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @DeleteMapping("/methods/{id}")
    fun deletePaymentMethod(@PathVariable id: String): ResponseEntity<Void> {
        return if (paymentService.deletePaymentMethod(id)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/methods/{id}/default")
    fun setDefaultPaymentMethod(@PathVariable id: String): ResponseEntity<PaymentMethodDTO> {
        val updated = paymentService.setDefaultPaymentMethodDTO(id)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
