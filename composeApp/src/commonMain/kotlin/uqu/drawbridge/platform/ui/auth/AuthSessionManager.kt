package uqu.drawbridge.platform.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uqu.drawbridge.platform.DashboardSummary
import uqu.drawbridge.platform.MobileApiException
import uqu.drawbridge.platform.MobileAuthApi
import uqu.drawbridge.platform.RegisterRequest
import uqu.drawbridge.platform.ui.common.userReadableMessage
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.platform.SecureTokenStorage

internal sealed interface AuthSessionState {
    data object RestoringSession : AuthSessionState
    data object Unauthenticated : AuthSessionState
    data class Loading(val message: String) : AuthSessionState
    data class Authenticated(
        val session: SessionState,
        val dashboardSummary: DashboardSummary? = null,
    ) : AuthSessionState
    data class SessionExpired(val message: String) : AuthSessionState
    data class Error(
        val message: String,
        val canRetryRestore: Boolean = false,
    ) : AuthSessionState
}

internal data class AuthActionResult(
    val success: Boolean,
    val message: String? = null,
)

internal class AuthSessionManager(
    private val secureTokenStorage: SecureTokenStorage,
) {
    private var activeToken: String? = null

    var state: AuthSessionState by mutableStateOf(AuthSessionState.RestoringSession)
        private set

    val api: MobileAuthApi = MobileAuthApi(
        tokenProvider = { activeToken ?: secureTokenStorage.readToken() },
        onUnauthorized = { expireSession() },
    )

    suspend fun restoreSession() {
        state = AuthSessionState.RestoringSession
        val token = secureTokenStorage.readToken()?.takeIf { it.isNotBlank() }
        if (token == null) {
            activeToken = null
            state = AuthSessionState.Unauthenticated
            return
        }

        val userId = token.extractUserId()
        if (userId == null) {
            clearStoredSession()
            state = AuthSessionState.SessionExpired("Your session expired. Please sign in again.")
            return
        }

        activeToken = token
        runCatching { api.fetchUserById(userId) }
            .onSuccess { user ->
                state = AuthSessionState.Authenticated(SessionState(user = user))
            }
            .onFailure { error ->
                handleRestoreFailure(error)
            }
    }

    suspend fun login(email: String, password: String, rememberMe: Boolean): AuthActionResult {
        val validationError = validateLogin(email, password)
        if (validationError != null) {
            state = AuthSessionState.Error(validationError)
            return AuthActionResult(success = false, message = validationError)
        }

        state = AuthSessionState.Loading("Signing in...")
        return runCatching {
            val mobileSession = api.login(email.trim(), password, rememberMe)
            activeToken = mobileSession.token
            if (rememberMe) {
                secureTokenStorage.saveToken(mobileSession.token)
            } else {
                secureTokenStorage.clearToken()
            }
            mobileSession
        }
            .fold(
                onSuccess = { mobileSession ->
                    state = AuthSessionState.Authenticated(
                        SessionState(user = mobileSession.user),
                    )
                    AuthActionResult(success = true)
                },
                onFailure = { error ->
                    val message = userReadableMessage(error, fallback = "Invalid email or password")
                    activeToken = null
                    state = AuthSessionState.Error(message)
                    AuthActionResult(success = false, message = message)
                },
            )
    }

    suspend fun register(request: RegisterRequest): AuthActionResult {
        state = AuthSessionState.Loading("Creating account...")
        return runCatching {
            val mobileSession = api.register(request)
            activeToken = mobileSession.token
            secureTokenStorage.saveToken(mobileSession.token)
            mobileSession
        }
            .fold(
                onSuccess = { mobileSession ->
                    state = AuthSessionState.Authenticated(
                        SessionState(user = mobileSession.user),
                    )
                    AuthActionResult(success = true)
                },
                onFailure = { error ->
                    val message = userReadableMessage(error, fallback = "Registration failed")
                    activeToken = null
                    state = AuthSessionState.Error(message)
                    AuthActionResult(success = false, message = message)
                },
            )
    }

    suspend fun logout() {
        val hadToken = activeToken != null || secureTokenStorage.readToken() != null
        state = AuthSessionState.Loading("Signing out...")
        if (hadToken) {
            runCatching { api.logout() }
        }
        clearStoredSession()
        state = AuthSessionState.Unauthenticated
    }

    fun setDashboardSummary(summary: DashboardSummary?) {
        val current = state as? AuthSessionState.Authenticated ?: return
        state = current.copy(dashboardSummary = summary)
    }

    fun clearAuthMessage() {
        if (state is AuthSessionState.Error || state is AuthSessionState.SessionExpired) {
            state = AuthSessionState.Unauthenticated
        }
    }

    private suspend fun expireSession() {
        clearStoredSession()
        state = AuthSessionState.SessionExpired("Your session expired. Please sign in again.")
    }

    private suspend fun clearStoredSession() {
        activeToken = null
        secureTokenStorage.clearToken()
    }

    private suspend fun handleRestoreFailure(error: Throwable) {
        if ((error as? MobileApiException)?.isUnauthorized == true) {
            expireSession()
            return
        }

        state = AuthSessionState.Error(
            message = userReadableMessage(error, fallback = "Could not restore your session. Check your connection and try again."),
            canRetryRestore = true,
        )
    }

    private fun validateLogin(email: String, password: String): String? {
        return when {
            email.isBlank() -> "Enter your business email."
            "@" !in email -> "Enter a valid business email."
            password.isBlank() -> "Enter your password."
            else -> null
        }
    }

}

@OptIn(ExperimentalEncodingApi::class)
private fun String.extractUserId(): String? {
    val payload = split(".").getOrNull(1) ?: return null
    val paddedPayload = payload.padEnd(payload.length + (4 - payload.length % 4) % 4, '=')
    val decoded = runCatching {
        Base64.UrlSafe.decode(paddedPayload).decodeToString()
    }.getOrNull() ?: return null

    return runCatching {
        Json.parseToJsonElement(decoded)
            .jsonObject["userId"]
            ?.jsonPrimitive
            ?.content
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
