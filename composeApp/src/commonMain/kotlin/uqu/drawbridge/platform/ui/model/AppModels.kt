package uqu.drawbridge.platform.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import uqu.drawbridge.platform.UserDTO
import uqu.drawbridge.platform.UserRole

internal enum class AuthScreen(val title: String) {
    Welcome("Welcome"),
    Login("Sign In"),
    Signup("Create Account"),
}

internal enum class MainTab(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Home),
    Account("Account", Icons.Default.Person),
}

internal data class SessionState(
    val token: String,
    val user: UserDTO,
)

internal data class SignupFormState(
    val role: UserRole = UserRole.RETAILER,
    val companyName: String = "",
    val businessEmail: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val commercialRegistration: String = "",
    val repName: String = "",
    val repJobTitle: String = "",
    val repPhone: String = "",
    val repEmail: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "Saudi Arabia",
)
