package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Address

interface AddressRepository : JpaRepository<Address, String> {
    fun findByUserId(userId: String): List<Address>
}
