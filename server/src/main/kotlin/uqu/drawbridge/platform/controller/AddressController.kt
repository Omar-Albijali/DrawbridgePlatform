package uqu.drawbridge.platform.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import uqu.drawbridge.platform.AddressResponseDto
import uqu.drawbridge.platform.CreateAddressRequest
import uqu.drawbridge.platform.service.AddressService
import uqu.drawbridge.platform.service.UserService

@RestController
@RequestMapping("/api/addresses")
class AddressController(
    private val addressService: AddressService,
    private val userService: UserService
) {

    private fun getUserId(authentication: Authentication): String {
        val email = authentication.name
        val user = userService.getUserByEmail(email)
            ?: throw IllegalArgumentException("User not found")
        return user.id!!
    }

    @GetMapping
    fun getAddresses(authentication: Authentication): ResponseEntity<List<AddressResponseDto>> {
        val userId = getUserId(authentication)
        return ResponseEntity.ok(addressService.getAddresses(userId))
    }

    @PostMapping
    fun addAddress(authentication: Authentication, @RequestBody request: CreateAddressRequest): ResponseEntity<AddressResponseDto> {
        val userId = getUserId(authentication)
        return ResponseEntity.ok(addressService.addAddress(userId, request))
    }

    @PutMapping("/{id}")
    fun updateAddress(
        authentication: Authentication,
        @PathVariable id: String,
        @RequestBody request: CreateAddressRequest
    ): ResponseEntity<AddressResponseDto> {
        val userId = getUserId(authentication)
        return ResponseEntity.ok(addressService.updateAddress(userId, id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteAddress(
        authentication: Authentication,
        @PathVariable id: String
    ): ResponseEntity<Void> {
        val userId = getUserId(authentication)
        addressService.deleteAddress(userId, id)
        return ResponseEntity.noContent().build()
    }
}
