package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.AuthResponse
import uqu.drawbridge.platform.LoginRequest
import uqu.drawbridge.platform.RegisterRequest
import uqu.drawbridge.platform.ForgotPasswordRequest
import uqu.drawbridge.platform.ResetPasswordRequest
import uqu.drawbridge.platform.VerifyEmailRequest
import uqu.drawbridge.platform.ResendVerificationRequest
import uqu.drawbridge.platform.LogoutRequest
import uqu.drawbridge.platform.security.JwtService
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService,
    private val jwtService: JwtService
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val response = userService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        val response = userService.login(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest, authentication: Authentication): ResponseEntity<Void> {
        val tokenUser = jwtService.extractUsername(request.token) ?: return ResponseEntity.badRequest().build()
        if (tokenUser != authentication.name) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        return try {
            jwtService.revokeToken(request.token)
            ResponseEntity.ok().build()
        } catch (_: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        // Trigger reset flow; always return 200 to prevent email enumeration
        userService.initiateForgotPassword(request.email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        return try {
            userService.resetPassword(request.token, request.newPassword)
            ResponseEntity.ok().build()
        } catch (_: IllegalArgumentException) {
            ResponseEntity.status(400).build()
        }
    }

    @PostMapping("/verify-email")
    fun verifyEmail(@RequestBody request: VerifyEmailRequest): ResponseEntity<Void> {
        return try {
            userService.verifyEmail(request.token)
            ResponseEntity.ok().build()
        } catch (_: IllegalArgumentException) {
            ResponseEntity.status(400).build()
        }
    }

    @PostMapping("/resend-verification")
    fun resendVerification(@RequestBody request: ResendVerificationRequest): ResponseEntity<Void> {
        userService.resendEmailVerification(request.email)
        return ResponseEntity.ok().build()
    }
}
