package uqu.drawbridge.platform.ui.marketplace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uqu.drawbridge.platform.CategoryDTO
import uqu.drawbridge.platform.MarketplaceProductQuery
import uqu.drawbridge.platform.MobileAuthApi
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.WishlistDTO
import uqu.drawbridge.platform.ui.auth.AuthActionResult
import uqu.drawbridge.platform.ui.common.userReadableMessage
import uqu.drawbridge.platform.ui.model.SessionState

internal enum class MarketplaceSortOption(
    val apiValue: String,
    val label: String,
) {
    Featured("featured", "Featured"),
    PriceLow("price-low", "Price low"),
    PriceHigh("price-high", "Price high"),
    Rating("rating", "Rating"),
    Newest("newest", "Newest"),
}

internal data class MarketplaceUiState(
    val products: List<ProductDTO> = emptyList(),
    val categories: List<CategoryDTO> = emptyList(),
    val brands: List<String> = emptyList(),
    val searchInput: String = "",
    val appliedSearch: String = "",
    val selectedCategoryId: String? = null,
    val selectedBrand: String? = null,
    val sortOption: MarketplaceSortOption = MarketplaceSortOption.Featured,
    val currentPage: Int = 0,
    val pageSize: Int = MarketplaceStateHolder.PageSize,
    val totalPages: Int = 0,
    val totalElements: Long = 0,
    val isFirst: Boolean = true,
    val isLast: Boolean = true,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
)

internal class MarketplaceStateHolder(
    private val api: MobileAuthApi,
) {
    var state: MarketplaceUiState by mutableStateOf(MarketplaceUiState())
        private set

    suspend fun loadInitial() {
        if (state.hasLoaded || state.isLoading) {
            return
        }
        loadFilterOptions()
        loadPage(reset = true)
    }

    suspend fun refresh() {
        loadFilterOptions(force = true)
        loadPage(reset = true)
    }

    fun updateSearchInput(value: String) {
        state = state.copy(searchInput = value)
    }

    suspend fun fetchImageBytes(imageUrl: String): ByteArray {
        return api.fetchImageBytes(imageUrl)
    }

    suspend fun applySearch() {
        state = state.copy(appliedSearch = state.searchInput.trim())
        loadPage(reset = true)
    }

    suspend fun clearSearch() {
        state = state.copy(searchInput = "", appliedSearch = "")
        loadPage(reset = true)
    }

    suspend fun selectCategory(categoryId: String?) {
        state = state.copy(selectedCategoryId = categoryId)
        loadPage(reset = true)
    }

    suspend fun selectBrand(brand: String?) {
        state = state.copy(selectedBrand = brand)
        loadPage(reset = true)
    }

    suspend fun selectSort(sortOption: MarketplaceSortOption) {
        state = state.copy(sortOption = sortOption)
        loadPage(reset = true)
    }

    suspend fun clearFilters() {
        state = state.copy(
            selectedCategoryId = null,
            selectedBrand = null,
            sortOption = MarketplaceSortOption.Featured,
            searchInput = "",
            appliedSearch = "",
        )
        loadPage(reset = true)
    }

    suspend fun loadMore() {
        if (state.isLast || state.isLoadingMore || state.isLoading) {
            return
        }
        loadPage(reset = false)
    }

    private suspend fun loadFilterOptions(force: Boolean = false) {
        if (!force && (state.categories.isNotEmpty() || state.brands.isNotEmpty())) {
            return
        }

        val categories = runCatching { api.fetchProductCategories() }.getOrDefault(state.categories)
        val brands = runCatching { api.fetchProductBrands() }.getOrDefault(state.brands)
        state = state.copy(
            categories = categories.sortedBy { it.name.lowercase() },
            brands = brands.filter { it.isNotBlank() }.distinct().sortedBy { it.lowercase() },
        )
    }

    private suspend fun loadPage(reset: Boolean) {
        val nextPage = if (reset) 0 else state.currentPage + 1
        val currentProducts = state.products
        state = state.copy(
            isLoading = reset && currentProducts.isEmpty(),
            isRefreshing = reset && currentProducts.isNotEmpty(),
            isLoadingMore = !reset,
            errorMessage = null,
        )

        val query = MarketplaceProductQuery(
            page = nextPage,
            size = PageSize,
            search = state.appliedSearch.takeIf { it.isNotBlank() },
            categoryIds = state.selectedCategoryId?.let(::listOf).orEmpty(),
            brands = state.selectedBrand?.let(::listOf).orEmpty(),
            sort = state.sortOption.apiValue,
        )

        runCatching { api.fetchMarketplaceProducts(query) }
            .fold(
                onSuccess = { page ->
                    state = state.copy(
                        products = if (reset) page.content else currentProducts + page.content,
                        currentPage = page.currentPage,
                        pageSize = page.pageSize,
                        totalPages = page.totalPages,
                        totalElements = page.totalElements,
                        isFirst = page.isFirst,
                        isLast = page.isLast,
                        hasLoaded = true,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        products = if (reset) emptyList() else currentProducts,
                        hasLoaded = true,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = userReadableMessage(error, "Unable to load products right now. Please try again."),
                    )
                },
            )
    }

    internal companion object {
        const val PageSize = 12
    }
}

