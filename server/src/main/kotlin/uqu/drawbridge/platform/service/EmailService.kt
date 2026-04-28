package uqu.drawbridge.platform.service

import io.mailtrap.client.MailtrapClient
import io.mailtrap.config.MailtrapConfig
import io.mailtrap.factory.MailtrapClientFactory
import io.mailtrap.model.request.emails.Address
import io.mailtrap.model.request.emails.MailtrapMail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import kotlin.math.max

@Service
class EmailService(
    private val templateEngine: TemplateEngine,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${mailtrap.api-token}") private val apiToken: String,
    @Value("\${mailtrap.sandbox}") private val sandbox: Boolean,
    @Value("\${mailtrap.inbox-id}") private val inboxId: Long,
    @Value("\${mailtrap.max-emails-per-second:1}") private val maxEmailsPerSecond: Int,
    @Value("\${mailtrap.max-retries:4}") private val maxRetries: Int,
    @Value("\${mailtrap.retry-base-delay-ms:1200}") private val retryBaseDelayMs: Long
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

    private val fromEmail = "no-reply@uqu-drawbridge.com"
    private val fromName = "Drawbridge"
    private val sendLock = Any()
    @Volatile
    private var lastSendAtMs: Long = 0

    private val minSendIntervalMs: Long = run {
        val safeRate = max(1, maxEmailsPerSecond)
        max(1L, 1000L / safeRate)
    }

    private val client: MailtrapClient by lazy {
        val configBuilder = MailtrapConfig.Builder().token(apiToken)
        if (sandbox && inboxId > 0) {
            configBuilder.sandbox(true).inboxId(inboxId)
        }
        MailtrapClientFactory.createMailtrapClient(configBuilder.build())
    }

    fun sendPasswordResetEmail(toEmail: String, recipientName: String, resetToken: String) {
        val resetUrl = "$baseUrl/reset-password?token=$resetToken"

        val ctx = Context().apply {
            setVariable("name", recipientName)
            setVariable("resetUrl", resetUrl)
        }

        val htmlBody = templateEngine.process("email/password-reset", ctx)

        val mail = MailtrapMail.builder()
            .from(Address(fromEmail, fromName))
            .to(listOf(Address(toEmail, recipientName)))
            .subject("Reset your Drawbridge password")
            .html(htmlBody)
            .build()

        sendMailWithRateLimitHandling(mail)
    }

    fun sendEmailVerificationEmail(toEmail: String, recipientName: String, verificationToken: String) {
        val verifyUrl = "$baseUrl/verify-email?token=$verificationToken"

        val ctx = Context().apply {
            setVariable("name", recipientName)
            setVariable("verifyUrl", verifyUrl)
        }

        val htmlBody = templateEngine.process("email/verify-email", ctx)

        val mail = MailtrapMail.builder()
            .from(Address(fromEmail, fromName))
            .to(listOf(Address(toEmail, recipientName)))
            .subject("Verify your Drawbridge email")
            .html(htmlBody)
            .build()

        sendMailWithRateLimitHandling(mail)
    }

    fun sendNotificationEmail(toEmail: String, recipientName: String, subject: String, title: String, message: String) {
        val safeName = recipientName.ifBlank { "there" }
        val htmlBody = """
            <html>
              <body style=\"font-family: Arial, sans-serif; color: #0f172a;\">
                <p>Hello $safeName,</p>
                <h2 style=\"margin-bottom: 8px;\">$title</h2>
                <p style=\"line-height: 1.5;\">$message</p>
                <p style=\"margin-top: 20px; color: #64748b;\">You can view this update in your Drawbridge notifications inbox.</p>
              </body>
            </html>
        """.trimIndent()

        val mail = MailtrapMail.builder()
            .from(Address(fromEmail, fromName))
            .to(listOf(Address(toEmail, safeName)))
            .subject(subject)
            .html(htmlBody)
            .build()

        sendMailWithRateLimitHandling(mail)
    }

    fun sendSupportTicketEmail(
        toEmail: String,
        ticketNumber: String,
        subject: String,
        category: String,
        description: String,
        userEmail: String,
        userId: String,
        attachmentUrl: String?
    ) {
        val safeAttachmentUrl = attachmentUrl ?: "No attachment provided"
        val htmlBody = """
            <html>
              <body style="font-family: Arial, sans-serif; color: #0f172a;">
                <h2 style="margin-bottom: 12px;">New support ticket received</h2>
                <table style="border-collapse: collapse; width: 100%;">
                  <tr><td style="padding: 8px; font-weight: 700;">Ticket Number</td><td style="padding: 8px;">$ticketNumber</td></tr>
                  <tr><td style="padding: 8px; font-weight: 700;">Subject</td><td style="padding: 8px;">$subject</td></tr>
                  <tr><td style="padding: 8px; font-weight: 700;">Category</td><td style="padding: 8px;">$category</td></tr>
                  <tr><td style="padding: 8px; font-weight: 700;">User Email</td><td style="padding: 8px;">$userEmail</td></tr>
                  <tr><td style="padding: 8px; font-weight: 700;">User ID</td><td style="padding: 8px;">$userId</td></tr>
                  <tr><td style="padding: 8px; font-weight: 700;">Attachment</td><td style="padding: 8px;">$safeAttachmentUrl</td></tr>
                </table>
                <div style="margin-top: 16px;">
                  <p style="margin-bottom: 8px; font-weight: 700;">Description</p>
                  <p style="white-space: pre-wrap; line-height: 1.6;">$description</p>
                </div>
              </body>
            </html>
        """.trimIndent()

        val mail = MailtrapMail.builder()
            .from(Address(fromEmail, fromName))
            .to(listOf(Address(toEmail, "Drawbridge Support")))
            .subject("New support ticket $ticketNumber")
            .html(htmlBody)
            .build()

        sendMailWithRateLimitHandling(mail)
    }

    private fun sendMailWithRateLimitHandling(mail: MailtrapMail) {
        repeat(maxRetries + 1) { attempt ->
            try {
                waitForSendWindow()
                client.send(mail)
                return
            } catch (ex: Exception) {
                val canRetry = attempt < maxRetries && isMailtrapRateLimitError(ex)
                if (!canRetry) {
                    throw ex
                }

                val delayMs = computeRetryDelayMs(attempt)
                log.warn(
                    "Mailtrap rate limit hit (attempt {}/{}). Retrying in {} ms.",
                    attempt + 1,
                    maxRetries + 1,
                    delayMs
                )
                Thread.sleep(delayMs)
            }
        }
    }

    private fun waitForSendWindow() {
        synchronized(sendLock) {
            val now = System.currentTimeMillis()
            val waitMs = minSendIntervalMs - (now - lastSendAtMs)
            if (waitMs > 0) {
                Thread.sleep(waitMs)
            }
            lastSendAtMs = System.currentTimeMillis()
        }
    }

    private fun computeRetryDelayMs(attempt: Int): Long {
        val multiplier = 1L shl attempt.coerceAtMost(10)
        return retryBaseDelayMs.coerceAtLeast(200L) * multiplier
    }

    private fun isMailtrapRateLimitError(ex: Exception): Boolean {
        var current: Throwable? = ex
        while (current != null) {
            val msg = current.message.orEmpty().lowercase()
            if (
                msg.contains("too many emails per second") ||
                msg.contains("too many requests") ||
                msg.contains("status code: 429") ||
                msg.contains("http 429")
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
