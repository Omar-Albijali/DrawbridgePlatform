package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.CategoryDTO
import uqu.drawbridge.platform.InventoryAuditLogDTO
import uqu.drawbridge.platform.InventoryItemDTO
import uqu.drawbridge.platform.InventoryStatus
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.ScheduleType
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.BarcodeScannerView
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.ErrorStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatCard
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.operations.AutoRestockUiState
import uqu.drawbridge.platform.ui.operations.InventoryDetailUiState
import uqu.drawbridge.platform.ui.operations.InventoryHistoryUiState
import uqu.drawbridge.platform.ui.operations.InventoryMode
import uqu.drawbridge.platform.ui.operations.InventoryStateHolder
import uqu.drawbridge.platform.ui.operations.PosStateHolder
import uqu.drawbridge.platform.ui.operations.ProductManagementStateHolder

private val InventoryText = Color(0xFFF8FAFC)
private val InventoryMuted = Color(0xFFA8B7C7)
private val InventoryCardBg = Color(0xFF102A3D).copy(alpha = 0.92f)
private val InventoryPanelBg = Color(0xFF162F43).copy(alpha = 0.86f)
private val InventoryBorder = Color.White.copy(alpha = 0.12f)
private val InventorySoftLine = Color.White.copy(alpha = 0.08f)
private val InventoryIconBg = Color(0xFF183348)
private val InventoryWarning = Color(0xFFFFA726)
private val InventoryDanger = Color(0xFFFF5A65)
private val InventorySuccess = Color(0xFF10B981)
private val HistoryText = InventoryText
private val HistoryMuted = InventoryMuted
private val HistorySoftText = Color(0xFF7F95AA)
private val HistoryBorder = InventoryBorder
private val HistoryPanel = Color.White.copy(alpha = 0.04f)

@Composable
internal fun InventoryMainScreen(
    inventoryStateHolder: InventoryStateHolder,
    onOpenDetail: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var showAddInventory by remember { mutableStateOf(false) }
    var selectedDetailItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        inventoryStateHolder.loadInitial()
    }

    val historyState = inventoryStateHolder.historyState
    val autoRestockState = inventoryStateHolder.autoRestockState
    InventoryListContent(
        inventoryStateHolder = inventoryStateHolder,
        showAddInventory = showAddInventory,
        onShowAddInventoryChange = { showAddInventory = it },
        onOpenDetail = { itemId ->
            showAddInventory = false
            selectedDetailItemId = itemId
            coroutineScope.launch { inventoryStateHolder.loadDetail(itemId) }
        },
        onOpenHistory = { item ->
            showAddInventory = false
            inventoryStateHolder.resetAddInventoryForm()
            coroutineScope.launch { inventoryStateHolder.openStockHistory(item) }
        },
        onOpenAutoRestock = { item ->
            showAddInventory = false
            inventoryStateHolder.resetAddInventoryForm()
            inventoryStateHolder.openAutoRestockConfig(item)
        },
        onShowMessage = onShowMessage,
    )

    selectedDetailItemId?.let { itemId ->
        InventoryPanelDialog(onDismiss = { selectedDetailItemId = null }) { closePanel ->
            ManualStockEditScreen(
                itemId = itemId,
                detail = inventoryStateHolder.detailState,
                onClose = closePanel,
                onRetry = { coroutineScope.launch { inventoryStateHolder.loadDetail(itemId) } },
                onStockDraftChange = inventoryStateHolder::updateStockDraft,
                onSave = {
                    coroutineScope.launch {
                        val result = inventoryStateHolder.saveStock()
                        result.message?.let(onShowMessage)
                    }
                },
            )
        }
    }

    historyState.item?.let { selectedHistoryItem ->
        InventoryPanelDialog(onDismiss = inventoryStateHolder::closeStockHistory) { closePanel ->
            InventoryHistoryScreen(
                item = selectedHistoryItem,
                historyState = historyState,
                onClose = closePanel,
                onRetry = { coroutineScope.launch { inventoryStateHolder.reloadStockHistory() } },
                onLoadMore = { coroutineScope.launch { inventoryStateHolder.loadMoreStockHistory() } },
            )
        }
    }

    autoRestockState.item?.let { selectedRestockItem ->
        InventoryPanelDialog(onDismiss = inventoryStateHolder::closeAutoRestockConfig) { closePanel ->
            AutoRestockConfigScreen(
                item = selectedRestockItem,
                state = autoRestockState,
                onClose = closePanel,
                onSelectSchedule = inventoryStateHolder::selectAutoRestockSchedule,
                onThresholdChange = inventoryStateHolder::updateAutoRestockThreshold,
                onQuantityChange = inventoryStateHolder::updateAutoRestockQuantity,
                onIntervalChange = inventoryStateHolder::updateAutoRestockInterval,
                onDayOfWeekChange = inventoryStateHolder::updateAutoRestockDayOfWeek,
                onDayOfMonthChange = inventoryStateHolder::updateAutoRestockDayOfMonth,
                onSave = {
                    coroutineScope.launch {
                        val result = inventoryStateHolder.saveAutoRestockConfig()
                        result.message?.let(onShowMessage)
                        if (result.success) closePanel()
                    }
                },
            )
        }
    }
}

@Composable
private fun InventoryPanelDialog(
    onDismiss: () -> Unit,
    content: @Composable (onClose: () -> Unit) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val closePanel: () -> Unit = {
        coroutineScope.launch {
            visible = false
            delay(180)
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = closePanel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .padding(horizontal = 16.dp, vertical = 72.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                    slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it / 3 },
                exit = fadeOut(animationSpec = tween(130, easing = FastOutSlowInEasing)) +
                    slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing)) { -it / 5 },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    content(closePanel)
                }
            }
        }
    }
}

