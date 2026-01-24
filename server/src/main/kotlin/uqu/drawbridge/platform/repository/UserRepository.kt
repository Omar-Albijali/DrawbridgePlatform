package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.User

interface UserRepository : JpaRepository<User, String> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}
