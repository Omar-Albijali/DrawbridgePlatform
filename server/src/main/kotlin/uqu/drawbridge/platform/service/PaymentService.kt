package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.model.*
import uqu.drawbridge.platform.repository.InvoiceRepository
import uqu.drawbridge.platform.repository.PaymentMethodRepository
import uqu.drawbridge.platform.repository.PaymentRepository
import uqu.drawbridge.platform.*
import uqu.drawbridge.platform.validation.RequestValidation
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentMethodRepository: PaymentMethodRepository,
    private val notificationService: NotificationService
) {

    // ==================== PAYMENT OPERATIONS ====================

    fun getAllPayments(): List<Payment> {
        return paymentRepository.findAll()
    }

    fun getPaymentById(id: String): Payment? {
        return paymentRepository.findById(id).orElse(null)
    }

    fun getPaymentsByOrderId(orderId: String): List<Payment> {
        return paymentRepository.findByOrderId(orderId)
    }

    fun getPaymentsByOwnerId(ownerId: String): List<Payment> {
        return paymentRepository.findByOwnerId(ownerId)
    }

    fun getPaymentsByStatus(status: PaymentStatus): List<Payment> {
        return paymentRepository.findByStatus(status)
    }

    fun getPaymentByTransactionRef(transactionRef: String): Payment? {
        return paymentRepository.findByTransactionRef(transactionRef)
    }

    @Transactional
    fun createPayment(payment: Payment): Payment {
        payment.transactionRef = generateTransactionRef()
        return paymentRepository.save(payment)
    }

    @Transactional
    fun updatePaymentStatus(id: String, status: PaymentStatus): Payment? {
        val payment = paymentRepository.findById(id).orElse(null) ?: return null
        payment.status = status
        if (status == PaymentStatus.COMPLETED) {
            payment.completedAt = LocalDateTime.now()
        }
        val savedPayment = paymentRepository.save(payment)

        notificationService.sendEventNotification(
            recipientId = savedPayment.ownerId,
            type = NotificationType.PAYMENT,
            eventKey = NotificationEventKey.PAYMENT_STATUS_UPDATED,
            entityType = NotificationEntityType.PAYMENT,
            entityId = savedPayment.id,
            preferenceKey = NotificationPreferenceKey.PAYMENT_STATUS,
            title = "Payment status updated",
            message = "Payment ${savedPayment.id ?: ""} is now ${savedPayment.status.name}.",
            deepLink = "/settings/payments"
        )

        return savedPayment
    }

    @Transactional
    fun processPayment(id: String): Payment? {
        return updatePaymentStatus(id, PaymentStatus.PROCESSING)
    }

    @Transactional
    fun completePayment(id: String): Payment? {
        return updatePaymentStatus(id, PaymentStatus.COMPLETED)
    }

    @Transactional
    fun refundPayment(id: String): Payment? {
        return updatePaymentStatus(id, PaymentStatus.REFUNDED)
    }

    @Transactional
    fun cancelPayment(id: String): Payment? {
        return updatePaymentStatus(id, PaymentStatus.CANCELLED)
    }

    // ==================== INVOICE OPERATIONS ====================

    fun getAllInvoices(): List<Invoice> {
        return invoiceRepository.findAll()
    }

    fun getInvoiceById(id: String): Invoice? {
        return invoiceRepository.findById(id).orElse(null)
    }

    fun getInvoiceByOrderId(orderId: String): Invoice? {
        return invoiceRepository.findByOrderId(orderId)
    }

    fun getInvoiceByNumber(invoiceNumber: String): Invoice? {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
    }

    fun getOverdueInvoices(): List<Invoice> {
        return invoiceRepository.findByDueDateBefore(LocalDateTime.now())
    }

    fun getInvoicesInDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<Invoice> {
        return invoiceRepository.findByIssueDateBetween(startDate, endDate)
    }

    @Transactional
    fun createInvoice(invoice: Invoice): Invoice {
        return invoiceRepository.save(invoice)
    }

    @Transactional
    fun createInvoiceForOrder(orderId: String, totalAmount: BigDecimal, currency: String, dueDays: Int = 30): Invoice {
        val invoice = Invoice(
            orderId = orderId,
            invoiceNumber = generateInvoiceNumber(),
            issueDate = LocalDateTime.now(),
            dueDate = LocalDateTime.now().plusDays(dueDays.toLong()),
            totalAmount = totalAmount,
            currency = currency
        )
        return invoiceRepository.save(invoice)
    }

    @Transactional
    fun deleteInvoice(id: String): Boolean {
        return if (invoiceRepository.existsById(id)) {
            invoiceRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== PAYMENT METHOD OPERATIONS ====================

    fun getPaymentMethodById(id: String): PaymentMethod? {
        return paymentMethodRepository.findById(id).orElse(null)
    }

    fun getPaymentMethodsByOwner(ownerId: String): List<PaymentMethod> {
        return paymentMethodRepository.findByOwnerId(ownerId)
    }

    fun getPaymentMethodsByOwnerAndType(ownerId: String, type: PaymentMethodType): List<PaymentMethod> {
        return paymentMethodRepository.findByOwnerIdAndType(ownerId, type)
    }

    fun getDefaultPaymentMethod(ownerId: String): PaymentMethod? {
        return paymentMethodRepository.findByOwnerIdAndIsDefaultTrue(ownerId)
    }

    @Transactional
    fun addPaymentMethod(paymentMethod: PaymentMethod): PaymentMethod {
        // If this is the first payment method for the owner, make it default
        val existingMethods = paymentMethodRepository.findByOwnerId(paymentMethod.ownerId)
        if (existingMethods.isEmpty()) {
            paymentMethod.isDefault = true
        }
        return paymentMethodRepository.save(paymentMethod)
    }

    @Transactional
    fun setDefaultPaymentMethod(id: String): PaymentMethod? {
        val method = paymentMethodRepository.findById(id).orElse(null) ?: return null
        
        // Remove default from other methods
        val currentDefault = paymentMethodRepository.findByOwnerIdAndIsDefaultTrue(method.ownerId)
        if (currentDefault != null && currentDefault.id != id) {
            currentDefault.isDefault = false
            paymentMethodRepository.save(currentDefault)
        }
        
        method.isDefault = true
        return paymentMethodRepository.save(method)
    }

    @Transactional
    fun deletePaymentMethod(id: String): Boolean {
        return if (paymentMethodRepository.existsById(id)) {
            paymentMethodRepository.deleteById(id)
            true
        } else {
            false
        }
    }

    // ==================== HELPER METHODS ====================

    private fun generateTransactionRef(): String {
        return "TXN-${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
    }

    private fun generateInvoiceNumber(): String {
        val year = LocalDateTime.now().year
        val random = UUID.randomUUID().toString().substring(0, 6).uppercase()
        return "INV-$year-$random"
    }

    // ==================== DTO MAPPING ====================

    fun Payment.toDTO() = PaymentDTO(
        id = (this.id ?: ""),
        orderId = this.orderId,
        ownerId = this.ownerId,
        paymentMethodId = this.paymentMethodId,
        amount = this.amount.toPlainString(),
        status = this.status,
        transactionRef = this.transactionRef,
        completedAt = this.completedAt.toString()
    )

    fun Invoice.toDTO() = InvoiceDTO(
        id = (this.id ?: ""),
        orderId = this.orderId,
        invoiceNumber = this.invoiceNumber,
        issueDate = this.issueDate.toString(),
        dueDate = this.dueDate.toString(),
        totalAmount = this.totalAmount.toPlainString(),
        currency = this.currency
    )

    fun PaymentMethod.toDTO() = PaymentMethodDTO(
        id = (this.id ?: ""),
        ownerId = this.ownerId,
        type = this.type,
        maskedDetails = this.maskedDetails,
        isDefault = this.isDefault
    )

    // ==================== DTO-RETURNING METHODS ====================

    fun getAllPaymentsDTO(): List<PaymentDTO> = getAllPayments().map { it.toDTO() }

    fun getPaymentDTOById(id: String): PaymentDTO? = getPaymentById(id)?.toDTO()

    fun getPaymentsDTOByOrderId(orderId: String): List<PaymentDTO> = getPaymentsByOrderId(orderId).map { it.toDTO() }

    fun createPaymentDTO(request: CreatePaymentRequest): PaymentDTO {
        RequestValidation.requireNotBlank(request.orderId, "orderId")
        RequestValidation.requireNotBlank(request.ownerId, "ownerId")
        RequestValidation.requireNotBlank(request.paymentMethodId, "paymentMethodId")
        val amount = RequestValidation.parsePositiveBigDecimal(request.amount, "amount")
        val payment = Payment(
            orderId = request.orderId,
            ownerId = request.ownerId,
            paymentMethodId = request.paymentMethodId,
            amount = amount,
            status = PaymentStatus.PENDING,
            transactionRef = request.transactionRef,
            completedAt = LocalDateTime.now()
        )
        return createPayment(payment).toDTO()
    }

    fun processPaymentDTO(id: String): PaymentDTO? = processPayment(id)?.toDTO()

    fun completePaymentDTO(id: String): PaymentDTO? = completePayment(id)?.toDTO()

    fun getAllInvoicesDTO(): List<InvoiceDTO> = getAllInvoices().map { it.toDTO() }

    fun getInvoiceDTOById(id: String): InvoiceDTO? = getInvoiceById(id)?.toDTO()

    fun getInvoiceDTOByOrderId(orderId: String): InvoiceDTO? = getInvoiceByOrderId(orderId)?.toDTO()

    fun createInvoiceDTO(request: CreateInvoiceRequest): InvoiceDTO {
        RequestValidation.requireNotBlank(request.orderId, "orderId")
        RequestValidation.requireNotBlank(request.invoiceNumber, "invoiceNumber")
        RequestValidation.requireNotBlank(request.currency, "currency")
        val totalAmount = RequestValidation.parsePositiveBigDecimal(request.totalAmount, "totalAmount")
        val invoice = Invoice(
            orderId = request.orderId,
            invoiceNumber = request.invoiceNumber,
            issueDate = LocalDateTime.parse(request.issueDate),
            dueDate = LocalDateTime.parse(request.dueDate),
            totalAmount = totalAmount,
            currency = request.currency
        )
        return createInvoice(invoice).toDTO()
    }

    fun getPaymentMethodsDTOByOwner(ownerId: String): List<PaymentMethodDTO> =  
        getPaymentMethodsByOwner(ownerId).map { it.toDTO() }

    fun addPaymentMethodDTO(request: CreatePaymentMethodRequest): PaymentMethodDTO {
        RequestValidation.requireNotBlank(request.ownerId, "ownerId")
        RequestValidation.requireNotBlank(request.type, "type")
        RequestValidation.requireNotBlank(request.maskedDetails, "maskedDetails")
        val paymentMethod = PaymentMethod(
            ownerId = request.ownerId,
            type = PaymentMethodType.valueOf(request.type),
            maskedDetails = request.maskedDetails,
            isDefault = request.isDefault
        )
        return addPaymentMethod(paymentMethod).toDTO()
    }

    fun setDefaultPaymentMethodDTO(id: String): PaymentMethodDTO? = setDefaultPaymentMethod(id)?.toDTO()
}
