package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uqu.drawbridge.platform.DashboardSummary
import uqu.drawbridge.platform.MobileApiConfig
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.BarcodeScannerView
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
                    text = "📷  Scan with Camera",
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
                    Text(
                        text = if (isSuccess) "✓ Scan Successful" else "✗ Scan Failed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
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
                            text = if (entry.success) "✓" else "✗",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (entry.success) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
