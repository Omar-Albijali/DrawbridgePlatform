package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.CategoryDTO
import uqu.drawbridge.platform.InventoryItemDTO
import uqu.drawbridge.platform.InventoryStatus
import uqu.drawbridge.platform.ProductDTO
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
import uqu.drawbridge.platform.ui.operations.InventoryMode
import uqu.drawbridge.platform.ui.operations.InventoryStateHolder
import uqu.drawbridge.platform.ui.operations.PosStateHolder
import uqu.drawbridge.platform.ui.operations.ProductManagementStateHolder

@Composable
internal fun InventoryMainScreen(
    inventoryStateHolder: InventoryStateHolder,
    onOpenDetail: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = inventoryStateHolder.state

    LaunchedEffect(Unit) {
        inventoryStateHolder.loadInitial()
    }

    ScreenSection(
        title = if (state.mode == InventoryMode.CatalogStock) "Inventory" else "Retail inventory",
        subtitle = if (state.mode == InventoryMode.CatalogStock) {
            "Track owned catalog stock and low-stock products."
        } else {
            "Track live retailer stock and POS-driven quantity changes."
        },
    ) {
        when {
            state.isLoading -> {
                repeat(3) {
                    LoadingStateCard(title = "Loading inventory", message = "Checking current stock levels.")
                }
                return@ScreenSection
            }
            state.errorMessage != null && state.totalCount == 0 -> {
                ErrorStateCard(
                    title = "Could not load inventory",
                    message = state.errorMessage,
                    actionText = "Try again",
                    onAction = { coroutineScope.launch { inventoryStateHolder.refresh() } },
                )
                return@ScreenSection
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(value = state.totalCount.toString(), label = "Items", modifier = Modifier.weight(1f))
            StatCard(value = state.lowStockCount.toString(), label = "Low", modifier = Modifier.weight(1f))
            StatCard(value = state.outOfStockCount.toString(), label = "Out", modifier = Modifier.weight(1f))
        }

        AppCard {
            AppTextField(
                value = state.searchInput,
                onValueChange = inventoryStateHolder::updateSearchInput,
                label = "Search inventory",
            )
            SecondaryButton(
                text = if (state.isRefreshing) "Refreshing..." else "Refresh",
                onClick = { coroutineScope.launch { inventoryStateHolder.refresh() } },
                enabled = !state.isRefreshing,
            )
        }

        if (state.errorMessage != null) {
            ErrorStateCard(
                title = "Inventory needs attention",
                message = state.errorMessage,
                actionText = "Refresh",
                onAction = { coroutineScope.launch { inventoryStateHolder.refresh() } },
            )
        }

        if (state.mode == InventoryMode.CatalogStock) {
            if (state.filteredCatalogProducts.isEmpty()) {
                EmptyStateCard(
                    title = "No products found",
                    message = if (state.catalogProducts.isEmpty()) "Create products before managing catalog stock." else "Try a different search.",
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
                EmptyStateCard(
                    title = "No inventory items found",
                    message = if (state.inventoryItems.isEmpty()) "Inventory entries will appear when products are stocked." else "Try a different search.",
                )
            } else {
                state.filteredInventoryItems.forEach { item ->
                    RetailerInventoryCard(
                        item = item,
                        isBusy = item.id in state.busyItemIds,
                        onOpenDetail = { onOpenDetail(item.id) },
                    )
                }
            }
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
    onOpenDetail: () -> Unit,
) {
    AppCard(modifier = Modifier.clickable(onClick = onOpenDetail)) {
        InventoryCardHeader(
            title = item.name,
            subtitle = item.supplier,
            stock = item.currentStock,
            statusText = inventoryStatusLabel(item),
            statusTone = inventoryStatusTone(item),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatusChip(text = "MOQ ${item.minimumOrderQuantity.coerceAtLeast(1)}", tone = StatusTone.Neutral)
            StatusChip(text = "Reorder ${item.reorderQuantity ?: item.autoOrderConfig?.reorderQuantity ?: 0}", tone = StatusTone.Neutral)
        }
        if (item.autoRestock) {
            StatusChip(text = "Auto restock on", tone = StatusTone.Success)
        }
        SecondaryButton(text = if (isBusy) "Updating..." else "View and update", onClick = onOpenDetail, enabled = !isBusy)
    }
}

@Composable
private fun CatalogStockCard(
    product: ProductDTO,
    isBusy: Boolean,
    onOpenDetail: () -> Unit,
) {
    AppCard(modifier = Modifier.clickable(onClick = onOpenDetail)) {
        InventoryCardHeader(
            title = product.name,
            subtitle = "${product.category.ifBlank { "Uncategorized" }} • GTIN ${product.gtin.ifBlank { "Not set" }}",
            stock = product.stock,
            statusText = stockStatusLabel(product),
            statusTone = stockStatusTone(product),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatusChip(text = "MOQ ${product.minimumOrderQuantity.coerceAtLeast(1)}", tone = StatusTone.Neutral)
            StatusChip(text = if (product.published) "Published" else "Draft", tone = if (product.published) StatusTone.Success else StatusTone.Neutral)
        }
        SecondaryButton(text = if (isBusy) "Updating..." else "View and update", onClick = onOpenDetail, enabled = !isBusy)
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
