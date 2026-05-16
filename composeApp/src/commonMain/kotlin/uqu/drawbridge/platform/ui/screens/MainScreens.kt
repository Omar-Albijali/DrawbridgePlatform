package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uqu.drawbridge.platform.DashboardSummary
import uqu.drawbridge.platform.MobileApiConfig
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.BarcodeScannerView
import uqu.drawbridge.platform.ui.components.DeferredFeatureCard
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatCard
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.model.AppDestination
import uqu.drawbridge.platform.ui.model.MoreDestination
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
    ScreenSection(
        title = "More",
        subtitle = "Role-based tools, preferences, and support.",
    ) {
        destinations.forEach { destination ->
            AppCard {
                ListItem(
                    headlineContent = {
                        Text(destination.title, fontWeight = FontWeight.Bold)
                    },
                    supportingContent = {
                        Text(destination.description)
                    },
                    leadingContent = {
                        Icon(destination.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        val isReady = destination.destination == AppDestination.POS || destination.destination == AppDestination.Wishlist
                        StatusChip(
                            text = if (isReady) "Ready" else "Queued",
                            tone = if (isReady) StatusTone.Success else StatusTone.Warning,
                        )
                    },
                )
                SecondaryButton(text = "Open", onClick = { onOpenDestination(destination.destination) })
            }
        }

        SecondaryButton(text = "Logout", onClick = onLogout)
    }
}

@Composable
internal fun MoreDestinationScreen(
    destination: AppDestination,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    wishlistContent: @Composable () -> Unit,
    posContent: @Composable () -> Unit,
) {
    when (destination) {
        AppDestination.Wishlist -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                wishlistContent()
            }
        }
        AppDestination.POS -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                posContent()
            }
        }
        AppDestination.Settings -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                AccountMainScreen(onLogout = onLogout)
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

// ==================== POS SCREEN ====================

data class ScanHistoryEntry(
    val gtin: String,
    val productName: String,
    val newStock: Int,
    val success: Boolean,
    val message: String,
)

@Composable
internal fun PosMainScreen(
    onScan: (gtin: String, onResult: (uqu.drawbridge.platform.PosScanResponse) -> Unit) -> Unit,
) {
    var barcodeInput by remember { mutableStateOf("") }
    var lastResult by remember { mutableStateOf<uqu.drawbridge.platform.PosScanResponse?>(null) }
    var scanHistory by remember { mutableStateOf(listOf<ScanHistoryEntry>()) }
    var isScanning by remember { mutableStateOf(false) }
    var showCameraScanner by remember { mutableStateOf(false) }

    // Helper to trigger scan
    fun triggerScan(gtin: String) {
        if (gtin.isBlank() || isScanning) return
        isScanning = true
        lastResult = null
        onScan(gtin.trim()) { response ->
            lastResult = response
            val success = response.message == "OK"
            scanHistory = listOf(
                ScanHistoryEntry(
                    gtin = gtin.trim(),
                    productName = response.productName,
                    newStock = response.newStock,
                    success = success,
                    message = response.message,
                )
            ) + scanHistory
            if (success) barcodeInput = ""
            isScanning = false
        }
    }

    ScreenSection(
        title = "POS Scanner",
        subtitle = "Scan a product barcode to reduce inventory by one.",
    ) {
        // --- Camera scanner ---
        AnimatedVisibility(visible = showCameraScanner) {
            BarcodeScannerView(
                onBarcodeScanned = { scannedValue ->
                    barcodeInput = scannedValue
                    showCameraScanner = false
                    triggerScan(scannedValue)
                },
                onClose = { showCameraScanner = false },
            )
        }

        // --- Barcode input ---
        AppCard {
            if (!showCameraScanner) {
                SecondaryButton(
                    text = "Scan with camera",
                    onClick = { showCameraScanner = true },
                )
            }

            AppTextField(
                value = barcodeInput,
                onValueChange = { barcodeInput = it },
                label = "Barcode / GTIN",
                keyboardType = KeyboardType.Number,
            )

            PrimaryButton(
                text = if (isScanning) "Scanning…" else "Scan",
                enabled = barcodeInput.isNotBlank() && !isScanning,
                onClick = { triggerScan(barcodeInput) },
            )
        }

        // --- Last scan result ---
        AnimatedVisibility(visible = lastResult != null) {
            lastResult?.let { result ->
                val isSuccess = result.message == "OK"
                AppCard {
                    StatusChip(
                        text = if (isSuccess) "Scan successful" else "Scan failed",
                        tone = if (isSuccess) StatusTone.Success else StatusTone.Error,
                    )
                    if (result.productName.isNotBlank()) {
                        Text(
                            text = result.productName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (isSuccess) {
                        Text(
                            text = "Remaining stock: ${result.newStock}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // --- Scan history ---
        if (scanHistory.isNotEmpty()) {
            Text(
                text = "Scan History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
            scanHistory.forEach { entry ->
                AppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.productName.ifBlank { entry.gtin },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (entry.success) "Stock: ${entry.newStock}" else entry.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = if (entry.success) "OK" else "Failed",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (entry.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
