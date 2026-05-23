package uqu.drawbridge.platform.ui.operations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uqu.drawbridge.platform.CategoryDTO
import uqu.drawbridge.platform.CreateInventoryItemRequest
import uqu.drawbridge.platform.CreateProductRequest
import uqu.drawbridge.platform.InventoryAuditLogDTO
import uqu.drawbridge.platform.InventoryItemDTO
import uqu.drawbridge.platform.InventoryStatus
import uqu.drawbridge.platform.MarketplaceProductQuery
import uqu.drawbridge.platform.MobileAuthApi
import uqu.drawbridge.platform.PosIntegrationApiKeyRotateResponse
import uqu.drawbridge.platform.PosIntegrationConfigDTO
import uqu.drawbridge.platform.PosIntegrationConfigUpdateRequest
import uqu.drawbridge.platform.PosIntegrationEventLogDTO
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.ProductImageResponse
import uqu.drawbridge.platform.ScheduleType
import uqu.drawbridge.platform.UpdateAutoOrderConfigRequest
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.auth.AuthActionResult
import uqu.drawbridge.platform.ui.common.userReadableMessage
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.platform.PickedFile

internal enum class InventoryMode {
    RetailerInventory,
    CatalogStock,
}

internal data class InventoryUiState(
    val mode: InventoryMode,
    val inventoryItems: List<InventoryItemDTO> = emptyList(),
    val catalogProducts: List<ProductDTO> = emptyList(),
    val categories: List<CategoryDTO> = emptyList(),
    val searchInput: String = "",
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val busyItemIds: Set<String> = emptySet(),
    val addInventoryProducts: List<ProductDTO> = emptyList(),
    val addInventorySearchInput: String = "",
    val selectedAddProduct: ProductDTO? = null,
    val addStockDraft: String = "0",
    val addThresholdDraft: String = "10",
    val addAutoRestock: Boolean = false,
    val isLoadingAddProducts: Boolean = false,
    val isSubmittingAddInventory: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val filteredInventoryItems: List<InventoryItemDTO>
        get() {
            val search = searchInput.trim()
            return inventoryItems.filter { item ->
                search.isEmpty() ||
                    item.name.contains(search, ignoreCase = true) ||
                    item.supplier.contains(search, ignoreCase = true)
            }
        }

    val filteredCatalogProducts: List<ProductDTO>
        get() {
            val search = searchInput.trim()
            return catalogProducts.filter { product ->
                search.isEmpty() ||
                    product.name.contains(search, ignoreCase = true) ||
                    product.category.contains(search, ignoreCase = true) ||
                    product.gtin.contains(search, ignoreCase = true)
            }
        }

    val totalCount: Int
        get() = if (mode == InventoryMode.RetailerInventory) inventoryItems.size else catalogProducts.size

    val lowStockCount: Int
        get() = when (mode) {
            InventoryMode.RetailerInventory -> inventoryItems.count { it.status == InventoryStatus.LOW_STOCK }
            InventoryMode.CatalogStock -> catalogProducts.count { it.stock in 1..9 || it.stock < it.minimumOrderQuantity.coerceAtLeast(1) }
        }

    val outOfStockCount: Int
        get() = when (mode) {
            InventoryMode.RetailerInventory -> inventoryItems.count { it.status == InventoryStatus.OUT_OF_STOCK || it.currentStock <= 0 }
            InventoryMode.CatalogStock -> catalogProducts.count { it.stock <= 0 }
        }

    val autoRestockCount: Int
        get() = when (mode) {
            InventoryMode.RetailerInventory -> inventoryItems.count { it.autoRestock }
            InventoryMode.CatalogStock -> 0
        }

    val filteredAddInventoryProducts: List<ProductDTO>
        get() {
            val search = addInventorySearchInput.trim()
            return addInventoryProducts.filter { product ->
                search.isEmpty() ||
                    product.name.contains(search, ignoreCase = true) ||
                    product.brand.contains(search, ignoreCase = true) ||
                    product.supplier.contains(search, ignoreCase = true) ||
                    product.category.contains(search, ignoreCase = true)
            }
        }
}

