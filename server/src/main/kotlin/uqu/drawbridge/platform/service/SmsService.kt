package uqu.drawbridge.platform.service

import com.twilio.Twilio
import com.twilio.rest.api.v2010.account.Message
import com.twilio.type.PhoneNumber
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SmsService(
    @Value("\${twilio.account-sid}") private val accountSid: String,
    @Value("\${twilio.auth-token}") private val authToken: String,
    @Value("\${twilio.from-number}") private val fromNumber: String
) {
    private val log = LoggerFactory.getLogger(SmsService::class.java)

    private fun isConfigured(): Boolean =
        accountSid.isNotBlank() && authToken.isNotBlank() && fromNumber.isNotBlank()

    fun sendNotificationSms(toNumber: String, title: String, message: String): Boolean {
        if (!isConfigured() || toNumber.isBlank()) {
            return false
        }

        return try {
            Twilio.init(accountSid, authToken)
            Message.creator(
                PhoneNumber(toNumber),
                PhoneNumber(fromNumber),
                "$title\n$message"
            ).create()
            true
        } catch (ex: Exception) {
            log.warn("Failed to send SMS notification to {}", toNumber, ex)
            false
        }
    }
}
