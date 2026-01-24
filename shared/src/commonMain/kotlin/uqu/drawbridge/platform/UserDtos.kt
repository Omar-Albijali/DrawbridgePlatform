package uqu.drawbridge.platform

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class UserRole {
    RETAILER,
    WHOLESALER
}

@OptIn(ExperimentalJsExport::class)
@JsExport
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
data class RepresentativeDto(
    val name: String,
    val jobTitle: String,
    val phoneNumber: String,
    val email: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
enum class VerificationStatus {
    VERIFIED,
    PENDING,
    UNVERIFIED
}

@OptIn(ExperimentalJsExport::class)
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

@OptIn(ExperimentalJsExport::class)
@JsExport
data class LoginRequest(
    val email: String,
    val password: String
)

@OptIn(ExperimentalJsExport::class)
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

@OptIn(ExperimentalJsExport::class)
@JsExport
data class AuthResponse(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: UserRole
)

@OptIn(ExperimentalJsExport::class)
@JsExport
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
data class CreateAddressRequest(
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String
)

@OptIn(ExperimentalJsExport::class)
@JsExport
data class UpdateUserProfileRequest(
    val company: String,
    val phone: String?,
    val commercialRegister: String?,
    val representative: RepresentativeDto
)
