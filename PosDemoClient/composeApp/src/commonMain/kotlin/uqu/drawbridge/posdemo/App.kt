package uqu.drawbridge.posdemo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.russhwolf.settings.Settings
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.dto.PosInventoryChangeRequest
import uqu.drawbridge.platform.dto.PosInventoryChangeType
import uqu.drawbridge.posdemo.api.PosDemoApiClient
import uqu.drawbridge.posdemo.ui.components.BarcodeScannerView
import uqu.drawbridge.posdemo.ui.theme.PosTheme
import kotlin.random.Random

import io.ktor.server.engine.embeddedServer
import io.ktor.server.cio.CIO
import io.ktor.server.application.install
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.routing.post
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.http.HttpStatusCode
import uqu.drawbridge.platform.dto.PosInventoryWebhookPayload
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab

data class ScanHistoryEntry(
    val gtin: String,
    val success: Boolean,
    val message: String,
    val productName: String? = null
)

@Composable
expect fun RequestCameraPermissionOnStartup()

// ── Main App ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    PosTheme {
        RequestCameraPermissionOnStartup()

        val colors = MaterialTheme.colorScheme
        val settings = remember { Settings() }

        // ── persisted settings ─────────────────────────────
        var serverUrl  by remember { mutableStateOf(settings.getString("serverUrl", "http://localhost:8080")) }
        var apiKey     by remember { mutableStateOf(settings.getString("apiKey", "")) }
        var retailerId by remember { mutableStateOf(settings.getString("retailerId", "")) }
        var enableBackgroundScanner by remember { mutableStateOf(settings.getBoolean("enableBackgroundScanner", true)) }

        LaunchedEffect(serverUrl)  { settings.putString("serverUrl", serverUrl) }
        LaunchedEffect(apiKey)     { settings.putString("apiKey", apiKey) }
        LaunchedEffect(retailerId) { settings.putString("retailerId", retailerId) }
        LaunchedEffect(enableBackgroundScanner) { settings.putBoolean("enableBackgroundScanner", enableBackgroundScanner) }

        // ── UI state ──────────────────────────────────────
        var barcodeInput     by remember { mutableStateOf("") }
        var scanHistory      by remember { mutableStateOf(listOf<ScanHistoryEntry>()) }
        var isScanning       by remember { mutableStateOf(false) }
        var showCameraScanner by remember { mutableStateOf(false) }
        var showSettingsDialog by remember { mutableStateOf(false) }
        val webhooks = remember { mutableStateListOf<PosInventoryWebhookPayload>() }

        LaunchedEffect(Unit) {
            embeddedServer(CIO, port = 8085) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                routing {
                    post("/webhook") {
                        try {
                            val payload = call.receive<PosInventoryWebhookPayload>()
                            webhooks.add(0, payload)
                            call.respondText("OK")
                        } catch (e: Exception) {
                            call.respondText("Error: ${e.message}", status = HttpStatusCode.BadRequest)
                        }
                    }
                }
            }.start(wait = false)
        }

        val coroutineScope = rememberCoroutineScope()
        val apiClient = remember { PosDemoApiClient() }

        val isConfigured = apiKey.isNotBlank() && retailerId.isNotBlank()

        fun triggerScan(gtin: String) {
            if (gtin.isBlank() || isScanning) return
            if (!isConfigured) {
                scanHistory = listOf(ScanHistoryEntry(gtin, false, "API Key and Retailer ID required — open Settings")) + scanHistory
                return
            }

            isScanning = true
            coroutineScope.launch {
                val request = PosInventoryChangeRequest(
                    eventId = "pos-demo-${Random.nextLong()}",
                    retailerId = retailerId.trim(),
                    gtin = gtin.trim(),
                    changeType = PosInventoryChangeType.DELTA,
                    quantityDelta = -1
                )

                val result = apiClient.sendInventoryChange(serverUrl, apiKey.trim(), request)

                result.onSuccess { response ->
                    scanHistory = listOf(ScanHistoryEntry(gtin, true, "OK — New stock: ${response.quantityAfter}", response.productName)) + scanHistory
                    barcodeInput = ""
                }.onFailure { err ->
                    scanHistory = listOf(ScanHistoryEntry(gtin, false, err.message ?: "Unknown Error")) + scanHistory
                }

                isScanning = false
            }
        }

        GlobalBarcodeScannerHook(enableBackgroundScanner) { scannedValue ->
            barcodeInput = "" // Clear input to prevent double scan and ghost text
            triggerScan(scannedValue)
        }

        // ── Scaffold ──────────────────────────────────────

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("POS Client", fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isConfigured) "Connected" else "Not configured",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isConfigured) colors.secondary else colors.onSurfaceVariant,
                            )
                        }
                    },
                    actions = {
                        // Settings gear button
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("⚙", fontSize = 22.sp, color = colors.onSurfaceVariant)
                                // Dot indicator when not configured
                                if (!isConfigured) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.error)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.surfaceContainerHigh,
                        titleContentColor = colors.onSurface,
                    )
                )
            },
            containerColor = colors.background
        ) { paddingValues ->
            val pagerState = rememberPagerState(pageCount = { 2 })

            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("Scanner") }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("Webhooks") }
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // ── Scanner Input Card ─────────────────────
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Text(
                                                text = "Scan Item",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.onSurface,
                                            )

                                            if (!showCameraScanner) {
                                                Button(
                                                    onClick = { showCameraScanner = true },
                                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = colors.primary,
                                                        contentColor = colors.onPrimary,
                                                    )
                                                ) {
                                                    Text("📷  Scan with Camera", style = MaterialTheme.typography.labelLarge)
                                                }
                                            }

                                            OutlinedTextField(
                                                value = barcodeInput,
                                                onValueChange = {
                                                    barcodeInput = it
                                                    if (it.length == 13 && it.all { char -> char.isDigit() }) {
                                                        triggerScan(it)
                                                        barcodeInput = ""
                                                    }
                                                },
                                                label = { Text("Barcode / GTIN") },
                                                modifier = Modifier.fillMaxWidth(),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = { triggerScan(barcodeInput) }
                                                ),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                            )

                                            Button(
                                                onClick = { triggerScan(barcodeInput) },
                                                enabled = barcodeInput.isNotBlank() && !isScanning,
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(12.dp),
                                            ) {
                                                Text(
                                                    if (isScanning) "Scanning…" else "Submit Scan",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }
                                        }
                                    }
                                }

                                // ── Scan History ───────────────────────────
                                if (scanHistory.isNotEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "Scan History",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colors.onSurface,
                                            )
                                            Text(
                                                text = "${scanHistory.size} scans",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    items(scanHistory) { entry ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    SelectionContainer {
                                                        Column {
                                                            if (entry.productName != null) {
                                                                Text(
                                                                    text = entry.productName,
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = colors.onSurface,
                                                                )
                                                                Text(
                                                                    text = entry.gtin,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = colors.onSurfaceVariant,
                                                                )
                                                            } else {
                                                                Text(
                                                                    text = entry.gtin,
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = colors.onSurface,
                                                                )
                                                            }
                                                            Spacer(Modifier.height(2.dp))
                                                            Text(
                                                                text = entry.message,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = if (entry.success) colors.secondary else colors.error,
                                                            )
                                                        }
                                                    }
                                                }
                                                // Status badge
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (entry.success) colors.secondary.copy(alpha = 0.15f)
                                                            else colors.error.copy(alpha = 0.15f)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (entry.success) "✓" else "✗",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (entry.success) colors.secondary else colors.error,
                                                        textAlign = TextAlign.Center,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (webhooks.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No webhooks received yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 32.dp)
                                        )
                                    }
                                } else {
                                    items(webhooks) { webhook ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant),
                                        ) {
                                            SelectionContainer {
                                                Column(modifier = Modifier.padding(16.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "GTIN: ${webhook.gtin}",
                                                                style = MaterialTheme.typography.titleMedium,
                                                                color = colors.onSurface,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                text = "Event ID: ${webhook.eventId}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colors.onSurfaceVariant
                                                            )
                                                        }
                                                    
                                                    val isPositive = webhook.changeAmount > 0
                                                    val badgeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                                                    val sign = if (isPositive) "+" else ""
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(badgeColor.copy(alpha = 0.2f))
                                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$sign${webhook.changeAmount}",
                                                            color = badgeColor,
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(Modifier.height(12.dp))
                                                HorizontalDivider(color = colors.outline.copy(alpha = 0.2f))
                                                Spacer(Modifier.height(12.dp))
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Before", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                                        Text("${webhook.quantityBefore}", style = MaterialTheme.typography.titleLarge, color = colors.onSurface, fontWeight = FontWeight.Bold)
                                                    }
                                                    
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text(webhook.sourceType, style = MaterialTheme.typography.labelMedium, color = colors.primary, fontWeight = FontWeight.Bold)
                                                        Text("➔", style = MaterialTheme.typography.titleLarge, color = colors.primary)
                                                    }
                                                    
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("After", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                                                        Text("${webhook.quantityAfter}", style = MaterialTheme.typography.titleLarge, color = colors.onSurface, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                
                                                if (!webhook.reason.isNullOrBlank()) {
                                                    Spacer(Modifier.height(12.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(colors.primaryContainer)
                                                            .padding(12.dp)
                                                    ) {
                                                        Text(
                                                            text = "📝 Reason: ${webhook.reason}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = colors.onPrimaryContainer,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                                
                                                Spacer(Modifier.height(12.dp))
                                                
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(colors.surfaceContainerHigh)
                                                        .padding(8.dp)
                                                ) {
                                                    val metaStyle = MaterialTheme.typography.labelSmall
                                                    val metaColor = colors.onSurfaceVariant.copy(alpha = 0.8f)
                                                    
                                                    Text("Retailer: ${webhook.retailerId}", style = metaStyle, color = metaColor)
                                                    Text("Product ID: ${webhook.productId}", style = metaStyle, color = metaColor)
                                                    Text("Inventory ID: ${webhook.inventoryItemId}", style = metaStyle, color = metaColor)
                                                    if (webhook.sourceId != null) {
                                                        Text("Source ID: ${webhook.sourceId}", style = metaStyle, color = metaColor)
                                                    }
                                                }
                                                
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = webhook.eventTime.substringBefore("T") + " " + webhook.eventTime.substringAfter("T").take(8),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.align(Alignment.End)
                                                )
                                            }
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Settings Dialog ───────────────────────────────

        if (showSettingsDialog) {
            SettingsDialog(
                serverUrl  = serverUrl,
                apiKey     = apiKey,
                retailerId = retailerId,
                enableBackgroundScanner = enableBackgroundScanner,
                onServerUrlChange  = { serverUrl = it },
                onApiKeyChange     = { apiKey = it },
                onRetailerIdChange = { retailerId = it },
                onEnableBackgroundScannerChange = { enableBackgroundScanner = it },
                onDismiss = { showSettingsDialog = false },
            )
        }

        // ── Camera Scanner Dialog ─────────────────────────

        if (showCameraScanner) {
            Dialog(
                onDismissRequest = { showCameraScanner = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    BarcodeScannerView(
                        onBarcodeScanned = { scannedValue ->
                            showCameraScanner = false
                            triggerScan(scannedValue)
                        },
                        onClose = { showCameraScanner = false }
                    )
                }
            }
        }
    }
}

// ── Settings Dialog Composable ─────────────────────────────

@Composable
private fun SettingsDialog(
    serverUrl: String,
    apiKey: String,
    retailerId: String,
    enableBackgroundScanner: Boolean,
    onServerUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onRetailerIdChange: (String) -> Unit,
    onEnableBackgroundScannerChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "API Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                Text(
                    text = "Configure connection to your Drawbridge server.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )

                HorizontalDivider(color = colors.outline.copy(alpha = 0.4f))

                // Fields
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = onServerUrlChange,
                    label = { Text("Server URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = onApiKeyChange,
                    label = { Text("POS API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                OutlinedTextField(
                    value = retailerId,
                    onValueChange = onRetailerIdChange,
                    label = { Text("Retailer ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Scanner", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                        Text("Listen for barcode scans globally", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    Switch(
                        checked = enableBackgroundScanner,
                        onCheckedChange = onEnableBackgroundScannerChange
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Done button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Done", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
