package uqu.drawbridge.platform.service

import org.springframework.stereotype.Service
import uqu.drawbridge.platform.AddressResponseDto
import uqu.drawbridge.platform.CreateAddressRequest
import uqu.drawbridge.platform.model.Address
import uqu.drawbridge.platform.repository.AddressRepository
import uqu.drawbridge.platform.repository.UserRepository

@Service
class AddressService(
    private val addressRepository: AddressRepository,
    private val userRepository: UserRepository
) {
    fun getAddresses(userId: String): List<AddressResponseDto> {
        return addressRepository.findByUserId(userId).map {
            AddressResponseDto(it.id!!, it.street, it.city, it.state, it.zipCode, it.country)
        }
    }

    fun addAddress(userId: String, request: CreateAddressRequest): AddressResponseDto {
        val user = userRepository.findById(userId).orElseThrow { IllegalArgumentException("User not found") }
        val address = Address(
            street = request.street,
            city = request.city,
            state = request.state,
            zipCode = request.zipCode,
            country = request.country
        )
        user.addresses.add(address)
        userRepository.save(user)
        
        // Find the saved address to return it (it should be in the list)
        val saved = user.addresses.last()
        return AddressResponseDto(saved.id!!, saved.street, saved.city, saved.state, saved.zipCode, saved.country)
    }

    fun updateAddress(userId: String, addressId: String, request: CreateAddressRequest): AddressResponseDto {
        val address = addressRepository.findById(addressId).orElseThrow { IllegalArgumentException("Address not found") }
        
        // Security check: ensure address belongs to user
        if (address.userId != userId) {
            throw IllegalArgumentException("Unauthorized access to address")
        }

        address.street = request.street
        address.city = request.city
        address.state = request.state
        address.zipCode = request.zipCode
        address.country = request.country
        
        val saved = addressRepository.save(address)
        return AddressResponseDto(saved.id!!, saved.street, saved.city, saved.state, saved.zipCode, saved.country)
    }

    fun deleteAddress(userId: String, addressId: String) {
        val address = addressRepository.findById(addressId).orElseThrow { IllegalArgumentException("Address not found") }
        
        // Security check
        if (address.userId != userId) {
            throw IllegalArgumentException("Unauthorized access to address")
        }
        
        addressRepository.delete(address)
    }
}
