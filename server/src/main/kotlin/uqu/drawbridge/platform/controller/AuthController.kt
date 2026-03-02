package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.AuthResponse
import uqu.drawbridge.platform.LoginRequest
import uqu.drawbridge.platform.RegisterRequest
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
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

    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody body: Map<String, String>): ResponseEntity<Void> {
        val email = body["email"] ?: return ResponseEntity.badRequest().build()
        // Trigger reset flow; always return 200 to prevent email enumeration
        userService.initiateForgotPassword(email)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody body: Map<String, String>): ResponseEntity<Void> {
        val token = body["token"] ?: return ResponseEntity.badRequest().build()
        val newPassword = body["newPassword"] ?: return ResponseEntity.badRequest().build()
        return try {
            userService.resetPassword(token, newPassword)
            ResponseEntity.ok().build()
        } catch (_: IllegalArgumentException) {
            ResponseEntity.status(400).build()
        }
    }
}
