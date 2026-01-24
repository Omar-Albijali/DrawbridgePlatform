package uqu.drawbridge.platform.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import uqu.drawbridge.platform.AuthResponse
import uqu.drawbridge.platform.LoginRequest
import uqu.drawbridge.platform.RegisterRequest
import uqu.drawbridge.platform.model.Address
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.security.JwtService

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): AuthResponse {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        // Create new user
        // Create new user using apply for cleaner initialization
        val user = User(
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password) ?: "", // Handle potential null safely
            phoneNumber = request.phoneNumber,
            role = request.role,
            businessName = request.businessName ?: "",
            avatar = "",
            commercialRegistrationNumber = request.commercialRegistrationNumber,
            verificationStatus = false
        ).apply {
            representative = Representative(
                name = request.repName,
                jobTitle = request.repJobTitle,
                phoneNumber = request.repPhoneNumber,
                email = request.repEmail
            )
            addresses = request.addresses.map { addrDto ->
                Address(
                    street = addrDto.street,
                    city = addrDto.city,
                    state = addrDto.state,
                    zipCode = addrDto.zipCode,
                    country = addrDto.country
                )
            }.toMutableList()
        }


        val savedUser = userRepository.save(user)

        // Generate JWT token
        val token = jwtService.generateToken(savedUser)

        return AuthResponse(
            token = token,
            userId = savedUser.id!!,
            email = savedUser.email,
            name = savedUser.representative.name,
            role = savedUser.role
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw uqu.drawbridge.platform.exception.InvalidCredentialsException("Invalid email or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw uqu.drawbridge.platform.exception.InvalidCredentialsException("Invalid email or password")
        }

        val token = jwtService.generateToken(user)

        return AuthResponse(
            token = token,
            userId = user.id!!,
            email = user.email,
            name = user.representative.name,
            role = user.role
        )
    }

    fun getUserById(id: String): User? {
        return userRepository.findById(id).orElse(null)
    }

    fun getUserByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }

    fun getUserDTOById(id: String): uqu.drawbridge.platform.UserDTO? {
        val user = getUserById(id) ?: return null
        return user.toDto()
    }

    fun updateUser(id: String, request: uqu.drawbridge.platform.UpdateUserProfileRequest): uqu.drawbridge.platform.UserDTO? {
        val user = getUserById(id) ?: return null

        user.businessName = request.company
        user.phoneNumber = request.phone ?: ""
        user.commercialRegistrationNumber = request.commercialRegister ?: ""

        val repDto = request.representative
        user.representative.name = repDto.name
        user.representative.jobTitle = repDto.jobTitle
        user.representative.phoneNumber = repDto.phoneNumber
        user.representative.email = repDto.email

        val savedUser = userRepository.save(user)
        return savedUser.toDto()
    }

    private fun User.toDto(): uqu.drawbridge.platform.UserDTO {
        return uqu.drawbridge.platform.UserDTO(
            id = this.id!!,
            name = this.representative.name,
            email = this.email,
            role = this.role,
            company = this.businessName,
            phone = this.phoneNumber,
            addresses = this.addresses.map { it.toDto() }.toTypedArray(),
            representative = this.representative.toDto(),
            commercialRegister = this.commercialRegistrationNumber,
            verificationStatus = if (this.verificationStatus) uqu.drawbridge.platform.VerificationStatus.VERIFIED else uqu.drawbridge.platform.VerificationStatus.UNVERIFIED,
            avatar = this.avatar
        )
    }

    private fun Address.toDto(): uqu.drawbridge.platform.AddressDto {
        return uqu.drawbridge.platform.AddressDto(
            id = this.id,
            street = this.street,
            city = this.city,
            state = this.state,
            zipCode = this.zipCode,
            country = this.country
        )
    }

    private fun Representative.toDto(): uqu.drawbridge.platform.RepresentativeDto {
        return uqu.drawbridge.platform.RepresentativeDto(
            name = this.name,
            jobTitle = this.jobTitle,
            phoneNumber = this.phoneNumber,
            email = this.email
        )
    }
}