internal data class InventoryDetailUiState(
    val inventoryItem: InventoryItemDTO? = null,
    val catalogProduct: ProductDTO? = null,
    val stockDraft: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

internal data class InventoryHistoryUiState(
    val item: InventoryItemDTO? = null,
    val logs: List<InventoryAuditLogDTO> = emptyList(),
    val page: Int = 0,
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasMore: Boolean
        get() = page + 1 < totalPages
}

internal data class AutoRestockUiState(
    val item: InventoryItemDTO? = null,
    val scheduleType: ScheduleType = ScheduleType.THRESHOLD_BASED,
    val minThresholdDraft: String = "10",
    val reorderQuantityDraft: String = "10",
    val intervalDaysDraft: String = "7",
    val dayOfWeek: String = "MONDAY",
    val dayOfMonthDraft: String = "1",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

internal class InventoryStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    private val mode = if (session.user.role == UserRole.RETAILER) {
        InventoryMode.RetailerInventory
    } else {
        InventoryMode.CatalogStock
    }

    var state: InventoryUiState by mutableStateOf(InventoryUiState(mode = mode))
        private set

    var detailState: InventoryDetailUiState by mutableStateOf(InventoryDetailUiState())
        private set

    var historyState: InventoryHistoryUiState by mutableStateOf(InventoryHistoryUiState())
        private set

    var autoRestockState: AutoRestockUiState by mutableStateOf(AutoRestockUiState())
        private set

    suspend fun fetchImageBytes(imageUrl: String): ByteArray = api.fetchImageBytes(imageUrl)

    suspend fun loadInitial() {
        if (state.hasLoaded || state.isLoading) return
        refresh()
    }

    suspend fun refresh() {
        state = state.copy(
            isLoading = !state.hasLoaded,
            isRefreshing = state.hasLoaded,
            errorMessage = null,
            actionMessage = null,
        )

        runCatching {
            val categories = if (session.user.role == UserRole.WHOLESALER) {
                runCatching { api.fetchProductCategories() }.getOrDefault(state.categories)
            } else {
                state.categories
            }
            when (mode) {
                InventoryMode.RetailerInventory -> state.copy(
                    inventoryItems = api.fetchInventoryByRetailer(session.user.id),
                    categories = categories,
                )
                InventoryMode.CatalogStock -> state.copy(
                    catalogProducts = api.fetchProductsByWholesaler(session.user.id),
                    categories = categories,
                )
            }
        }.fold(
            onSuccess = { loaded ->
                state = loaded.copy(
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = userReadableMessage(error, "Unable to load inventory right now."),
                )
            },
        )
    }

    fun updateSearchInput(value: String) {
        state = state.copy(searchInput = value)
    }

    fun updateAddInventorySearchInput(value: String) {
        state = state.copy(addInventorySearchInput = value)
    }

    fun selectAddInventoryProduct(product: ProductDTO) {
        state = state.copy(
            selectedAddProduct = product,
            addStockDraft = "0",
            addThresholdDraft = "10",
            addAutoRestock = false,
        )
    }

    fun clearAddInventorySelection() {
        state = state.copy(selectedAddProduct = null)
    }

    fun resetAddInventoryForm() {
        state = state.copy(
            addInventorySearchInput = "",
            selectedAddProduct = null,
            addStockDraft = "0",
            addThresholdDraft = "10",
            addAutoRestock = false,
            isSubmittingAddInventory = false,
        )
    }

    fun updateAddStockDraft(value: String) {
        state = state.copy(addStockDraft = value.filter { it.isDigit() })
    }

    fun updateAddThresholdDraft(value: String) {
        state = state.copy(addThresholdDraft = value.filter { it.isDigit() })
    }

    fun toggleAddAutoRestock() {
        state = state.copy(addAutoRestock = !state.addAutoRestock)
    }

    suspend fun loadAddInventoryProducts(force: Boolean = false) {
        if (mode != InventoryMode.RetailerInventory) return
        if (!force && state.addInventoryProducts.isNotEmpty()) return
        state = state.copy(isLoadingAddProducts = true, errorMessage = null)
        runCatching {
            api.fetchMarketplaceProducts(MarketplaceProductQuery(size = 50)).content
        }.fold(
            onSuccess = { products ->
                state = state.copy(addInventoryProducts = products, isLoadingAddProducts = false)
            },
            onFailure = { error ->
                state = state.copy(
                    isLoadingAddProducts = false,
                    errorMessage = userReadableMessage(error, "Unable to load marketplace products."),
                )
            },
        )
    }

    suspend fun createInventoryItem(): AuthActionResult {
        if (mode != InventoryMode.RetailerInventory) {
            return AuthActionResult(success = false, message = "Inventory creation is available for retailer accounts.")
        }
        val product = state.selectedAddProduct
        if (product == null) {
            return AuthActionResult(success = false, message = "Select a product first.")
        }
        val currentStock = state.addStockDraft.toIntOrNull()
        val minThreshold = state.addThresholdDraft.toIntOrNull()
        val validationMessage = when {
            currentStock == null || currentStock < 0 -> "Stock must be zero or higher."
            minThreshold == null || minThreshold < 0 -> "Minimum threshold must be zero or higher."
            else -> null
        }
        if (validationMessage != null) {
            state = state.copy(errorMessage = validationMessage)
            return AuthActionResult(success = false, message = validationMessage)
        }
        val validatedStock = currentStock ?: 0
        val validatedThreshold = minThreshold ?: 0

        state = state.copy(isSubmittingAddInventory = true, errorMessage = null)
        return runCatching {
            api.createInventoryItem(
                CreateInventoryItemRequest(
                    productId = product.id,
                    retailerId = session.user.id,
                    currentStock = validatedStock,
                    minThreshold = validatedThreshold,
                    autoRestock = state.addAutoRestock,
                ),
            )
        }.fold(
            onSuccess = {
                resetAddInventoryForm()
                refresh()
                state = state.copy(actionMessage = "Product added to inventory.")
                AuthActionResult(success = true, message = "Product added to inventory.")
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to add product to inventory.")
                state = state.copy(isSubmittingAddInventory = false, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }

    suspend fun toggleAutoRestock(item: InventoryItemDTO): AuthActionResult {
        if (mode != InventoryMode.RetailerInventory) {
            return AuthActionResult(success = false, message = "Auto-restock is available for retailer inventory.")
        }

        val enabled = !item.autoRestock
        val originalItems = state.inventoryItems
        state = state.copy(
            inventoryItems = state.inventoryItems.map { current ->
                if (current.id == item.id) {
                    current.copy(
                        autoRestock = enabled,
                        autoOrderConfig = current.autoOrderConfig?.copy(enabled = enabled),
                    )
                } else {
                    current
                }
            },
            busyItemIds = state.busyItemIds + item.id,
            errorMessage = null,
            actionMessage = null,
        )

        return runCatching {
            api.toggleAutoOrderConfig(item.id, enabled)
        }.fold(
            onSuccess = { config ->
                state = state.copy(
                    inventoryItems = state.inventoryItems.map { current ->
                        if (current.id == item.id) {
                            current.copy(autoRestock = config.enabled, autoOrderConfig = config)
                        } else {
                            current
                        }
                    },
                    busyItemIds = state.busyItemIds - item.id,
                    actionMessage = if (config.enabled) "Auto-restock enabled." else "Auto-restock disabled.",
                )
                AuthActionResult(
                    success = true,
                    message = if (config.enabled) "Auto-restock enabled." else "Auto-restock disabled.",
                )
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to update auto-restock.")
                state = state.copy(
                    inventoryItems = originalItems,
                    busyItemIds = state.busyItemIds - item.id,
                    errorMessage = message,
                )
                AuthActionResult(success = false, message = message)
            },
        )
    }

    suspend fun openStockHistory(item: InventoryItemDTO) {
        historyState = InventoryHistoryUiState(item = item, isLoading = true)
        loadStockHistoryPage(item = item, page = 0, replace = true)
    }

    fun closeStockHistory() {
        historyState = InventoryHistoryUiState()
    }

    fun openAutoRestockConfig(item: InventoryItemDTO) {
        val minimumOrderQuantity = item.minimumOrderQuantity.coerceAtLeast(1)
        val config = item.autoOrderConfig
        autoRestockState = AutoRestockUiState(
            item = item,
            scheduleType = config?.scheduleType ?: ScheduleType.THRESHOLD_BASED,
            minThresholdDraft = (config?.minThreshold ?: 10).toString(),
            reorderQuantityDraft = (config?.reorderQuantity ?: item.reorderQuantity ?: minimumOrderQuantity)
                .coerceAtLeast(minimumOrderQuantity)
                .toString(),
            intervalDaysDraft = (config?.intervalDays ?: 7).toString(),
            dayOfWeek = config?.dayOfWeek ?: "MONDAY",
            dayOfMonthDraft = config?.dayOfMonth ?: "1",
        )
    }

    fun closeAutoRestockConfig() {
        autoRestockState = AutoRestockUiState()
    }

    fun selectAutoRestockSchedule(scheduleType: ScheduleType) {
        autoRestockState = autoRestockState.copy(scheduleType = scheduleType, errorMessage = null)
    }

    fun updateAutoRestockThreshold(value: String) {
        autoRestockState = autoRestockState.copy(minThresholdDraft = value.filter { it.isDigit() }, errorMessage = null)
    }

    fun updateAutoRestockQuantity(value: String) {
        autoRestockState = autoRestockState.copy(reorderQuantityDraft = value.filter { it.isDigit() }, errorMessage = null)
    }

    fun updateAutoRestockInterval(value: String) {
        autoRestockState = autoRestockState.copy(intervalDaysDraft = value.filter { it.isDigit() }, errorMessage = null)
    }

    fun updateAutoRestockDayOfWeek(value: String) {
        autoRestockState = autoRestockState.copy(dayOfWeek = value, errorMessage = null)
    }

    fun updateAutoRestockDayOfMonth(value: String) {
        autoRestockState = autoRestockState.copy(dayOfMonthDraft = value.filter { it.isDigit() }, errorMessage = null)
    }

    suspend fun saveAutoRestockConfig(): AuthActionResult {
        val item = autoRestockState.item ?: return AuthActionResult(false, "No inventory item selected.")
        val minimumOrderQuantity = item.minimumOrderQuantity.coerceAtLeast(1)
        val threshold = autoRestockState.minThresholdDraft.toIntOrNull() ?: 0
        val reorderQuantity = autoRestockState.reorderQuantityDraft.toIntOrNull()
        val intervalDays = autoRestockState.intervalDaysDraft.toIntOrNull()
        val dayOfMonth = autoRestockState.dayOfMonthDraft.toIntOrNull()
        val validationMessage = when {
            reorderQuantity == null || reorderQuantity < minimumOrderQuantity -> "Reorder quantity must be at least $minimumOrderQuantity."
            autoRestockState.scheduleType == ScheduleType.INTERVAL_DAYS && (intervalDays == null || intervalDays < 1) -> "Interval must be at least 1 day."
            autoRestockState.scheduleType == ScheduleType.MONTHLY && (dayOfMonth == null || dayOfMonth !in 1..28) -> "Day of month must be 1-28."
            else -> null
        }
        if (validationMessage != null) {
            autoRestockState = autoRestockState.copy(errorMessage = validationMessage)
            return AuthActionResult(false, validationMessage)
        }
        val validatedReorderQuantity = reorderQuantity ?: minimumOrderQuantity
        val validatedIntervalDays = intervalDays ?: 1
        val validatedDayOfMonth = dayOfMonth ?: 1

        autoRestockState = autoRestockState.copy(isSaving = true, errorMessage = null)
        state = state.copy(busyItemIds = state.busyItemIds + item.id)
        return runCatching {
            api.updateAutoOrderConfig(
                inventoryItemId = item.id,
                request = UpdateAutoOrderConfigRequest(
                    enabled = true,
                    minThreshold = threshold,
                    reorderQuantity = validatedReorderQuantity,
                    scheduleType = autoRestockState.scheduleType,
                    intervalDays = if (autoRestockState.scheduleType == ScheduleType.INTERVAL_DAYS) validatedIntervalDays else null,
                    dayOfWeek = if (autoRestockState.scheduleType == ScheduleType.WEEKLY) autoRestockState.dayOfWeek else null,
                    dayOfMonth = if (autoRestockState.scheduleType == ScheduleType.MONTHLY) validatedDayOfMonth.toString() else null,
                ),
            )
        }.fold(
            onSuccess = { config ->
                state = state.copy(
                    inventoryItems = state.inventoryItems.map { current ->
                        if (current.id == item.id) {
                            current.copy(autoRestock = true, autoOrderConfig = config, reorderQuantity = config.reorderQuantity)
                        } else {
                            current
                        }
                    },
                    busyItemIds = state.busyItemIds - item.id,
                    actionMessage = "Auto-restock configuration saved.",
                )
                autoRestockState = autoRestockState.copy(isSaving = false)
                AuthActionResult(true, "Auto-restock configuration saved.")
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to save auto-restock configuration.")
                autoRestockState = autoRestockState.copy(isSaving = false, errorMessage = message)
                state = state.copy(busyItemIds = state.busyItemIds - item.id, errorMessage = message)
                AuthActionResult(false, message)
            },
        )
    }

    suspend fun reloadStockHistory() {
        val item = historyState.item ?: return
        loadStockHistoryPage(item = item, page = 0, replace = true)
    }

    suspend fun loadMoreStockHistory() {
        val item = historyState.item ?: return
        if (!historyState.hasMore || historyState.isLoadingMore || historyState.isLoading) return
        loadStockHistoryPage(item = item, page = historyState.page + 1, replace = false)
    }

    private suspend fun loadStockHistoryPage(
        item: InventoryItemDTO,
        page: Int,
        replace: Boolean,
    ) {
        historyState = historyState.copy(
            isLoading = replace,
            isLoadingMore = !replace,
            errorMessage = null,
        )

        runCatching {
            api.fetchInventoryAuditLogs(
                inventoryItemId = item.id,
                stockTargetType = "RETAILER_INVENTORY",
                page = page,
                size = 10,
            )
        }.fold(
            onSuccess = { result ->
                if (historyState.item?.id != item.id) return@fold
                historyState = historyState.copy(
                    logs = if (replace) result.items else historyState.logs + result.items,
                    page = result.page,
                    totalElements = result.totalElements,
                    totalPages = result.totalPages,
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                if (historyState.item?.id != item.id) return@fold
                historyState = historyState.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = userReadableMessage(error, "Unable to load stock history."),
                )
            },
        )
    }

    suspend fun loadDetail(itemId: String) {
        detailState = InventoryDetailUiState(isLoading = true)
        when (mode) {
            InventoryMode.RetailerInventory -> runCatching { api.fetchInventoryItem(itemId) }.fold(
                onSuccess = { item ->
                    detailState = InventoryDetailUiState(
                        inventoryItem = item,
                        stockDraft = item.currentStock.toString(),
                    )
                },
                onFailure = { error ->
                    detailState = InventoryDetailUiState(
                        errorMessage = userReadableMessage(error, "Unable to load inventory item."),
                    )
                },
            )
            InventoryMode.CatalogStock -> {
                val product = state.catalogProducts.firstOrNull { it.id == itemId }
                    ?: runCatching { api.fetchProductById(itemId) }.getOrNull()
                detailState = if (product != null) {
                    InventoryDetailUiState(
                        catalogProduct = product,
                        stockDraft = product.stock.toString(),
                    )
                } else {
                    InventoryDetailUiState(errorMessage = "Unable to load product stock detail.")
                }
            }
        }
    }

    fun updateStockDraft(value: String) {
        detailState = detailState.copy(stockDraft = value.filter { it.isDigit() })
    }

    suspend fun saveStock(): AuthActionResult {
        val quantity = detailState.stockDraft.toIntOrNull()
        if (quantity == null || quantity < 0) {
            val message = "Stock quantity must be zero or higher."
            detailState = detailState.copy(errorMessage = message)
            return AuthActionResult(success = false, message = message)
        }

        val item = detailState.inventoryItem
        val product = detailState.catalogProduct
        val targetId = item?.id ?: product?.id
        if (targetId == null) {
            val message = "No inventory item is selected."
            detailState = detailState.copy(errorMessage = message)
            return AuthActionResult(success = false, message = message)
        }

        detailState = detailState.copy(isSaving = true, errorMessage = null)
        state = state.copy(busyItemIds = state.busyItemIds + targetId)

        return runCatching {
            if (item != null) {
                api.updateInventoryQuantity(item.id, quantity)
            } else {
                val selectedProduct = requireNotNull(product)
                val categoryId = categoryIdFor(selectedProduct)
                    ?: throw IllegalArgumentException("Select a valid category before updating this product.")
                api.updateProduct(
                    selectedProduct.id,
                    selectedProduct.toProductRequest(
                        wholesalerId = session.user.id,
                        categoryId = categoryId,
                        stock = quantity,
                    ),
                )
            }
        }.fold(
            onSuccess = { updated ->
                refresh()
                if (updated is InventoryItemDTO) {
                    detailState = InventoryDetailUiState(
                        inventoryItem = updated,
                        stockDraft = updated.currentStock.toString(),
                    )
                } else if (updated is ProductDTO) {
                    detailState = InventoryDetailUiState(
                        catalogProduct = updated,
                        stockDraft = updated.stock.toString(),
                    )
                }
                state = state.copy(
                    busyItemIds = state.busyItemIds - targetId,
                    actionMessage = "Stock updated.",
                )
                AuthActionResult(success = true, message = "Stock updated.")
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to update stock.")
                detailState = detailState.copy(isSaving = false, errorMessage = message)
                state = state.copy(busyItemIds = state.busyItemIds - targetId, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }

    private fun categoryIdFor(product: ProductDTO): String? {
        return state.categories.firstOrNull { it.name == product.category }?.id
    }
}

internal data class ProductFormUiState(
    val productId: String? = null,
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val stock: String = "0",
    val minimumOrderQuantity: String = "1",
    val gtin: String = "",
    val categoryId: String = "",
    val published: Boolean = true,
    val images: List<ProductFormImage> = emptyList(),
    val removedImageIds: Set<String> = emptySet(),
    val isLoadingImages: Boolean = false,
    val imageErrorMessage: String? = null,
    val rating: Double = 0.0,
    val reviews: Int = 0,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val isEditing: Boolean = productId != null
}

internal data class ProductFormImage(
    val localId: String,
    val remoteId: String? = null,
    val url: String? = null,
    val name: String = "",
    val mimeType: String? = null,
    val bytes: ByteArray? = null,
    val sortIndex: Int = 0,
) {
    val isNew: Boolean
        get() = bytes != null
}

internal data class ProductManagementUiState(
    val products: List<ProductDTO> = emptyList(),
    val categories: List<CategoryDTO> = emptyList(),
    val searchInput: String = "",
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val busyProductIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val filteredProducts: List<ProductDTO>
        get() {
            val search = searchInput.trim()
            return products.filter { product ->
                search.isEmpty() ||
                    product.name.contains(search, ignoreCase = true) ||
                    product.category.contains(search, ignoreCase = true) ||
                    product.gtin.contains(search, ignoreCase = true)
            }
        }
}

internal class ProductManagementStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: ProductManagementUiState by mutableStateOf(ProductManagementUiState())
        private set

    var formState: ProductFormUiState by mutableStateOf(ProductFormUiState())
        private set

    val isEnabled: Boolean = session.user.role == UserRole.WHOLESALER

    val brandName: String
        get() = session.user.company.ifBlank { "Your company" }

    suspend fun fetchImageBytes(imageUrl: String): ByteArray = api.fetchImageBytes(imageUrl)

    suspend fun loadInitial() {
        if (state.hasLoaded || state.isLoading) return
        refresh()
    }

    suspend fun refresh() {
        if (!isEnabled) {
            state = ProductManagementUiState(
                hasLoaded = true,
                errorMessage = "Product management is available for wholesaler accounts.",
            )
            return
        }

        state = state.copy(
            isLoading = !state.hasLoaded,
            isRefreshing = state.hasLoaded,
            errorMessage = null,
            actionMessage = null,
        )

        runCatching {
            val categories = runCatching { api.fetchProductCategories() }.getOrDefault(state.categories)
            val products = api.fetchProductsByWholesaler(session.user.id)
            categories to products
        }.fold(
            onSuccess = { (categories, products) ->
                state = state.copy(
                    products = products,
                    categories = categories,
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = userReadableMessage(error, "Unable to load products right now."),
                )
            },
        )
    }

    fun updateSearchInput(value: String) {
        state = state.copy(searchInput = value)
    }

    fun startCreate() {
        formState = ProductFormUiState(
            categoryId = state.categories.firstOrNull()?.id.orEmpty(),
            published = true,
        )
    }

    fun startEdit(product: ProductDTO) {
        formState = ProductFormUiState(
            productId = product.id,
            name = product.name,
            description = product.description,
            price = product.price.toString(),
            stock = product.stock.toString(),
            minimumOrderQuantity = product.minimumOrderQuantity.coerceAtLeast(1).toString(),
            gtin = product.gtin,
            categoryId = state.categories.firstOrNull { it.name == product.category }?.id.orEmpty(),
            published = product.published,
            images = product.images.mapIndexed { index, url ->
                ProductFormImage(
                    localId = "existing-url-$index-$url",
                    url = url,
                    name = "Product image ${index + 1}",
                    sortIndex = index,
                )
            },
            rating = product.rating,
            reviews = product.reviews,
        )
    }

    fun updateForm(update: ProductFormUiState.() -> ProductFormUiState) {
        formState = formState.update()
    }

    suspend fun loadFormProductImages(force: Boolean = false) {
        val productId = formState.productId ?: return
        if (!force && formState.images.any { it.remoteId != null }) return

        formState = formState.copy(isLoadingImages = true, imageErrorMessage = null)
        runCatching { api.fetchProductImages(productId) }.fold(
            onSuccess = { images ->
                formState = formState.copy(
                    images = images.toProductFormImages().ifEmpty { formState.images },
                    removedImageIds = emptySet(),
                    isLoadingImages = false,
                    imageErrorMessage = null,
                )
            },
            onFailure = { error ->
                formState = formState.copy(
                    isLoadingImages = false,
                    imageErrorMessage = userReadableMessage(error, "Unable to load product images."),
                )
            },
        )
    }

    fun addPickedProductImage(file: PickedFile): AuthActionResult {
        val mimeType = file.mimeType ?: guessImageMimeType(file.name)
        val validationMessage = when {
            formState.images.size >= MaxProductImages -> "You can upload up to $MaxProductImages product images."
            !mimeType.startsWith("image/") -> "Select a PNG, JPG, or WebP image."
            file.bytes.isEmpty() -> "Selected image is empty."
            file.bytes.size > MaxProductImageBytes -> "Image must be 10MB or smaller."
            else -> null
        }
        if (validationMessage != null) {
            formState = formState.copy(imageErrorMessage = validationMessage)
            return AuthActionResult(false, validationMessage)
        }

        val index = formState.images.size
        val image = ProductFormImage(
            localId = "local-$index-${file.name}-${file.bytes.size}",
            name = file.name.ifBlank { "product-image-${index + 1}.jpg" },
            mimeType = mimeType,
            bytes = file.bytes,
            sortIndex = index,
        )
        formState = formState.copy(
            images = (formState.images + image).withProductImageSortIndex(),
            imageErrorMessage = null,
        )
        return AuthActionResult(true, "Image added.")
    }

    fun removeProductFormImage(localId: String) {
        val image = formState.images.firstOrNull { it.localId == localId } ?: return
        formState = formState.copy(
            images = formState.images.filterNot { it.localId == localId }.withProductImageSortIndex(),
            removedImageIds = image.remoteId?.let { formState.removedImageIds + it } ?: formState.removedImageIds,
            imageErrorMessage = null,
        )
    }

    suspend fun saveForm(): AuthActionResult {
        val validationError = validateForm()
        if (validationError != null) {
            formState = formState.copy(errorMessage = validationError)
            return AuthActionResult(success = false, message = validationError)
        }

        val category = state.categories.first { it.id == formState.categoryId }
        val request = CreateProductRequest(
            name = formState.name.trim(),
            description = formState.description.trim(),
            price = formState.price.toDouble(),
            image = "",
            category = category.name,
            categoryId = category.id,
            wholesalerId = session.user.id,
            brand = session.user.company,
            stock = formState.stock.toInt(),
            minimumOrderQuantity = formState.minimumOrderQuantity.toInt(),
            gtin = formState.gtin.trim(),
        )

        formState = formState.copy(isSubmitting = true, errorMessage = null)
        return runCatching {
            val submittedForm = formState
            val saved = if (formState.productId == null) {
                api.createProduct(request)
            } else {
                api.updateProduct(formState.productId!!, request)
            }
            val publishedProduct = if (saved.published != submittedForm.published) {
                api.toggleProductPublished(saved.id)
            } else {
                saved
            }

            submittedForm.removedImageIds.forEach { imageId ->
                api.deleteProductImage(imageId)
            }

            val orderedImageIds = mutableListOf<String>()
            submittedForm.images.forEachIndexed { index, image ->
                when {
                    image.remoteId != null && image.remoteId !in submittedForm.removedImageIds -> {
                        orderedImageIds += image.remoteId
                    }
                    image.bytes != null -> {
                        val uploaded = api.uploadProductImage(
                            productId = saved.id,
                            fileName = image.name,
                            mimeType = image.mimeType,
                            bytes = image.bytes,
                            altText = submittedForm.name.trim(),
                            sortIndex = index,
                        )
                        uploaded.id?.let { orderedImageIds += it }
                    }
                }
            }
            if (orderedImageIds.isNotEmpty()) {
                api.reorderProductImages(saved.id, orderedImageIds)
            }

            publishedProduct
        }.fold(
            onSuccess = {
                refresh()
                formState = ProductFormUiState()
                val message = if (request.name.isBlank()) "Product saved." else "Product saved."
                state = state.copy(actionMessage = message)
                AuthActionResult(success = true, message = message)
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to save product.")
                formState = formState.copy(isSubmitting = false, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }

    suspend fun togglePublished(product: ProductDTO): AuthActionResult {
        return productAction(product.id, "Product status updated.") {
            api.toggleProductPublished(product.id)
        }
    }

    suspend fun deleteProduct(product: ProductDTO): AuthActionResult {
        return productAction(product.id, "Product deleted.") {
            api.deleteProduct(product.id)
        }
    }

    private suspend fun productAction(
        productId: String,
        successMessage: String,
        action: suspend () -> Unit,
    ): AuthActionResult {
        state = state.copy(busyProductIds = state.busyProductIds + productId, errorMessage = null)
        return runCatching {
            action()
            refresh()
        }.fold(
            onSuccess = {
                state = state.copy(
                    busyProductIds = state.busyProductIds - productId,
                    actionMessage = successMessage,
                )
                AuthActionResult(success = true, message = successMessage)
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Product update failed.")
                state = state.copy(busyProductIds = state.busyProductIds - productId, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }

    private fun validateForm(): String? {
        val price = formState.price.toDoubleOrNull()
        val stock = formState.stock.toIntOrNull()
        val moq = formState.minimumOrderQuantity.toIntOrNull()
        return when {
            formState.name.isBlank() -> "Product name is required."
            formState.description.isBlank() -> "Product description is required."
            price == null || price <= 0.0 -> "Price must be greater than zero."
            stock == null || stock < 0 -> "Stock must be zero or higher."
            moq == null || moq < 1 -> "Minimum order quantity must be at least 1."
            moq > stock -> "Minimum order quantity cannot exceed stock."
            formState.categoryId.isBlank() || state.categories.none { it.id == formState.categoryId } -> "Select a valid category."
            formState.gtin.isBlank() || !formState.gtin.all { it.isDigit() } -> "Valid numeric GTIN is required."
            else -> null
        }
    }

    private fun List<ProductImageResponse>.toProductFormImages(): List<ProductFormImage> {
        return sortedBy { it.sortIndex }.mapIndexed { index, image ->
            ProductFormImage(
                localId = "remote-${image.id ?: image.url}",
                remoteId = image.id,
                url = image.url,
                name = image.altText.ifBlank { "Product image ${index + 1}" },
                sortIndex = index,
            )
        }
    }

    private fun List<ProductFormImage>.withProductImageSortIndex(): List<ProductFormImage> {
        return mapIndexed { index, image -> image.copy(sortIndex = index) }
    }

    private fun guessImageMimeType(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "jpg", "jpeg" -> "image/jpeg"
            else -> "image/jpeg"
        }
    }

    private companion object {
        const val MaxProductImages = 10
        const val MaxProductImageBytes = 10 * 1024 * 1024
    }
}

internal enum class PosIntegrationTab {
    Setup,
    SyncLogs,
}

internal data class PosUiState(
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRotating: Boolean = false,
    val isRefreshingEvents: Boolean = false,
    val selectedTab: PosIntegrationTab = PosIntegrationTab.Setup,
    val contractExpanded: Boolean = false,
    val config: PosIntegrationConfigDTO? = null,
    val events: List<PosIntegrationEventLogDTO> = emptyList(),
    val statusDraft: String = "DISABLED",
    val webhookEnabledDraft: Boolean = false,
    val webhookUrlDraft: String = "",
    val webhookSecretDraft: String = "",
    val generatedKey: PosIntegrationApiKeyRotateResponse? = null,
    val errorMessage: String? = null,
) {
    val isActive: Boolean
        get() = statusDraft == "ACTIVE"

    val totalEvents: Int
        get() = events.size

    val storedEvents: Int
        get() = events.count { it.status.equals("STORED", ignoreCase = true) || it.status.equals("DELIVERED", ignoreCase = true) }

    val failedEvents: Int
        get() = events.count { it.status.equals("FAILED", ignoreCase = true) || it.lastError != null }
}

internal class PosStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: PosUiState by mutableStateOf(PosUiState())
        private set

    val isEnabled: Boolean = session.user.role == UserRole.RETAILER

    suspend fun loadInitial() {
        if (state.hasLoaded || state.isLoading) return
        refresh()
    }

    suspend fun refresh() {
        if (!isEnabled) {
            state = state.copy(
                hasLoaded = true,
                isLoading = false,
                errorMessage = "POS integration is available for retailer accounts.",
            )
            return
        }
        state = state.copy(
            isLoading = !state.hasLoaded,
            errorMessage = null,
        )
        runCatching {
            val config = api.fetchPosIntegrationConfig()
            val events = api.fetchPosIntegrationEventLogs(limit = 7)
            config to events
        }.fold(
            onSuccess = { (config, events) ->
                state = state.copy(
                    hasLoaded = true,
                    isLoading = false,
                    config = config,
                    events = events,
                    statusDraft = config.status.ifBlank { "DISABLED" },
                    webhookEnabledDraft = config.webhookEnabled,
                    webhookUrlDraft = config.webhookUrl.orEmpty(),
                    webhookSecretDraft = "",
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    hasLoaded = true,
                    isLoading = false,
                    errorMessage = userReadableMessage(error, "Unable to load POS integration."),
                )
            },
        )
    }

    suspend fun refreshEvents() {
        if (!isEnabled || state.isRefreshingEvents) return
        state = state.copy(isRefreshingEvents = true, errorMessage = null)
        runCatching { api.fetchPosIntegrationEventLogs(limit = 7) }.fold(
            onSuccess = { events ->
                state = state.copy(isRefreshingEvents = false, events = events)
            },
            onFailure = { error ->
                state = state.copy(
                    isRefreshingEvents = false,
                    errorMessage = userReadableMessage(error, "Unable to refresh POS events."),
                )
            },
        )
    }

    fun selectTab(tab: PosIntegrationTab) {
        state = state.copy(selectedTab = tab)
    }

    fun toggleContractExpanded() {
        state = state.copy(contractExpanded = !state.contractExpanded)
    }

    fun updateActive(enabled: Boolean) {
        state = state.copy(statusDraft = if (enabled) "ACTIVE" else "DISABLED")
    }

    fun updateWebhookEnabled(enabled: Boolean) {
        state = state.copy(webhookEnabledDraft = enabled)
    }

    fun updateWebhookUrl(value: String) {
        state = state.copy(webhookUrlDraft = value)
    }

    fun updateWebhookSecret(value: String) {
        state = state.copy(webhookSecretDraft = value)
    }

    suspend fun rotateApiKey(): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "POS integration is available for retailer accounts.")
        }
        if (state.isRotating) {
            return AuthActionResult(success = false, message = null)
        }
        state = state.copy(isRotating = true, errorMessage = null, generatedKey = null)
        return runCatching {
            api.rotatePosIntegrationApiKey()
        }.fold(
            onSuccess = { rotated ->
                val config = state.config?.copy(
                    integrationExists = true,
                    status = state.statusDraft,
                    apiKeyPrefix = rotated.apiKeyPrefix,
                    rotatedAt = rotated.rotatedAt,
                ) ?: PosIntegrationConfigDTO(
                    retailerId = rotated.retailerId,
                    integrationExists = true,
                    status = state.statusDraft,
                    apiKeyPrefix = rotated.apiKeyPrefix,
                    webhookEnabled = state.webhookEnabledDraft,
                    webhookUrl = state.webhookUrlDraft.takeIf { it.isNotBlank() },
                    webhookSecretConfigured = false,
                    createdAt = null,
                    rotatedAt = rotated.rotatedAt,
                )
                state = state.copy(
                    isRotating = false,
                    config = config,
                    generatedKey = rotated,
                    statusDraft = config.status.ifBlank { "ACTIVE" },
                    errorMessage = null,
                )
                AuthActionResult(success = true, message = "API key generated. Save it now; it will only be shown once.")
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to rotate POS API key.")
                state = state.copy(isRotating = false, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }

    suspend fun saveConfig(): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "POS integration is available for retailer accounts.")
        }
        if (state.config?.integrationExists != true) {
            return AuthActionResult(success = false, message = "Generate an API key before saving POS settings.")
        }
        state = state.copy(isSaving = true, errorMessage = null)
        return runCatching {
            api.updatePosIntegrationConfig(
                PosIntegrationConfigUpdateRequest(
                    status = state.statusDraft,
                    webhookEnabled = state.webhookEnabledDraft,
                    webhookUrl = state.webhookUrlDraft.trim().takeIf { it.isNotBlank() },
                    webhookSecret = state.webhookSecretDraft.trim().takeIf { it.isNotBlank() },
                ),
            )
        }.fold(
            onSuccess = { config ->
                state = state.copy(
                    isSaving = false,
                    config = config,
                    statusDraft = config.status.ifBlank { "DISABLED" },
                    webhookEnabledDraft = config.webhookEnabled,
                    webhookUrlDraft = config.webhookUrl.orEmpty(),
                    webhookSecretDraft = "",
                    errorMessage = null,
                )
                AuthActionResult(success = true, message = "POS settings saved.")
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to save POS settings.")
                state = state.copy(isSaving = false, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }
}

private fun ProductDTO.toProductRequest(
    wholesalerId: String,
    categoryId: String,
    stock: Int = this.stock,
): CreateProductRequest {
    return CreateProductRequest(
        name = name,
        description = description,
        price = price,
        image = image,
        category = category,
        categoryId = categoryId,
        wholesalerId = wholesalerId,
        brand = brand,
        stock = stock,
        minimumOrderQuantity = minimumOrderQuantity.coerceAtLeast(1),
        gtin = gtin,
    )
}
