package uqu.drawbridge.platform.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import uqu.drawbridge.platform.ErrorResponse
import uqu.drawbridge.platform.service.PosApiKeyService

@Component
class PosApiKeyAuthenticationFilter(
    private val posApiKeyService: PosApiKeyService
) : OncePerRequestFilter() {

    companion object {
        const val RETAILER_ID_ATTR = "posRetailerId"
        const val INTEGRATION_ID_ATTR = "posIntegrationId"
    }

    private val objectMapper = ObjectMapper().findAndRegisterModules()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath ?: return true
        return !path.startsWith("/api/pos/")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val apiKey = request.getHeader("X-API-Key")
        if (apiKey.isNullOrBlank()) {
            unauthorized(response, "Missing API key")
            return
        }

        val integration = posApiKeyService.authenticate(apiKey)
        if (integration == null) {
            unauthorized(response, "Invalid API key")
            return
        }

        request.setAttribute(RETAILER_ID_ATTR, integration.retailerId)
        request.setAttribute(INTEGRATION_ID_ATTR, integration.id)

        val auth = UsernamePasswordAuthenticationToken(
            "pos:${integration.retailerId}",
            null,
            listOf(SimpleGrantedAuthority("ROLE_POS"))
        )
        SecurityContextHolder.getContext().authentication = auth

        filterChain.doFilter(request, response)
    }

    private fun unauthorized(response: HttpServletResponse, message: String) {
        SecurityContextHolder.clearContext()
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.writer.write(objectMapper.writeValueAsString(ErrorResponse(message, 401)))
    }
}
