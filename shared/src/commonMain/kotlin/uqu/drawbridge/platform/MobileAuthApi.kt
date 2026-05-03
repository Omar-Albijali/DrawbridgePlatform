package uqu.drawbridge.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MobileApiException(
    message: String,
    val statusCode: Int? = null,
) : Exception(message)

data class MobileSession(
    val token: String,
    val user: UserDTO,
)

data class DashboardSummary(
    val totalOrders: Int,
    val pendingOrders: Int,
    val processingOrders: Int,
    val totalAmount: Double,
)

class MobileAuthApi(
    private val client: HttpClient = createMobileHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun login(email: String, password: String, rememberMe: Boolean): MobileSession {
        val response = client.post(buildUrl("/auth/login")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password, rememberMe = rememberMe))
        }
        ensureSuccess(response.status, response.bodyAsText())

        val authResponse = json.decodeFromString(AuthResponsePayload.serializer(), response.bodyAsText()).toAuthResponse()
        val user = fetchUserById(authResponse.userId, authResponse.token)
        return MobileSession(token = authResponse.token, user = user)
    }

    suspend fun register(request: RegisterRequest): MobileSession {
        val response = client.post(buildUrl("/auth/register")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(request.toPayload())
        }
        ensureSuccess(response.status, response.bodyAsText())

        val authResponse = json.decodeFromString(AuthResponsePayload.serializer(), response.bodyAsText()).toAuthResponse()
        val user = fetchUserById(authResponse.userId, authResponse.token)
        return MobileSession(token = authResponse.token, user = user)
    }

    suspend fun fetchDashboardSummary(userId: String, role: UserRole, token: String): DashboardSummary {
        val endpoint = if (role == UserRole.WHOLESALER) {
            "/orders/wholesaler/$userId"
        } else {
            "/orders/retailer/$userId"
        }

        val response = client.get(buildUrl(endpoint)) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())

        val items = json.parseToJsonElement(response.bodyAsText()).jsonArray
        var pending = 0
        var processing = 0
        var total = 0.0

        items.forEach { item ->
            val obj = item.jsonObject
            val statusToken = obj.stringValue("status")
            val subtotal = obj.doubleValue("subtotal")
            if (statusToken == OrderStatus.PENDING.name) {
                pending += 1
            }
            if (statusToken == OrderStatus.PROCESSING.name) {
                processing += 1
            }
            total += subtotal
        }

        return DashboardSummary(
            totalOrders = items.size,
            pendingOrders = pending,
            processingOrders = processing,
            totalAmount = total,
        )
    }

    suspend fun fetchUserById(userId: String, token: String): UserDTO {
        val response = client.get(buildUrl("/users/$userId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
        val payload = json.decodeFromString(UserPayload.serializer(), response.bodyAsText())
        return payload.toUserDto()
    }

    suspend fun scanBarcode(retailerId: String, gtin: String, token: String): PosScanResponse {
        val response = client.post(buildUrl("/inventory/scan")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(PosScanRequest(retailerId = retailerId, gtin = gtin))
        }

        val body = response.bodyAsText()
        val parsed = json.parseToJsonElement(body).jsonObject
        return PosScanResponse(
            productName = parsed.stringValue("productName"),
            newStock = parsed["newStock"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            message = parsed.stringValue("message"),
        )
    }

    private fun buildUrl(path: String): String = "${MobileApiConfig.baseUrl}${path}"

    private fun ensureSuccess(status: HttpStatusCode, body: String) {
        if (status.isSuccess()) {
            return
        }

        val message = runCatching {
            json.parseToJsonElement(body)
                .jsonObject["message"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()

        throw MobileApiException(
            message = message ?: "Request failed with status ${status.value}",
            statusCode = status.value,
        )
    }

    private fun RegisterRequest.toPayload(): RegisterRequestPayload {
        return RegisterRequestPayload(
            email = email,
            password = password,
            phoneNumber = phoneNumber,
            role = role.name,
            businessName = businessName,
            commercialRegistrationNumber = commercialRegistrationNumber,
            repName = repName,
            repJobTitle = repJobTitle,
            repPhoneNumber = repPhoneNumber,
            repEmail = repEmail,
            addresses = addresses.map { address ->
                AddressPayload(
                    id = address.id,
                    street = address.street,
                    city = address.city,
                    state = address.state,
                    zipCode = address.zipCode,
                    country = address.country,
                )
            },
        )
    }

}

@Serializable
private data class AddressPayload(
    val id: String? = null,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

@Serializable
private data class RegisterRequestPayload(
    val email: String,
    val password: String,
    val phoneNumber: String,
    val role: String,
    val businessName: String? = null,
    val commercialRegistrationNumber: String,
    val repName: String,
    val repJobTitle: String,
    val repPhoneNumber: String,
    val repEmail: String,
    val addresses: List<AddressPayload>,
)

@Serializable
private data class AuthResponsePayload(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: String,
) {
    fun toAuthResponse(): AuthResponse {
        return AuthResponse(
            token = token,
            userId = userId,
            email = email,
            name = name,
            role = enumValueOfOrDefault(role, UserRole.RETAILER),
        )
    }
}

@Serializable
private data class UserPayload(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val company: String,
    val phone: String? = null,
    val addresses: List<AddressPayload>? = null,
    val representative: RepresentativePayload? = null,
    val commercialRegister: String? = null,
    val verificationStatus: String? = null,
    val avatar: String? = null,
) {
    fun toUserDto(): UserDTO {
        return UserDTO(
            id = id,
            name = name,
            email = email,
            role = enumValueOfOrDefault(role, UserRole.RETAILER),
            company = company,
            phone = phone,
            addresses = addresses?.map { addr ->
                AddressDto(
                    id = addr.id,
                    street = addr.street,
                    city = addr.city,
                    state = addr.state,
                    zipCode = addr.zipCode,
                    country = addr.country,
                )
            }?.toTypedArray(),
            representative = representative?.toDto(),
            commercialRegister = commercialRegister,
            verificationStatus = verificationStatus?.let { enumValueOfOrDefault(it, VerificationStatus.PENDING) },
            avatar = avatar,
        )
    }
}

@Serializable
private data class RepresentativePayload(
    val name: String,
    val jobTitle: String,
    val phoneNumber: String,
    val email: String,
) {
    fun toDto(): RepresentativeDto {
        return RepresentativeDto(
            name = name,
            jobTitle = jobTitle,
            phoneNumber = phoneNumber,
            email = email,
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(token: String, default: T): T {
    return runCatching { enumValueOf<T>(token) }.getOrDefault(default)
}

private fun createMobileHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }
}

private fun JsonObject.stringValue(key: String): String {
    val element = this[key] ?: return ""
    return when (element) {
        is JsonPrimitive -> element.content
        else -> element.toString()
    }
}

private fun JsonObject.doubleValue(key: String): Double {
    val element = this[key] ?: return 0.0
    return when (element) {
        is JsonPrimitive -> element.doubleOrNull ?: 0.0
        else -> 0.0
    }
}
