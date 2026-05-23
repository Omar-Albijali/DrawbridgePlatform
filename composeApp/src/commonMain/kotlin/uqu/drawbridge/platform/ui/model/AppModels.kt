package uqu.drawbridge.platform.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector
import uqu.drawbridge.platform.UserDTO
import uqu.drawbridge.platform.UserRole

internal enum class AuthScreen(val title: String) {
    Welcome("Welcome"),
    Login("Sign In"),
    Signup("Create Account"),
}

internal enum class AppDestination {
    Home,
    Marketplace,
    Cart,
    Orders,
    Inventory,
    Products,
    More,
    Account,
    Wishlist,
    Notifications,
    Support,
    Reports,
    Settings,
    POS,
}

internal data class MainTab(
    val destination: AppDestination,
    val title: String,
    val icon: ImageVector,
)

internal data class MoreDestination(
    val destination: AppDestination,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

internal fun primaryTabsFor(role: UserRole): List<MainTab> {
    return when (role) {
        UserRole.WHOLESALER -> listOf(
            MainTab(AppDestination.Home, "Home", Icons.Default.Home),
            MainTab(AppDestination.Marketplace, "Market", Icons.Default.Search),
            MainTab(AppDestination.Products, "Products", Icons.Default.Storefront),
            MainTab(AppDestination.Orders, "Orders", Icons.AutoMirrored.Filled.ListAlt),
            MainTab(AppDestination.More, "More", Icons.Default.Menu),
        )
        UserRole.RETAILER -> listOf(
            MainTab(AppDestination.Home, "Home", Icons.Default.Home),
            MainTab(AppDestination.Marketplace, "Market", Icons.Default.Search),
            MainTab(AppDestination.Inventory, "Inventory", Icons.Default.Inventory2),
            MainTab(AppDestination.Orders, "Orders", Icons.AutoMirrored.Filled.ListAlt),
            MainTab(AppDestination.More, "More", Icons.Default.Menu),
        )
    }
}

internal fun moreDestinationsFor(role: UserRole): List<MoreDestination> {
    val account = MoreDestination(AppDestination.Account, "Account", "Profile and preferences", Icons.Default.Person)
    val settings = MoreDestination(AppDestination.Settings, "Settings", "App configuration", Icons.Default.Settings)
    val notifications = MoreDestination(AppDestination.Notifications, "Notifications", "Alerts and updates", Icons.Default.Notifications)
    val support = MoreDestination(AppDestination.Support, "Support", "Help and tickets", Icons.Default.Person)

    return when (role) {
        UserRole.RETAILER -> listOf(
            account,
            settings,
            MoreDestination(AppDestination.Wishlist, "Wishlist", "Saved marketplace products", Icons.Default.Favorite),
            notifications,
            MoreDestination(AppDestination.POS, "POS", "External POS integration", Icons.Default.Storefront),
            support,
        )
        UserRole.WHOLESALER -> listOf(
            account,
            settings,
            notifications,
            support,
        )
    }
}

internal data class SessionState(
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
