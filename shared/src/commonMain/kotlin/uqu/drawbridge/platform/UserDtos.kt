package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlinx.serialization.Serializable

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class UserRole {
    RETAILER,
    WHOLESALER
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class AddressDto(
    val id: String? = null,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class RepresentativeDto(
    val name: String,
    val jobTitle: String,
    val phoneNumber: String,
    val email: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
enum class VerificationStatus {
    VERIFIED,
    PENDING,
    UNVERIFIED
}

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class UserDTO(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val company: String,
    val phone: String?,
    val addresses: Array<AddressDto>?,
    val representative: RepresentativeDto?,
    val commercialRegister: String?,
    val verificationStatus: VerificationStatus?,
    val avatar: String?
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val phoneNumber: String,
    val role: UserRole,
    val businessName: String? = null,
    val commercialRegistrationNumber: String,
    val repName: String,
    val repJobTitle: String,
    val repPhoneNumber: String,
    val repEmail: String,
    val addresses: Array<AddressDto> // List -> Array
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: UserRole
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class VerifyEmailRequest(
    val token: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ResendVerificationRequest(
    val email: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class LogoutRequest(
    val token: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class AddressResponseDto(
    val id: String,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class CreateAddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class UpdateUserProfileRequest(
    val company: String,
    val phone: String?,
    val commercialRegister: String?,
    val representative: RepresentativeDto
)

@OptIn(ExperimentalJsExport::class)
@JsExport
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
