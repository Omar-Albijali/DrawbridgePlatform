package uqu.drawbridge.platform.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uqu.drawbridge.platform.AuthResponse
import uqu.drawbridge.platform.LoginRequest
import uqu.drawbridge.platform.RegisterRequest
import uqu.drawbridge.platform.model.Address
import uqu.drawbridge.platform.model.PasswordResetToken
import uqu.drawbridge.platform.model.Representative
import uqu.drawbridge.platform.model.User
import uqu.drawbridge.platform.repository.PasswordResetTokenRepository
import uqu.drawbridge.platform.repository.UserRepository
import uqu.drawbridge.platform.security.JwtService
import java.time.LocalDateTime
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val emailService: EmailService,
    private val passwordResetTokenRepository: PasswordResetTokenRepository
) {

    private fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun register(request: RegisterRequest): AuthResponse {
        val normalizedEmail = normalizeEmail(request.email)

        // Check if email already exists (case-insensitive)
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw IllegalArgumentException("Email already registered")
        }

        // Create new user
        // Create new user using apply for cleaner initialization
        val user = User(
            email = normalizedEmail,
            passwordHash = requireNotNull(passwordEncoder.encode(request.password)) {
                "Password encoding failed during registration"
            },
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
        val user = userRepository.findByEmail(normalizeEmail(request.email))
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
        return userRepository.findByEmail(normalizeEmail(email))
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

    fun changePassword(id: String, request: uqu.drawbridge.platform.ChangePasswordRequest): Boolean {
        val user = getUserById(id) ?: return false

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw uqu.drawbridge.platform.exception.InvalidCredentialsException("Current password is incorrect")
        }

        user.passwordHash = requireNotNull(passwordEncoder.encode(request.newPassword)) {
            "Password encoding failed; password was not changed"
        }
        userRepository.save(user)
        return true
    }

    @Transactional
    fun initiateForgotPassword(email: String) {
        val user = userRepository.findByEmail(normalizeEmail(email)) ?: return

        // Invalidate any existing tokens for this user
        passwordResetTokenRepository.deleteAllByUserId(user.id!!)

        // Create a new 1-hour token
        val token = PasswordResetToken(
            token = UUID.randomUUID().toString(),
            userId = user.id!!,
            expiresAt = LocalDateTime.now().plusHours(1)
        )
        passwordResetTokenRepository.save(token)

        emailService.sendPasswordResetEmail(
            toEmail = user.email,
            recipientName = user.representative.name,
            resetToken = token.token
        )
    }

    @Transactional
    fun resetPassword(token: String, newPassword: String) {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw IllegalArgumentException("Invalid or expired password reset token")

        if (resetToken.used) {
            throw IllegalArgumentException("This reset link has already been used")
        }
        if (resetToken.expiresAt.isBefore(LocalDateTime.now())) {
            throw IllegalArgumentException("This reset link has expired")
        }

        val user = getUserById(resetToken.userId)
            ?: throw IllegalArgumentException("User not found")

        user.passwordHash = requireNotNull(passwordEncoder.encode(newPassword)) {
            "Password encoding failed; password was not changed"
        }
        userRepository.save(user)

        resetToken.used = true
        passwordResetTokenRepository.save(resetToken)
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
