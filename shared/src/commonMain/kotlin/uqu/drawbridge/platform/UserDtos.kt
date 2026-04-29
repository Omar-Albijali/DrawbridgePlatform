@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package uqu.drawbridge.platform


import kotlin.js.JsExport

@JsExport
enum class UserRole {
    RETAILER,
    WHOLESALER
}

@JsExport
data class AddressDto(
    val id: String? = null,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@JsExport
data class RepresentativeDto(
    val name: String,
    val jobTitle: String,
    val phoneNumber: String,
    val email: String
)

@JsExport
enum class VerificationStatus {
    VERIFIED,
    PENDING,
    UNVERIFIED
}

@JsExport
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

@JsExport
data class LoginRequest(
    val email: String,
    val password: String,
    val rememberMe: Boolean = false
)

@JsExport
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

@JsExport
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: UserRole
)

@JsExport
data class ForgotPasswordRequest(
    val email: String
)

@JsExport
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

@JsExport
data class VerifyEmailRequest(
    val token: String
)

@JsExport
data class ResendVerificationRequest(
    val email: String
)

@JsExport
data class LogoutRequest(
    val token: String
)

@JsExport
data class AddressResponseDto(
    val id: String,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@JsExport
data class CreateAddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@JsExport
data class UpdateUserProfileRequest(
    val company: String,
    val phone: String?,
    val commercialRegister: String?,
    val representative: RepresentativeDto
)

@JsExport
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
