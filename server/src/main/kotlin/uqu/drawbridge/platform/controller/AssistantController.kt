package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import uqu.drawbridge.platform.model.AssistantRequest
import uqu.drawbridge.platform.model.AssistantResponse
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.service.OrderService
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/assistant")
class AssistantController(
    private val orderService: OrderService,
    private val userRepository: UserRepository
) {

    @PostMapping
    fun handleAssistant(
        @RequestBody request: AssistantRequest,
        authentication: Authentication
    ): AssistantResponse {

        val email = authentication.name

        val user = userRepository.findByEmail(email)
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session")

        val userId = user.id
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User record has no ID")

        fun statusEmoji(status: Any?): String = when (status?.toString()) {
            "DELIVERED" -> "✅"
            "CANCELLED" -> "❌"
            "SHIPPED" -> "🚚"
            "PENDING" -> "⏳"
            "PROCESSING" -> "🔄"
            else -> "ℹ️"
        }

        fun statusLabel(status: Any?): String = when (status?.toString()) {
            "DELIVERED" -> "Delivered"
            "CANCELLED" -> "Cancelled"
            "SHIPPED" -> "Shipped"
            "PENDING" -> "Pending"
            "PROCESSING" -> "Processing"
            else -> status?.toString() ?: "Unknown"
        }

        fun formatDate(raw: String): String {
            return try {
                val dt = LocalDateTime.parse(raw)
                "${dt.dayOfMonth}/${dt.monthValue}/${dt.year}"
            } catch (_: Exception) {
                raw
            }
        }

        fun money(value: Double): String = "SAR ${"%.2f".format(value)}"

        val reply = when (request.intent) {

            "track_order" -> {
                // ✅ هذا يجيب كل الأوردرز حق اليوزر (مو آخر واحد)
                val orders = orderService.getOrdersDTOByRetailer(userId)

                if (orders.isEmpty()) {
                    "📭 No orders were found for your account."
                } else {

                    // الأحدث أول (placedAt عندك ISO String غالباً)
                    val sorted = orders.sortedByDescending { it.placedAt }

                    val sb = StringBuilder()
                    sb.append("🧾 Your orders summary\n\n")

                    var grandTotal = 0.0

                    sorted.forEachIndexed { index, o ->
                        val categories = o.items
                            .map { it.productCategory }
                            .filter { it.isNotBlank() && it != "Unknown" }
                            .distinct()

                        val categoriesText = if (categories.isEmpty()) "—" else categories.joinToString(", ")

                        grandTotal += o.subtotal

                        sb.append("Order ${index + 1}\n")
                        sb.append("🆔 ID: #${o.id}\n")
                        sb.append("📅 Date: ${formatDate(o.placedAt)}\n")
                        sb.append("🏷️ Category: $categoriesText\n")
                        sb.append("💰 Total: ${money(o.subtotal)}\n")
                        sb.append("📌 Status: ${statusEmoji(o.status)} ${statusLabel(o.status)}\n")
                        sb.append("\n")
                    }

                    if (sorted.size > 1) {
                        sb.append("🧮 Grand Total: ${money(grandTotal)}")
                    }

                    sb.toString().trim()
                }
            }

            "refund" ->
                "💸 Refund request\n" +
                        "1) Go to Orders\n" +
                        "2) Open the order\n" +
                        "3) Select the item\n" +
                        "4) Click Request Refund\n\n" +
                        "If the option is unavailable, the order might be outside the refund window."

            "technical_issue" ->
                "🛠️ Technical troubleshooting\n" +
                        "1) Refresh the page\n" +
                        "2) Clear browser cache\n" +
                        "3) Try again\n\n" +
                        "If the issue persists, I can escalate it to support."

            "pricing" ->
                "💳 Pricing & subscription\n" +
                        "Go to Settings > Payments to review your plan, invoices, and billing details."

            else ->
                "📨 Got it.\n" +
                        "I’ve forwarded your request to our support team. You’ll receive an update as soon as possible."
        }

        return AssistantResponse(reply)
    }
}