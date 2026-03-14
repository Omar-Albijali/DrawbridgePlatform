package uqu.drawbridge.platform.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uqu.drawbridge.platform.DashboardSummary
import uqu.drawbridge.platform.MobileApiConfig
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatCard
import uqu.drawbridge.platform.ui.model.SessionState

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
internal fun DashboardMainScreen(
    dashboardSummary: DashboardSummary?,
    onRefresh: () -> Unit,
) {
    ScreenSection(
        title = "Dashboard",
        subtitle = "Track order flow and business volume.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${dashboardSummary?.totalOrders ?: 0}",
                label = "Orders",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${dashboardSummary?.pendingOrders ?: 0}",
                label = "Pending",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${dashboardSummary?.processingOrders ?: 0}",
                label = "Processing",
            )
        }
        AppCard {
            Text(
                "Total Value",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "SAR ${dashboardSummary?.totalAmount ?: 0.0}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            PrimaryButton(text = "Refresh Data", onClick = onRefresh)
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
