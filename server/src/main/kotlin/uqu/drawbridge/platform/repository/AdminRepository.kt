package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Admin

interface AdminRepository : JpaRepository<Admin, String> {
    fun findByEmail(email: String): Admin?
    fun existsByEmail(email: String): Boolean
}