internal data class WishlistProductItem(
    val wishlistItem: WishlistDTO,
    val product: ProductDTO?,
)

internal data class WishlistUiState(
    val items: List<WishlistProductItem> = emptyList(),
    val productIds: Set<String> = emptySet(),
    val busyProductIds: Set<String> = emptySet(),
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
)

internal class WishlistStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: WishlistUiState by mutableStateOf(WishlistUiState())
        private set

    val isEnabled: Boolean = session.user.role == UserRole.RETAILER

    suspend fun refresh() {
        if (!isEnabled) {
            state = WishlistUiState(hasLoaded = true)
            return
        }

        state = state.copy(isLoading = !state.hasLoaded, errorMessage = null, actionMessage = null)
        runCatching {
            val apiItems = api.fetchWishlist(session.user.id)
            apiItems.map { item ->
                WishlistProductItem(
                    wishlistItem = item,
                    product = runCatching { api.fetchProductById(item.productId) }.getOrNull(),
                )
            }
        }
            .fold(
                onSuccess = { items ->
                    state = state.copy(
                        items = items,
                        productIds = items.map { it.wishlistItem.productId }.toSet(),
                        hasLoaded = true,
                        isLoading = false,
                        errorMessage = null,
                    )
                },
                onFailure = { error ->
                    state = state.copy(
                        items = emptyList(),
                        productIds = emptySet(),
                        hasLoaded = true,
                        isLoading = false,
                        errorMessage = userReadableMessage(error, "Unable to load wishlist right now."),
                    )
                },
            )
    }

    fun isInWishlist(productId: String): Boolean = productId in state.productIds

    fun clearActionMessage() {
        state = state.copy(actionMessage = null)
    }

    suspend fun toggle(productId: String): AuthActionResult {
        return if (isInWishlist(productId)) {
            remove(productId)
        } else {
            add(productId)
        }
    }

    suspend fun add(productId: String): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "Wishlist is available for retailer accounts.")
        }
        return performWishlistAction(
            productId = productId,
            successMessage = "Saved to wishlist.",
        ) {
            api.addToWishlist(session.user.id, productId)
        }
    }

    suspend fun remove(productId: String): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "Wishlist is available for retailer accounts.")
        }
        return performWishlistAction(
            productId = productId,
            successMessage = "Removed from wishlist.",
        ) {
            api.removeFromWishlist(session.user.id, productId)
        }
    }

    private suspend fun performWishlistAction(
        productId: String,
        successMessage: String,
        action: suspend () -> Unit,
    ): AuthActionResult {
        state = state.copy(
            busyProductIds = state.busyProductIds + productId,
            errorMessage = null,
            actionMessage = null,
        )

        return runCatching {
            action()
            refresh()
        }
            .fold(
                onSuccess = {
                    state = state.copy(
                        busyProductIds = state.busyProductIds - productId,
                        actionMessage = successMessage,
                    )
                    AuthActionResult(success = true, message = successMessage)
                },
                onFailure = { error ->
                    val message = userReadableMessage(error, "Wishlist update failed. Please try again.")
                    state = state.copy(
                        busyProductIds = state.busyProductIds - productId,
                        errorMessage = message,
                    )
                    AuthActionResult(success = false, message = message)
                },
            )
    }
}

internal data class ProductDetailUiState(
    val product: ProductDTO? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

internal class ProductDetailStateHolder(
    private val api: MobileAuthApi,
) {
    var state: ProductDetailUiState by mutableStateOf(ProductDetailUiState())
        private set

    suspend fun load(productId: String) {
        if (state.product?.id == productId && state.errorMessage == null) {
            return
        }

        state = ProductDetailUiState(isLoading = true)
        runCatching { api.fetchProductById(productId) }
            .fold(
                onSuccess = { product ->
                    state = ProductDetailUiState(product = product)
                },
                onFailure = { error ->
                    state = ProductDetailUiState(
                        errorMessage = userReadableMessage(error, "Unable to load this product."),
                    )
                },
            )
    }

    suspend fun fetchImageBytes(imageUrl: String): ByteArray {
        return api.fetchImageBytes(imageUrl)
    }
}