@Composable
private fun ManualStockEditScreen(
    itemId: String,
    detail: InventoryDetailUiState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onStockDraftChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val item = detail.inventoryItem
    val product = detail.catalogProduct
    val title = item?.name ?: product?.name ?: "Inventory detail"
    val subtitle = item?.supplier ?: product?.brand?.ifBlank { product.category } ?: "#${shortId(itemId)}"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 420.dp),
        shape = RoundedCornerShape(24.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InventoryPanelBg)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Manual Stock Edit", style = MaterialTheme.typography.titleMedium, color = InventoryText, fontWeight = FontWeight.Black)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = InventoryMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close stock edit", tint = InventoryMuted, modifier = Modifier.size(22.dp))
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when {
                    detail.isLoading -> InventoryStatePanel(
                        label = "Loading",
                        title = "Loading stock",
                        message = "Fetching current inventory detail.",
                    )
                    detail.errorMessage != null && item == null && product == null -> InventoryStatePanel(
                        label = "Issue",
                        title = "Inventory unavailable",
                        message = detail.errorMessage,
                        actionText = "Try again",
                        onAction = onRetry,
                    )
                    else -> {
                        val currentStock = item?.currentStock ?: product?.stock ?: 0
                        val moq = item?.minimumOrderQuantity ?: product?.minimumOrderQuantity ?: 1
                        val statusText = item?.let(::inventoryStatusLabel) ?: product?.let(::stockStatusLabel) ?: "Stock"
                        val statusTone = item?.let(::inventoryStatusTone) ?: product?.let(::stockStatusTone) ?: StatusTone.Neutral

                        Text(title, style = MaterialTheme.typography.titleLarge, color = InventoryText, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Surface(shape = RoundedCornerShape(10.dp), color = HistoryPanel, border = BorderStroke(1.dp, InventoryBorder)) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Current stock", style = MaterialTheme.typography.labelMedium, color = InventoryMuted, fontWeight = FontWeight.Bold)
                                    Text(currentStock.toString(), style = MaterialTheme.typography.headlineSmall, color = InventoryText, fontWeight = FontWeight.Black)
                                    Text("MOQ ${moq.coerceAtLeast(1)}", style = MaterialTheme.typography.bodySmall, color = InventoryMuted, fontWeight = FontWeight.SemiBold)
                                }
                                StatusChip(text = statusText, tone = statusTone)
                            }
                        }

                        detail.errorMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = InventoryDanger, fontWeight = FontWeight.SemiBold)
                        }
                        AutoRestockNumberInput(
                            label = "New stock quantity",
                            value = detail.stockDraft,
                            suffix = "units",
                            helper = "Client validation prevents negative stock.",
                            onChange = onStockDraftChange,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            InventoryActionButton("Cancel", Icons.Default.Close, onClose, Modifier.weight(1f))
                            InventoryActionButton(if (detail.isSaving) "Saving..." else "Save Stock", Icons.Default.CheckCircle, onSave, Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

@Composable
private fun InventoryListContent(
    inventoryStateHolder: InventoryStateHolder,
    showAddInventory: Boolean,
    onShowAddInventoryChange: (Boolean) -> Unit,
    onOpenDetail: (String) -> Unit,
    onOpenHistory: (InventoryItemDTO) -> Unit,
    onOpenAutoRestock: (InventoryItemDTO) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = inventoryStateHolder.state

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InventoryCatalogHeader(
            mode = state.mode,
            onAddInventory = {
                if (state.mode == InventoryMode.RetailerInventory) {
                    onShowAddInventoryChange(!showAddInventory)
                    if (!showAddInventory) {
                        coroutineScope.launch { inventoryStateHolder.loadAddInventoryProducts() }
                    } else {
                        inventoryStateHolder.resetAddInventoryForm()
                    }
                } else {
                    coroutineScope.launch { inventoryStateHolder.refresh() }
                }
            },
            isRefreshing = state.isRefreshing,
        )

        if (showAddInventory && state.mode == InventoryMode.RetailerInventory) {
            AddInventoryPanel(
                state = state,
                onSearchChange = inventoryStateHolder::updateAddInventorySearchInput,
                onSelectProduct = inventoryStateHolder::selectAddInventoryProduct,
                onChangeProduct = inventoryStateHolder::clearAddInventorySelection,
                onStockChange = inventoryStateHolder::updateAddStockDraft,
                onThresholdChange = inventoryStateHolder::updateAddThresholdDraft,
                onToggleAutoRestock = inventoryStateHolder::toggleAddAutoRestock,
                onCancel = {
                    onShowAddInventoryChange(false)
                    inventoryStateHolder.resetAddInventoryForm()
                },
                onSubmit = {
                    coroutineScope.launch {
                        val result = inventoryStateHolder.createInventoryItem()
                        result.message?.let(onShowMessage)
                        if (result.success) {
                            onShowAddInventoryChange(false)
                        }
                    }
                },
            )
        }

        when {
            state.isLoading -> InventoryStatePanel(
                label = "Loading",
                title = "Loading inventory",
                message = "Checking current stock levels.",
            )
            state.errorMessage != null && state.totalCount == 0 -> {
                InventoryStatePanel(
                    label = "Issue",
                    title = "Could not load inventory",
                    message = state.errorMessage,
                    actionText = "Try again",
                    onAction = { coroutineScope.launch { inventoryStateHolder.refresh() } },
                )
            }
            else -> {
                InventoryStatsGrid(state = state)

                InventorySearchField(
                    value = state.searchInput,
                    onValueChange = inventoryStateHolder::updateSearchInput,
                    placeholder = if (state.mode == InventoryMode.CatalogStock) {
                        "Search catalog stock..."
                    } else {
                        "Search inventory..."
                    },
                )

                if (state.errorMessage != null) {
                    InventoryStatePanel(
                        label = "Issue",
                        title = "Inventory needs attention",
                        message = state.errorMessage,
                        actionText = "Refresh",
                        onAction = { coroutineScope.launch { inventoryStateHolder.refresh() } },
                    )
                }

                if (state.mode == InventoryMode.CatalogStock) {
                    if (state.filteredCatalogProducts.isEmpty()) {
                        InventoryStatePanel(
                            title = "No products found",
                            message = if (state.catalogProducts.isEmpty()) {
                                "Create products before managing catalog stock."
                            } else {
                                "Try a different search."
                            },
                        )
                    } else {
                        state.filteredCatalogProducts.forEach { product ->
                            CatalogStockCard(
                                product = product,
                                isBusy = product.id in state.busyItemIds,
                                onOpenDetail = { onOpenDetail(product.id) },
                            )
                        }
                    }
                } else {
                    if (state.filteredInventoryItems.isEmpty()) {
                        InventoryStatePanel(
                            title = "No inventory items found",
                            message = if (state.inventoryItems.isEmpty()) {
                                "Inventory entries will appear when products are stocked."
                            } else {
                                "Try a different search."
                            },
                        )
                    } else {
                        state.filteredInventoryItems.forEach { item ->
                            RetailerInventoryCard(
                                item = item,
                                isBusy = item.id in state.busyItemIds,
                                onToggleAutoRestock = {
                                    coroutineScope.launch {
                                        val result = inventoryStateHolder.toggleAutoRestock(item)
                                        result.message?.let(onShowMessage)
                                    }
                                },
                                onOpenDetail = { onOpenDetail(item.id) },
                                onOpenHistory = { onOpenHistory(item) },
                                onOpenAutoRestock = { onOpenAutoRestock(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoRestockConfigScreen(
    item: InventoryItemDTO,
    state: AutoRestockUiState,
    onClose: () -> Unit,
    onSelectSchedule: (ScheduleType) -> Unit,
    onThresholdChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit,
    onDayOfWeekChange: (String) -> Unit,
    onDayOfMonthChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 690.dp),
        shape = RoundedCornerShape(24.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                        Text("Auto-Restock Configuration", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close auto-restock", tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Restock Strategy", style = MaterialTheme.typography.titleSmall, color = InventoryText, fontWeight = FontWeight.Black)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AutoRestockStrategyOption(ScheduleType.THRESHOLD_BASED, state.scheduleType, "Threshold Based", "Reorder below a set level", Icons.Default.Settings, onSelectSchedule)
                    AutoRestockStrategyOption(ScheduleType.WEEKLY, state.scheduleType, "Weekly Schedule", "Reorder on a weekly day", Icons.Default.CheckCircle, onSelectSchedule)
                    AutoRestockStrategyOption(ScheduleType.MONTHLY, state.scheduleType, "Monthly Schedule", "Reorder on a monthly day", Icons.Default.Inventory2, onSelectSchedule)
                    AutoRestockStrategyOption(ScheduleType.INTERVAL_DAYS, state.scheduleType, "Fixed Interval", "Reorder every X days", Icons.Default.History, onSelectSchedule)
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(InventorySoftLine, RoundedCornerShape(999.dp)))
                Text("Configuration Details", style = MaterialTheme.typography.titleSmall, color = InventoryText, fontWeight = FontWeight.Black)
                Surface(shape = RoundedCornerShape(8.dp), color = InventoryPanelBg, border = BorderStroke(1.dp, InventoryBorder)) {
                    Text(
                        "Product MOQ: ${item.minimumOrderQuantity.coerceAtLeast(1)} units",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InventoryMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                when (state.scheduleType) {
                    ScheduleType.THRESHOLD_BASED -> AutoRestockNumberInput("Minimum Threshold", state.minThresholdDraft, "units", "Trigger reorder below this stock", onThresholdChange)
                    ScheduleType.WEEKLY -> AutoRestockWeekPicker(selected = state.dayOfWeek, onSelected = onDayOfWeekChange)
                    ScheduleType.MONTHLY -> AutoRestockNumberInput("Day of Month", state.dayOfMonthDraft, "day", "Use 1-28", onDayOfMonthChange)
                    ScheduleType.INTERVAL_DAYS -> AutoRestockNumberInput("Interval Days", state.intervalDaysDraft, "days", "Reorder every X days", onIntervalChange)
                    ScheduleType.DAILY -> Unit
                }

                AutoRestockNumberInput("Reorder Quantity", state.reorderQuantityDraft, "units", "Amount to order when triggered", onQuantityChange)
                state.errorMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = InventoryDanger, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    InventoryActionButton("Cancel", Icons.Default.Close, onClose, Modifier.weight(1f))
                    InventoryActionButton(if (state.isSaving) "Saving..." else "Save", Icons.Default.CheckCircle, onSave, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AutoRestockStrategyOption(
    scheduleType: ScheduleType,
    selected: ScheduleType,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onSelect: (ScheduleType) -> Unit,
) {
    val active = scheduleType == selected
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onSelect(scheduleType) },
        shape = RoundedCornerShape(10.dp),
        color = if (active) Color(0xFF0D3C34) else HistoryPanel,
        border = BorderStroke(1.dp, if (active) InventorySuccess else InventoryBorder),
    ) {
        Row(modifier = Modifier.padding(13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (active) InventorySuccess else InventoryMuted, modifier = Modifier.size(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = InventoryText, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (active) Color(0xFF71E8C1) else InventoryMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun AutoRestockNumberInput(
    label: String,
    value: String,
    suffix: String,
    helper: String,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = InventoryText, fontWeight = FontWeight.Black)
        Surface(shape = RoundedCornerShape(8.dp), color = InventoryPanelBg, border = BorderStroke(1.dp, InventoryBorder)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = InventoryText, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                )
                Text(suffix, style = MaterialTheme.typography.bodyMedium, color = InventoryMuted, fontWeight = FontWeight.Bold)
            }
        }
        Text(helper, style = MaterialTheme.typography.bodySmall, color = InventoryMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AutoRestockWeekPicker(
    selected: String,
    onSelected: (String) -> Unit,
) {
    val days = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Day of Week", style = MaterialTheme.typography.labelLarge, color = InventoryText, fontWeight = FontWeight.Black)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            days.chunked(4).forEach { rowDays ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rowDays.forEach { day ->
                        AutoRestockDayChip(day.take(3), selected == day, { onSelected(day) }, Modifier.weight(1f))
                    }
                    repeat(4 - rowDays.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun AutoRestockDayChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(40.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF0D3C34) else HistoryPanel,
        border = BorderStroke(1.dp, if (selected) InventorySuccess else InventoryBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) InventorySuccess else InventoryMuted, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun InventoryHistoryScreen(
    item: InventoryItemDTO,
    historyState: InventoryHistoryUiState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 690.dp),
        shape = RoundedCornerShape(24.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = HistorySoftText,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "STOCK HISTORY",
                            style = MaterialTheme.typography.labelSmall,
                            color = HistoryMuted,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = HistoryText,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.supplier,
                        style = MaterialTheme.typography.bodyMedium,
                        color = HistoryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close stock history",
                        tint = HistorySoftText,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HistoryBorder),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(InventoryPanelBg)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = eventCountLabel(historyState.totalElements),
                    style = MaterialTheme.typography.labelLarge,
                    color = HistoryText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "NEWEST FIRST",
                    style = MaterialTheme.typography.labelSmall,
                    color = HistoryMuted,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HistoryBorder),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when {
                    historyState.isLoading -> {
                        repeat(3) {
                            InventoryHistoryLoadingCard()
                        }
                    }
                    historyState.errorMessage != null -> {
                        InventoryHistoryStateCard(
                            title = "Unable to load stock history",
                            message = historyState.errorMessage,
                            actionText = "Try again",
                            onAction = onRetry,
                        )
                    }
                    historyState.logs.isEmpty() -> {
                        InventoryHistoryStateCard(
                            title = "No stock history yet",
                            message = "Stock movements for this inventory item will appear here.",
                        )
                    }
                    else -> {
                        historyState.logs.forEach { log ->
                            InventoryHistoryEventCard(log = log)
                        }
                        InventoryHistoryFooter(
                            hasMore = historyState.hasMore,
                            isLoadingMore = historyState.isLoadingMore,
                            onLoadMore = onLoadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryHistoryLoadingCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp),
        shape = RoundedCornerShape(10.dp),
        color = HistoryPanel,
        border = BorderStroke(1.dp, HistoryBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .background(HistoryBorder, RoundedCornerShape(999.dp)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(HistoryBorder.copy(alpha = 0.55f), RoundedCornerShape(999.dp)),
            )
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(10.dp)
                    .background(HistoryBorder.copy(alpha = 0.55f), RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun InventoryHistoryStateCard(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = HistoryPanel,
        border = BorderStroke(1.dp, HistoryBorder),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InventoryProductTile(modifier = Modifier.size(48.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = HistoryText, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = HistoryMuted, textAlign = TextAlign.Center)
            if (actionText != null && onAction != null) {
                InventoryHistoryFooterButton(text = actionText, enabled = true, onClick = onAction)
            }
        }
    }
}

@Composable
private fun InventoryHistoryEventCard(log: InventoryAuditLogDTO) {
    val amountTone = amountTone(log.changeAmount)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = HistoryPanel,
        border = BorderStroke(1.dp, HistoryBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(amountTone.background, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = amountSymbol(log.changeAmount),
                        style = MaterialTheme.typography.titleLarge,
                        color = amountTone.foreground,
                        fontWeight = FontWeight.Normal,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        InventoryHistorySourceChip(sourceType = log.sourceType)
                        InventoryHistoryAmountChip(amount = log.changeAmount)
                    }
                    Text(
                        text = "${log.quantityBefore} \u2192 ${log.quantityAfter}",
                        style = MaterialTheme.typography.titleLarge,
                        color = HistoryText,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = formatAuditDateTime(log.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = HistoryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InventoryHistoryMetaBlock(
                    label = "Changed by",
                    value = log.changedBy.ifBlank { "SYSTEM" },
                    modifier = Modifier.weight(1f),
                )
                InventoryHistoryMetaBlock(
                    label = "Reason",
                    value = log.reason?.takeIf { it.isNotBlank() } ?: "Not specified",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InventoryHistorySourceChip(sourceType: String) {
    val tone = sourceTone(sourceType)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tone.background,
        border = BorderStroke(1.dp, tone.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = sourceIcon(sourceType),
                contentDescription = null,
                tint = tone.foreground,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = sourceLabel(sourceType),
                style = MaterialTheme.typography.labelSmall,
                color = tone.foreground,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InventoryHistoryAmountChip(amount: Int) {
    val tone = amountTone(amount)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tone.background,
        border = BorderStroke(1.dp, tone.border),
    ) {
        Text(
            text = formatSignedAmount(amount),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = tone.foreground,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun InventoryHistoryMetaBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = HistoryMuted, fontWeight = FontWeight.SemiBold)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = HistoryText,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InventoryHistoryFooter(
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
) {
    val enabled = hasMore && !isLoadingMore
    InventoryHistoryFooterButton(
        text = when {
            isLoadingMore -> "Loading..."
            hasMore -> "Load more"
            else -> "All history loaded"
        },
        enabled = enabled,
        onClick = onLoadMore,
    )
}

@Composable
private fun InventoryHistoryFooterButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = HistoryPanel,
        border = BorderStroke(1.dp, if (enabled) HistoryBorder else Color.Transparent),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = HistoryMuted,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class HistoryTone(
    val background: Color,
    val foreground: Color,
    val border: Color,
)

private fun eventCountLabel(totalElements: Int): String {
    return "$totalElements ${if (totalElements == 1) "EVENT" else "EVENTS"}"
}

private fun sourceLabel(sourceType: String): String = when (sourceType.uppercase()) {
    "MANUAL" -> "Manual"
    "ORDER" -> "Order"
    "RESTOCK" -> "Restock"
    "POS" -> "POS"
    else -> "System"
}

private fun sourceIcon(sourceType: String): ImageVector = when (sourceType.uppercase()) {
    "MANUAL" -> Icons.Default.History
    "ORDER" -> Icons.Default.ShoppingCart
    "RESTOCK" -> Icons.Default.CheckCircle
    "POS" -> Icons.Default.Inventory2
    else -> Icons.Default.Settings
}

private fun sourceTone(sourceType: String): HistoryTone = when (sourceType.uppercase()) {
    "MANUAL" -> HistoryTone(Color(0xFF153A5C), Color(0xFF9CCBFF), Color(0xFF275B86))
    "ORDER" -> HistoryTone(Color(0xFF172D61), Color(0xFF94B2FF), Color(0xFF3155A1))
    "RESTOCK" -> HistoryTone(Color(0xFF0D3C34), Color(0xFF71E8C1), Color(0xFF167C67))
    "POS" -> HistoryTone(Color(0xFF3B1747), Color(0xFFF0ABFC), Color(0xFF7E2F91))
    else -> HistoryTone(Color.White.copy(alpha = 0.06f), HistoryMuted, HistoryBorder)
}

private fun amountTone(amount: Int): HistoryTone = when {
    amount > 0 -> HistoryTone(Color(0xFF0D3C34), Color(0xFF35D399), Color(0xFF167C67))
    amount < 0 -> HistoryTone(Color(0xFF4A1D26), Color(0xFFFF8A94), Color(0xFF8D3442))
    else -> HistoryTone(Color.White.copy(alpha = 0.06f), HistoryMuted, HistoryBorder)
}

private fun amountSymbol(amount: Int): String = when {
    amount > 0 -> "\u2191"
    amount < 0 -> "\u2193"
    else -> "\u2194"
}

private fun formatSignedAmount(amount: Int): String {
    return if (amount > 0) "+$amount" else amount.toString()
}

private fun formatAuditDateTime(value: String): String {
    val normalized = value.trim().replace("T", " ").removeSuffix("Z").substringBefore(".")
    val parts = normalized.split(" ")
    val dateParts = parts.getOrNull(0)?.split("-").orEmpty()
    val timeParts = parts.getOrNull(1)?.split(":").orEmpty()
    val year = dateParts.getOrNull(0)?.toIntOrNull()
    val month = dateParts.getOrNull(1)?.toIntOrNull()
    val day = dateParts.getOrNull(2)?.toIntOrNull()
    val hour = timeParts.getOrNull(0)?.toIntOrNull()
    val minute = timeParts.getOrNull(1)?.toIntOrNull()
    if (year == null || month == null || day == null || hour == null || minute == null) {
        return value.take(16).replace("T", " ")
    }

    val displayHour = when (val hour12 = hour % 12) {
        0 -> 12
        else -> hour12
    }
    val amPm = if (hour >= 12) "PM" else "AM"
    return "${monthName(month)} $day, $year, ${displayHour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')} $amPm"
}

private fun monthName(month: Int): String = when (month) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    12 -> "Dec"
    else -> "Date"
}

@Composable
private fun InventoryCatalogHeader(
    mode: InventoryMode,
    onAddInventory: () -> Unit,
    isRefreshing: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = "Inventory",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InventoryText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (mode == InventoryMode.CatalogStock) {
                        "Monitor catalog stock, reorder needs, and product availability"
                    } else {
                        "Monitor stock, reorder needs, and automation"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = InventoryMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddInventory),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isRefreshing) "Refreshing inventory..." else {
                            if (mode == InventoryMode.CatalogStock) "Refresh Catalog Stock" else "Add Product to Inventory"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryStatsGrid(state: uqu.drawbridge.platform.ui.operations.InventoryUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            InventoryStatTile(
                value = state.totalCount.toString(),
                label = "Total Items",
                icon = Icons.Default.Inventory2,
                tint = InventoryMuted,
                modifier = Modifier.weight(1f),
            )
            InventoryStatTile(
                value = state.lowStockCount.toString(),
                label = "Low Stock",
                icon = Icons.Default.Inventory2,
                tint = InventoryWarning,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            InventoryStatTile(
                value = state.outOfStockCount.toString(),
                label = "Out of Stock",
                icon = Icons.Default.Inventory2,
                tint = InventoryDanger,
                modifier = Modifier.weight(1f),
            )
            InventoryStatTile(
                value = state.autoRestockCount.toString(),
                label = "Auto-Restock",
                icon = Icons.Default.CheckCircle,
                tint = InventorySuccess,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InventoryStatTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(14.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, tint.copy(alpha = if (tint == InventoryMuted) 0.18f else 0.45f)),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = InventoryMuted,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = InventoryText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun InventorySearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = InventoryMuted, modifier = Modifier.size(20.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = InventoryMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = InventoryText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun InventoryStatePanel(
    title: String,
    message: String,
    label: String = "Empty",
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusChip(text = label, tone = StatusTone.Neutral)
            Text(title, style = MaterialTheme.typography.titleLarge, color = InventoryText, fontWeight = FontWeight.Black)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = InventoryMuted)
            if (actionText != null && onAction != null) {
                PrimaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
private fun AddInventoryPanel(
    state: uqu.drawbridge.platform.ui.operations.InventoryUiState,
    onSearchChange: (String) -> Unit,
    onSelectProduct: (ProductDTO) -> Unit,
    onChangeProduct: () -> Unit,
    onStockChange: (String) -> Unit,
    onThresholdChange: (String) -> Unit,
    onToggleAutoRestock: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add Inventory Item",
                        style = MaterialTheme.typography.titleLarge,
                        color = InventoryText,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = if (state.selectedAddProduct == null) "Select a marketplace product" else "Configure stock controls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InventoryMuted,
                    )
                }
                StatusChip(text = if (state.selectedAddProduct == null) "Step 1" else "Step 2", tone = StatusTone.Success)
            }

            val selectedProduct = state.selectedAddProduct
            if (selectedProduct == null) {
                InventorySearchField(
                    value = state.addInventorySearchInput,
                    onValueChange = onSearchChange,
                    placeholder = "Search marketplace products...",
                )
                when {
                    state.isLoadingAddProducts -> Text(
                        text = "Loading products...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InventoryMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                    state.filteredAddInventoryProducts.isEmpty() -> Text(
                        text = "No products found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InventoryMuted,
                    )
                    else -> state.filteredAddInventoryProducts.take(8).forEach { product ->
                        AddInventoryProductRow(product = product, onClick = { onSelectProduct(product) })
                    }
                }
                InventoryActionButton(
                    text = "Cancel",
                    icon = Icons.Default.MoreVert,
                    onClick = onCancel,
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = InventoryPanelBg,
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        InventoryProductTile(modifier = Modifier.size(54.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = selectedProduct.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = InventoryText,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = selectedProduct.brand.ifBlank { selectedProduct.supplier.ifBlank { selectedProduct.category } },
                                style = MaterialTheme.typography.bodySmall,
                                color = InventoryMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "Change",
                            modifier = Modifier.clickable(onClick = onChangeProduct),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    InventoryNumberField(
                        label = "Current Stock",
                        value = state.addStockDraft,
                        onValueChange = onStockChange,
                        modifier = Modifier.weight(1f),
                    )
                    InventoryNumberField(
                        label = "Min Threshold",
                        value = state.addThresholdDraft,
                        onValueChange = onThresholdChange,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    InventoryToggle(checked = state.addAutoRestock, enabled = !state.isSubmittingAddInventory, onClick = onToggleAutoRestock)
                    Text(
                        text = "Enable Auto-Restock",
                        style = MaterialTheme.typography.titleMedium,
                        color = InventoryText,
                        fontWeight = FontWeight.Black,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    InventoryActionButton(
                        text = "Cancel",
                        icon = Icons.Default.MoreVert,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    InventoryActionButton(
                        text = if (state.isSubmittingAddInventory) "Adding..." else "Add",
                        icon = Icons.Default.Add,
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddInventoryProductRow(
    product: ProductDTO,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InventoryProductTile(modifier = Modifier.size(52.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = InventoryText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.brand.ifBlank { product.supplier.ifBlank { product.category } },
                    style = MaterialTheme.typography.bodySmall,
                    color = InventoryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "MOQ ${product.minimumOrderQuantity.coerceAtLeast(1)}",
                style = MaterialTheme.typography.labelMedium,
                color = InventoryMuted,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InventoryNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(76.dp),
        shape = RoundedCornerShape(8.dp),
        color = InventoryPanelBg,
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = InventoryMuted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = InventoryText,
                    fontWeight = FontWeight.Black,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
internal fun InventoryDetailMainScreen(
    itemId: String,
    inventoryStateHolder: InventoryStateHolder,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val detail = inventoryStateHolder.detailState

    LaunchedEffect(itemId) {
        inventoryStateHolder.loadDetail(itemId)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OperationsHeader(title = "Inventory detail", subtitle = "#${shortId(itemId)}", onBack = onBack)

        when {
            detail.isLoading -> LoadingStateCard(title = "Loading stock", message = "Fetching current inventory detail.")
            detail.errorMessage != null && detail.inventoryItem == null && detail.catalogProduct == null -> ErrorStateCard(
                title = "Inventory unavailable",
                message = detail.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { inventoryStateHolder.loadDetail(itemId) } },
            )
            detail.inventoryItem != null -> InventoryDetailContent(
                title = detail.inventoryItem.name,
                subtitle = detail.inventoryItem.supplier,
                currentStock = detail.inventoryItem.currentStock,
                minimumOrderQuantity = detail.inventoryItem.minimumOrderQuantity,
                statusText = inventoryStatusLabel(detail.inventoryItem),
                statusTone = inventoryStatusTone(detail.inventoryItem),
                stockDraft = detail.stockDraft,
                isSaving = detail.isSaving,
                errorMessage = detail.errorMessage,
                onStockDraftChange = inventoryStateHolder::updateStockDraft,
                onSave = {
                    coroutineScope.launch {
                        val result = inventoryStateHolder.saveStock()
                        result.message?.let(onShowMessage)
                    }
                },
            )
            detail.catalogProduct != null -> InventoryDetailContent(
                title = detail.catalogProduct.name,
                subtitle = "${detail.catalogProduct.category.ifBlank { "Uncategorized" }} • ${detail.catalogProduct.brand}",
                currentStock = detail.catalogProduct.stock,
                minimumOrderQuantity = detail.catalogProduct.minimumOrderQuantity,
                statusText = stockStatusLabel(detail.catalogProduct),
                statusTone = stockStatusTone(detail.catalogProduct),
                stockDraft = detail.stockDraft,
                isSaving = detail.isSaving,
                errorMessage = detail.errorMessage,
                onStockDraftChange = inventoryStateHolder::updateStockDraft,
                onSave = {
                    coroutineScope.launch {
                        val result = inventoryStateHolder.saveStock()
                        result.message?.let(onShowMessage)
                    }
                },
            )
        }
    }
}

@Composable
internal fun ProductManagementMainScreen(
    productManagementStateHolder: ProductManagementStateHolder,
    onCreateProduct: () -> Unit,
    onEditProduct: (ProductDTO) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = productManagementStateHolder.state
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        productManagementStateHolder.loadInitial()
    }

    ScreenSection(
        title = "Products",
        subtitle = "Manage your wholesaler catalog, stock, and publish state.",
    ) {
        when {
            state.isLoading -> {
                repeat(3) {
                    LoadingStateCard(title = "Loading products", message = "Fetching managed catalog data.")
                }
                return@ScreenSection
            }
            state.errorMessage != null && state.products.isEmpty() -> {
                ErrorStateCard(
                    title = "Could not load products",
                    message = state.errorMessage,
                    actionText = "Try again",
                    onAction = { coroutineScope.launch { productManagementStateHolder.refresh() } },
                )
                return@ScreenSection
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(value = state.products.size.toString(), label = "Total", modifier = Modifier.weight(1f))
            StatCard(value = state.products.count { it.published }.toString(), label = "Live", modifier = Modifier.weight(1f))
            StatCard(value = state.products.count { it.stock <= 0 }.toString(), label = "Out", modifier = Modifier.weight(1f))
        }

        AppCard {
            AppTextField(
                value = state.searchInput,
                onValueChange = productManagementStateHolder::updateSearchInput,
                label = "Search products",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(text = "Add product", onClick = onCreateProduct, modifier = Modifier.weight(1f))
                SecondaryButton(
                    text = if (state.isRefreshing) "Refreshing..." else "Refresh",
                    onClick = { coroutineScope.launch { productManagementStateHolder.refresh() } },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isRefreshing,
                )
            }
        }

        if (state.errorMessage != null) {
            ErrorStateCard(title = "Product update needs attention", message = state.errorMessage)
        }

        if (state.filteredProducts.isEmpty()) {
            EmptyStateCard(
                title = "No products found",
                message = if (state.products.isEmpty()) "Create your first product to publish it to the marketplace." else "Try a different search.",
                actionText = if (state.products.isEmpty()) "Add product" else null,
                onAction = if (state.products.isEmpty()) onCreateProduct else null,
            )
        } else {
            state.filteredProducts.forEach { product ->
                ManagedProductCard(
                    product = product,
                    isBusy = product.id in state.busyProductIds,
                    confirmDelete = pendingDeleteId == product.id,
                    onEdit = { onEditProduct(product) },
                    onTogglePublished = {
                        coroutineScope.launch {
                            val result = productManagementStateHolder.togglePublished(product)
                            result.message?.let(onShowMessage)
                        }
                    },
                    onDelete = {
                        if (pendingDeleteId == product.id) {
                            coroutineScope.launch {
                                val result = productManagementStateHolder.deleteProduct(product)
                                pendingDeleteId = null
                                result.message?.let(onShowMessage)
                            }
                        } else {
                            pendingDeleteId = product.id
                        }
                    },
                    onCancelDelete = { pendingDeleteId = null },
                )
            }
        }
    }
}

@Composable
internal fun ProductFormMainScreen(
    productManagementStateHolder: ProductManagementStateHolder,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val form = productManagementStateHolder.formState
    val categories = productManagementStateHolder.state.categories

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OperationsHeader(
            title = if (form.isEditing) "Edit product" else "Create product",
            subtitle = if (form.isEditing) "Update catalog details and stock." else "Add a new wholesaler product.",
            onBack = onBack,
        )

        ScreenSection(
            title = if (form.isEditing) "Product details" else "New product",
            subtitle = "Images are deferred until the native picker and upload path are wired.",
        ) {
            if (form.errorMessage != null) {
                ErrorStateCard(title = "Product form needs attention", message = form.errorMessage)
            }

            AppCard {
                AppTextField(
                    value = form.name,
                    onValueChange = { value -> productManagementStateHolder.updateForm { copy(name = value) } },
                    label = "Product name",
                )
                AppTextField(
                    value = form.description,
                    onValueChange = { value -> productManagementStateHolder.updateForm { copy(description = value) } },
                    label = "Description",
                )
                CategoryPicker(
                    categories = categories,
                    selectedCategoryId = form.categoryId,
                    onSelected = { categoryId -> productManagementStateHolder.updateForm { copy(categoryId = categoryId) } },
                )
            }

            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = form.price,
                        onValueChange = { value -> productManagementStateHolder.updateForm { copy(price = decimalInput(value)) } },
                        label = "Price SAR",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = form.stock,
                        onValueChange = { value -> productManagementStateHolder.updateForm { copy(stock = digitsOnly(value)) } },
                        label = "Stock",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = form.minimumOrderQuantity,
                        onValueChange = { value -> productManagementStateHolder.updateForm { copy(minimumOrderQuantity = digitsOnly(value)) } },
                        label = "MOQ",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                    AppTextField(
                        value = form.gtin,
                        onValueChange = { value -> productManagementStateHolder.updateForm { copy(gtin = digitsOnly(value)) } },
                        label = "GTIN",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            AppCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Marketplace status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (form.published) "Product will be visible when saved." else "Product will remain unpublished.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusChip(
                        text = if (form.published) "Published" else "Draft",
                        tone = if (form.published) StatusTone.Success else StatusTone.Neutral,
                    )
                }
                SecondaryButton(
                    text = if (form.published) "Set as draft" else "Publish",
                    onClick = { productManagementStateHolder.updateForm { copy(published = !published) } },
                )
            }

            AppCard {
                StatusChip(text = "Image upload deferred", tone = StatusTone.Warning)
                Text(
                    "Product image upload needs the native file/photo picker path. Existing product imagery is preserved by backend product image records.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "Cancel", onClick = onBack, modifier = Modifier.weight(1f), enabled = !form.isSubmitting)
                PrimaryButton(
                    text = if (form.isSubmitting) "Saving..." else "Save product",
                    onClick = {
                        coroutineScope.launch {
                            val result = productManagementStateHolder.saveForm()
                            result.message?.let(onShowMessage)
                            if (result.success) onBack()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !form.isSubmitting,
                )
            }
        }
    }
}

@Composable
internal fun PosMainScreen(
    posStateHolder: PosStateHolder,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = posStateHolder.state
    var showCameraScanner by remember { mutableStateOf(false) }

    ScreenSection(
        title = "POS Scanner",
        subtitle = "Use manual GTIN entry or the platform scanner fallback to decrement retailer inventory.",
    ) {
        AnimatedVisibility(visible = showCameraScanner) {
            BarcodeScannerView(
                onBarcodeScanned = { scannedValue ->
                    showCameraScanner = false
                    posStateHolder.updateBarcodeInput(scannedValue)
                    coroutineScope.launch {
                        val result = posStateHolder.scan(scannedValue)
                        result.message?.let(onShowMessage)
                    }
                },
                onClose = { showCameraScanner = false },
            )
        }

        AppCard {
            StatusChip(text = "Manual entry ready", tone = StatusTone.Success)
            Text(
                "Native iOS camera scanning remains behind the build-safe fallback until AVFoundation is wired without risking Kotlin/Native compilation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SecondaryButton(
                text = if (showCameraScanner) "Hide camera fallback" else "Open scanner fallback",
                onClick = { showCameraScanner = !showCameraScanner },
            )
            AppTextField(
                value = state.barcodeInput,
                onValueChange = posStateHolder::updateBarcodeInput,
                label = "Barcode / GTIN",
                keyboardType = KeyboardType.Number,
            )
            PrimaryButton(
                text = if (state.isScanning) "Scanning..." else "Lookup and decrement",
                enabled = state.barcodeInput.isNotBlank() && !state.isScanning,
                onClick = {
                    coroutineScope.launch {
                        val result = posStateHolder.scan()
                        result.message?.let(onShowMessage)
                    }
                },
            )
        }

        if (state.errorMessage != null) {
            ErrorStateCard(title = "POS lookup failed", message = state.errorMessage)
        }

        state.lastResult?.let { result ->
            val success = result.message == "OK"
            AppCard {
                StatusChip(
                    text = if (success) "Scan successful" else "Scan failed",
                    tone = if (success) StatusTone.Success else StatusTone.Error,
                )
                Text(
                    text = result.productName.ifBlank { "No matched product" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = if (success) "Remaining stock: ${result.newStock}" else result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (success) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
            }
        }

        if (state.history.isEmpty()) {
            EmptyStateCard(
                title = "No scans yet",
                message = "Successful and failed manual barcode lookups will appear here during this session.",
            )
        } else {
            Text("Scan history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            state.history.forEach { entry ->
                AppCard {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.productName.ifBlank { entry.gtin }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (entry.success) "Stock after scan: ${entry.newStock}" else entry.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusChip(text = if (entry.success) "OK" else "Failed", tone = if (entry.success) StatusTone.Success else StatusTone.Error)
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerInventoryCard(
    item: InventoryItemDTO,
    isBusy: Boolean,
    onToggleAutoRestock: () -> Unit,
    onOpenDetail: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAutoRestock: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InventoryCardTop(
                title = item.name,
                subtitle = item.supplier,
                statusText = inventoryStatusLabel(item),
                statusTone = inventoryStatusTone(item),
                onMore = onOpenDetail,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InventoryMetricLine(label = "Stock:", value = item.currentStock.toString())
                    InventoryMetricLine(label = "SKU:", value = shortId(item.id).uppercase())
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InventoryMetricLine(label = "MOQ:", value = item.minimumOrderQuantity.coerceAtLeast(1).toString())
                    InventoryMetricLine(label = "Min:", value = item.autoOrderConfig?.minThreshold?.toString() ?: "0")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(InventorySoftLine, RoundedCornerShape(999.dp)),
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = HistoryPanel,
                border = BorderStroke(1.dp, InventoryBorder),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = InventoryMuted, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (isBusy) "Updating..." else "Auto-Restock",
                            style = MaterialTheme.typography.titleSmall,
                            color = InventoryText,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                        Text(
                            text = "Automatically reorder when stock is low",
                            style = MaterialTheme.typography.bodySmall,
                            color = InventoryMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    InventoryToggle(
                        checked = item.autoRestock,
                        enabled = !isBusy,
                        onClick = onToggleAutoRestock,
                    )
                }
            }

            if (item.autoRestock) {
                InventoryAutoRestockPanel(item = item)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InventoryActionButton(
                    text = "History",
                    icon = Icons.Default.History,
                    onClick = onOpenHistory,
                    modifier = Modifier.weight(1f),
                )
                InventoryActionButton(
                    text = "Configure",
                    icon = Icons.Default.Settings,
                    onClick = onOpenAutoRestock,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CatalogStockCard(
    product: ProductDTO,
    isBusy: Boolean,
    onOpenDetail: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isBusy, onClick = onOpenDetail),
        shape = RoundedCornerShape(14.dp),
        color = InventoryCardBg,
        border = BorderStroke(1.dp, InventoryBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InventoryCardTop(
                title = product.name,
                subtitle = product.brand.ifBlank { product.category.ifBlank { "Catalog product" } },
                statusText = stockStatusLabel(product),
                statusTone = stockStatusTone(product),
                onMore = onOpenDetail,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InventoryMetricLine(label = "Stock:", value = product.stock.toString())
                    InventoryMetricLine(label = "GTIN:", value = product.gtin.ifBlank { "Not set" })
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    InventoryMetricLine(label = "MOQ:", value = product.minimumOrderQuantity.coerceAtLeast(1).toString())
                    InventoryMetricLine(label = "State:", value = if (product.published) "Live" else "Draft")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InventoryActionButton(
                    text = if (isBusy) "Updating..." else "Update Stock",
                    icon = Icons.Default.Settings,
                    onClick = onOpenDetail,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun InventoryCardTop(
    title: String,
    subtitle: String,
    statusText: String,
    statusTone: StatusTone,
    onMore: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        InventoryProductTile(modifier = Modifier.size(74.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = InventoryText,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = InventoryMuted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            StatusChip(text = statusText, tone = statusTone)
        }
        IconButton(onClick = onMore, modifier = Modifier.size(34.dp)) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = InventoryMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun InventoryProductTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(InventoryIconBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Inventory2,
            contentDescription = null,
            tint = InventoryMuted,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
private fun InventoryMetricLine(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InventoryMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = InventoryText,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InventoryToggle(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(50.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (checked) InventorySuccess else Color(0xFF607286),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White, RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun InventoryAutoRestockPanel(item: InventoryItemDTO) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF0D3C34).copy(alpha = 0.72f),
        border = BorderStroke(1.dp, InventorySuccess.copy(alpha = 0.38f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(InventorySuccess, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Text("Active Configuration", style = MaterialTheme.typography.titleSmall, color = InventoryText, fontWeight = FontWeight.Black)
            }
            InventoryConfigTextLine("Reorder Quantity", "${item.reorderQuantity ?: item.autoOrderConfig?.reorderQuantity ?: 0} units")
            InventoryConfigTextLine("Trigger Threshold", "${item.autoOrderConfig?.minThreshold ?: 0} units")
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(InventorySuccess.copy(alpha = 0.24f), RoundedCornerShape(999.dp)))
            InventoryConfigTextLine("Next Restock Date", nextRestockLabel(item) ?: scheduleSummary(item), compact = true)
        }
    }
}

@Composable
private fun InventoryConfigTextLine(label: String, value: String, compact: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = Color(0xFF71E8C1), fontWeight = FontWeight.SemiBold)
        Text(value, style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium, color = InventoryText, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InventoryConfigLine(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InventoryMuted, fontWeight = FontWeight.SemiBold)
        Text(value.toString(), style = MaterialTheme.typography.bodyMedium, color = InventoryText, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun InventoryActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, InventoryBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = InventoryText, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = InventoryText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InventoryDetailContent(
    title: String,
    subtitle: String,
    currentStock: Int,
    minimumOrderQuantity: Int,
    statusText: String,
    statusTone: StatusTone,
    stockDraft: String,
    isSaving: Boolean,
    errorMessage: String?,
    onStockDraftChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    ScreenSection(title = title, subtitle = subtitle) {
        AppCard {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Current stock", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(currentStock.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                StatusChip(text = statusText, tone = statusTone)
            }
            StatusChip(text = "MOQ ${minimumOrderQuantity.coerceAtLeast(1)}", tone = StatusTone.Neutral)
        }

        if (errorMessage != null) {
            ErrorStateCard(title = "Stock update failed", message = errorMessage)
        }

        AppCard {
            Text("Update stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Client validation prevents negative stock, then the backend remains the source of truth.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppTextField(
                value = stockDraft,
                onValueChange = onStockDraftChange,
                label = "New stock quantity",
                keyboardType = KeyboardType.Number,
            )
            PrimaryButton(
                text = if (isSaving) "Saving..." else "Save stock",
                enabled = !isSaving && stockDraft.isNotBlank(),
                onClick = onSave,
            )
        }
    }
}

@Composable
private fun ManagedProductCard(
    product: ProductDTO,
    isBusy: Boolean,
    confirmDelete: Boolean,
    onEdit: () -> Unit,
    onTogglePublished: () -> Unit,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            ProductTile(label = product.category.ifBlank { "Product" }, modifier = Modifier.size(72.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(product.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatusChip(text = if (product.published) "Published" else "Draft", tone = if (product.published) StatusTone.Success else StatusTone.Neutral)
                    StatusChip(text = stockStatusLabel(product), tone = stockStatusTone(product))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(formatMoney(product.price), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
            Text("MOQ ${product.minimumOrderQuantity.coerceAtLeast(1)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(text = "Edit", onClick = onEdit, modifier = Modifier.weight(1f), enabled = !isBusy)
            SecondaryButton(
                text = if (product.published) "Unpublish" else "Publish",
                onClick = onTogglePublished,
                modifier = Modifier.weight(1f),
                enabled = !isBusy,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(
                text = if (confirmDelete) "Confirm delete" else "Delete",
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                enabled = !isBusy,
            )
            if (confirmDelete) {
                SecondaryButton(text = "Cancel", onClick = onCancelDelete, modifier = Modifier.weight(1f), enabled = !isBusy)
            }
        }
    }
}

@Composable
private fun CategoryPicker(
    categories: List<CategoryDTO>,
    selectedCategoryId: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        if (categories.isEmpty()) {
            StatusChip(text = "No categories loaded", tone = StatusTone.Warning)
        }
        categories.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { category ->
                    SecondaryButton(
                        text = if (category.id == selectedCategoryId) "${category.name} selected" else category.name,
                        onClick = { onSelected(category.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InventoryCardHeader(
    title: String,
    subtitle: String,
    stock: Int,
    statusText: String,
    statusTone: StatusTone,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        ProductTile(label = title.take(1).uppercase(), modifier = Modifier.size(72.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            StatusChip(text = statusText, tone = statusTone)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(stock.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductTile(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

@Composable
private fun OperationsHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun inventoryStatusLabel(item: InventoryItemDTO): String {
    return when {
        item.currentStock <= 0 || item.status == InventoryStatus.OUT_OF_STOCK -> "Out of stock"
        item.status == InventoryStatus.LOW_STOCK -> "Low stock"
        else -> "Optimal"
    }
}

private fun inventoryStatusTone(item: InventoryItemDTO): StatusTone {
    return when {
        item.currentStock <= 0 || item.status == InventoryStatus.OUT_OF_STOCK -> StatusTone.Error
        item.status == InventoryStatus.LOW_STOCK -> StatusTone.Warning
        else -> StatusTone.Success
    }
}

private fun nextRestockLabel(item: InventoryItemDTO): String? {
    return item.autoOrderConfig?.nextScheduledAt
        ?.takeIf { item.autoRestock && it.isNotBlank() }
        ?.take(10)
}

private fun scheduleSummary(item: InventoryItemDTO): String {
    val config = item.autoOrderConfig ?: return "Not scheduled"
    return when (config.scheduleType) {
        ScheduleType.THRESHOLD_BASED -> "Triggers below ${config.minThreshold} units"
        ScheduleType.DAILY -> "Daily restock check"
        ScheduleType.WEEKLY -> "Weekly${config.dayOfWeek?.let { " on ${it.lowercase().replaceFirstChar { char -> char.uppercase() }}" } ?: ""}"
        ScheduleType.MONTHLY -> "Monthly${config.dayOfMonth?.let { " on day $it" } ?: ""}"
        ScheduleType.INTERVAL_DAYS -> "Every ${config.intervalDays ?: 0} days"
    }
}

private fun stockStatusLabel(product: ProductDTO): String {
    val moq = product.minimumOrderQuantity.coerceAtLeast(1)
    return when {
        product.stock <= 0 -> "Out of stock"
        product.stock < moq -> "Below MOQ"
        product.stock < 10 -> "Low stock"
        else -> "In stock"
    }
}

private fun stockStatusTone(product: ProductDTO): StatusTone {
    val moq = product.minimumOrderQuantity.coerceAtLeast(1)
    return when {
        product.stock <= 0 || product.stock < moq -> StatusTone.Error
        product.stock < 10 -> StatusTone.Warning
        else -> StatusTone.Success
    }
}

private fun formatMoney(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    val text = rounded.toString()
    val decimals = text.substringAfter('.', "")
    return "SAR " + when (decimals.length) {
        0 -> "$text.00"
        1 -> "${text}0"
        else -> text
    }
}

private fun shortId(id: String): String = id.take(8).ifBlank { "pending" }

private fun digitsOnly(value: String): String = value.filter { it.isDigit() }

private fun decimalInput(value: String): String {
    var dotSeen = false
    return value.filter { char ->
        when {
            char.isDigit() -> true
            char == '.' && !dotSeen -> {
                dotSeen = true
                true
            }
            else -> false
        }
    }
}
