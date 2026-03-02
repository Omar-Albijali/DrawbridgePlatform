package uqu.drawbridge.platform.service

import io.mailtrap.config.MailtrapConfig
import io.mailtrap.factory.MailtrapClientFactory
import io.mailtrap.model.request.emails.Address
import io.mailtrap.model.request.emails.MailtrapMail
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class EmailService(
    private val templateEngine: TemplateEngine,
    @Value("\${app.base-url}") private val baseUrl: String,
    @Value("\${mailtrap.api-token}") private val apiToken: String,
    @Value("\${mailtrap.sandbox}") private val sandbox: Boolean,
    @Value("\${mailtrap.inbox-id}") private val inboxId: Long
) {
    private val fromEmail = "no-reply@uqu-drawbridge.com"
    private val fromName = "Drawbridge"

    private val client by lazy {
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

        // SDK throws on failure (InvalidRequestBodyException, HttpClientException, etc.)
        if (sandbox && inboxId > 0) {
            client.testingApi().emails().send(mail, inboxId)
        } else {
            client.sendingApi().emails().send(mail)
        }
    }
}

