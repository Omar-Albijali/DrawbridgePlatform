package uqu.drawbridge.platform.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.ChangePasswordRequest
import uqu.drawbridge.platform.UserDTO
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: String): ResponseEntity<UserDTO> {
        val userDto = userService.getUserDTOById(id)
        return if (userDto != null) {
            ResponseEntity.ok(userDto)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}")
    fun updateUser(
        @PathVariable id: String,
        @RequestBody request: uqu.drawbridge.platform.UpdateUserProfileRequest
    ): ResponseEntity<UserDTO> {
        val updatedUser = userService.updateUser(id, request)
        return if (updatedUser != null) {
            ResponseEntity.ok(updatedUser)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PatchMapping("/{id}/password")
    fun changePassword(
        @PathVariable id: String,
        @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        return try {
            val success = userService.changePassword(id, request)
            if (success) ResponseEntity.noContent().build()
            else ResponseEntity.notFound().build()
        } catch (_: uqu.drawbridge.platform.exception.InvalidCredentialsException) {
            ResponseEntity.status(403).build()
        }
    }
}
