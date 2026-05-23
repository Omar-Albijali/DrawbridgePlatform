package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Invoice
import java.time.LocalDateTime

interface InvoiceRepository : JpaRepository<Invoice, String> {
    fun findByOrder_Id(orderId: String): Invoice?
    fun findByInvoiceNumber(invoiceNumber: String): Invoice?
    fun findByDueDateBefore(dueDate: LocalDateTime): List<Invoice>
    fun findByIssueDateBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Invoice>
}
