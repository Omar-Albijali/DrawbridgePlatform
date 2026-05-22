package uqu.drawbridge.platform.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uqu.drawbridge.platform.MobileApiConfig
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppPageHeader
import uqu.drawbridge.platform.ui.components.DeferredFeatureCard
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.model.AppDestination
import uqu.drawbridge.platform.ui.model.MoreDestination
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.theme.AppNavySurfaceHigh

@Composable
internal fun HomeMainScreen(
    session: SessionState,
    onOpenDashboard: () -> Unit,
    onLogout: () -> Unit,
) {
    ScreenSection(
        title = "Hello, ${session.user.name.ifBlank { session.user.email }}",
        subtitle = "Your operational workspace is ready.",
    ) {
        AppCard {
            Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(session.user.role.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            
            Text("Company", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(session.user.company.ifBlank { "Not provided" }, style = MaterialTheme.typography.titleMedium)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                PrimaryButton(text = "Open Dashboard", onClick = onOpenDashboard)
                SecondaryButton(text = "Logout", onClick = onLogout)
            }
        }
    }
}

@Composable
internal fun AccountMainScreen(
    onLogout: () -> Unit,
) {
    ScreenSection(
        title = "Account Settings",
        subtitle = "Control app behavior and active backend environment.",
    ) {
        AppCard {
            Text("Connection Status",
                style = MaterialTheme.typography.titleMedium,
                color= MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "API: ${MobileApiConfig.baseUrl}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            SecondaryButton(text = "Sign Out", onClick = onLogout)
        }
    }
}

@Composable
internal fun FeaturePlaceholderMainScreen(
    destination: AppDestination,
) {
    val (title, subtitle, body) = when (destination) {
        AppDestination.Marketplace -> Triple(
            "Marketplace",
            "Product discovery, filters, and wishlist actions.",
            "Phase 3 will connect the shared product DTOs and API client to native marketplace cards, search, filters, pagination, product detail, and wishlist actions.",
        )
        AppDestination.Cart -> Triple(
            "Cart",
            "MOQ-aware cart and checkout foundation.",
            "Phase 4 will add cart lines, quantity validation, checkout, order creation, and success or failure states with the existing shared cart and order DTOs.",
        )
        AppDestination.Orders -> Triple(
            "Orders",
            "Mobile order tracking for the active role.",
            "Phase 4 will add order lists, detail screens, status cards, tracking metadata, and role-appropriate actions.",
        )
        AppDestination.Inventory -> Triple(
            "Inventory",
            "Stock health and reorder controls.",
            "Phase 5 will add retailer inventory cards, low-stock indicators, detail views, quantity updates, and auto-restock controls.",
        )
        AppDestination.Products -> Triple(
            "Products",
            "Wholesaler catalog management.",
            "Phase 5 will add product list, create/edit forms, validation, publish state, and image upload when the platform picker and backend upload flow are connected.",
        )
        else -> Triple(
            destination.name,
            "Native workflow foundation.",
            "This destination is reserved in the role-aware mobile navigation and will be implemented in a later feature phase.",
        )
    }

    ScreenSection(title = title, subtitle = subtitle) {
        DeferredFeatureCard(destination = destination, title = "$title is mapped", message = body)
        EmptyStateCard(
            title = "No local data loaded yet",
            message = "The foundation is in place without duplicating web-only contracts or iOS-only business logic.",
        )
    }
}

@Composable
internal fun MoreMainScreen(
    destinations: List<MoreDestination>,
    onOpenDestination: (AppDestination) -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MoreHeaderCard()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            destinations.chunked(2).forEach { rowDestinations ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowDestinations.forEach { destination ->
                        MoreMenuCard(
                            destination = destination,
                            onClick = { onOpenDestination(destination.destination) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowDestinations.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            MoreLogoutButton(onClick = onLogout)
        }
    }
}

@Composable
private fun MoreHeaderCard() {
    AppPageHeader(
        title = "More",
        subtitle = "Account, tools, and support",
    )
}

@Composable
private fun MoreMenuCard(
    destination: MoreDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = moreDestinationTint(destination.destination)
    Card(
        onClick = onClick,
        modifier = modifier.height(154.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppNavySurfaceHigh.copy(alpha = 0.92f)),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(tint.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(destination.icon, contentDescription = null, tint = tint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Text(
                            text = destination.description,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreLogoutButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = "Logout",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun moreDestinationTint(destination: AppDestination): Color {
    return when (destination) {
        AppDestination.Account -> Color(0xFF10B981)
        AppDestination.Settings -> Color(0xFF93C5FD)
        AppDestination.Wishlist -> Color(0xFFFF5A8A)
        AppDestination.Notifications -> Color(0xFFFFB020)
        AppDestination.POS -> Color(0xFF60A5FA)
        AppDestination.Reports -> Color(0xFF7DD3FC)
        AppDestination.Support -> Color(0xFFC084FC)
        AppDestination.Inventory -> Color(0xFF34D399)
        else -> Color(0xFFA8B7C7)
    }
}

@Composable
internal fun MoreDestinationScreen(
    destination: AppDestination,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    wishlistContent: @Composable () -> Unit,
    posContent: @Composable () -> Unit,
    supportContent: @Composable () -> Unit,
    notificationsContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    inventoryContent: @Composable () -> Unit,
) {
    when (destination) {
        AppDestination.Inventory -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                inventoryContent()
            }
        }
        AppDestination.Wishlist -> {
            wishlistContent()
        }
        AppDestination.POS -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                posContent()
            }
        }
        AppDestination.Account -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                AccountMainScreen(onLogout = onLogout)
            }
        }
        AppDestination.Support -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                supportContent()
            }
        }
        AppDestination.Notifications -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                notificationsContent()
            }
        }
        AppDestination.Settings -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                settingsContent()
            }
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                FeaturePlaceholderMainScreen(destination = destination)
            }
        }
    }
}
