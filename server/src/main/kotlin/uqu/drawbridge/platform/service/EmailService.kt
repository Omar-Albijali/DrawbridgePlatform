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

@Service
class EmailService(
    private val templateEngine: TemplateEngine,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${mailtrap.api-token:}") private val apiToken: String,
    @Value("\${mailtrap.sandbox:false}") private val sandbox: Boolean,
    @Value("\${mailtrap.inbox-id:0}") private val inboxId: Long,
    @Value("\${mailtrap.from-email:no-reply@uqu-drawbridge.com}") private val fromEmail: String,
    @Value("\${mailtrap.skip-after-error-ms:60000}") private val skipAfterErrorMs: Long
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)

//    private val fromEmail = "no-reply@uqu-drawbridge.com"
    private val fromName = "Drawbridge"
    @Volatile
    private var skipUntilMs: Long = 0

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
        if (apiToken.isBlank()) {
            log.debug("Skipping Mailtrap email send because mailtrap.api-token is not configured.")
            return
        }
        val now = System.currentTimeMillis()
        if (now < skipUntilMs) {
            log.debug("Skipping Mailtrap email send because the service is in fail-soft mode.")
            return
        }

        runCatching {
            client.send(mail)
        }.onFailure { ex ->
            skipUntilMs = System.currentTimeMillis() + skipAfterErrorMs.coerceAtLeast(0)
            log.warn("Mailtrap email delivery failed; skipping Mailtrap sends temporarily.", ex)
        }
    }
}
