package uqu.drawbridge.platform.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
            MainTab(AppDestination.Inventory, "Inventory", Icons.Default.Menu),
            MainTab(AppDestination.Products, "Products", Icons.Default.ShoppingCart),
            MainTab(AppDestination.Orders, "Orders", Icons.Default.Menu),
            MainTab(AppDestination.More, "More", Icons.Default.Menu),
        )
        UserRole.RETAILER -> listOf(
            MainTab(AppDestination.Home, "Home", Icons.Default.Home),
            MainTab(AppDestination.Marketplace, "Market", Icons.Default.Search),
            MainTab(AppDestination.Cart, "Cart", Icons.Default.ShoppingCart),
            MainTab(AppDestination.Orders, "Orders", Icons.Default.Menu),
            MainTab(AppDestination.More, "More", Icons.Default.Menu),
        )
    }
}

internal fun moreDestinationsFor(role: UserRole): List<MoreDestination> {
    val common = listOf(
        MoreDestination(AppDestination.Notifications, "Notifications", "Inbox and read status", Icons.Default.Notifications),
        MoreDestination(AppDestination.Support, "Support", "Tickets and help requests", Icons.Default.Person),
        MoreDestination(AppDestination.Reports, "Reports", "Sales, orders, and inventory summaries", Icons.Default.Menu),
        MoreDestination(AppDestination.Settings, "Settings", "Profile, security, and app preferences", Icons.Default.Person),
    )

    return when (role) {
        UserRole.RETAILER -> listOf(
            MoreDestination(AppDestination.Wishlist, "Wishlist", "Saved marketplace products", Icons.Default.Favorite),
            MoreDestination(AppDestination.POS, "POS", "Barcode and manual GTIN tools", Icons.Default.Search),
        ) + common
        UserRole.WHOLESALER -> common
    }
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
