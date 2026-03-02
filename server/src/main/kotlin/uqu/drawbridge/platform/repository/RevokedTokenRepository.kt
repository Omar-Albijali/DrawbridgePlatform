package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.RevokedToken

interface RevokedTokenRepository : JpaRepository<RevokedToken, String>

