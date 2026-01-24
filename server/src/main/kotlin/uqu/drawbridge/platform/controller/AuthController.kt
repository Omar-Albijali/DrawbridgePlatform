package uqu.drawbridge.platform.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.AuthResponse
import uqu.drawbridge.platform.ErrorResponse
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
}
