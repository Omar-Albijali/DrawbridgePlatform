package uqu.drawbridge.platform.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.CategoryDTO
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.ErrorStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.marketplace.MarketplaceSortOption
import uqu.drawbridge.platform.ui.marketplace.MarketplaceStateHolder
import uqu.drawbridge.platform.ui.marketplace.ProductDetailStateHolder
import uqu.drawbridge.platform.ui.marketplace.WishlistProductItem
import uqu.drawbridge.platform.ui.marketplace.WishlistStateHolder

@Composable
internal fun MarketplaceMainScreen(
    marketplaceStateHolder: MarketplaceStateHolder,
    wishlistStateHolder: WishlistStateHolder,
    onOpenProduct: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = marketplaceStateHolder.state
    var filtersExpanded by remember { mutableStateOf(false) }
    val activeFilterCount = activeFilterCount(
        selectedCategoryId = state.selectedCategoryId,
        selectedBrand = state.selectedBrand,
        sortOption = state.sortOption,
    )

    LaunchedEffect(Unit) {
        marketplaceStateHolder.loadInitial()
        wishlistStateHolder.refresh()
    }

    ScreenSection(
        title = "Marketplace",
        subtitle = "Browse verified wholesale products.",
    ) {
        AppCard {
            AppTextField(
                value = state.searchInput,
                onValueChange = marketplaceStateHolder::updateSearchInput,
                label = "Search products",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(
                    text = "Search",
                    onClick = { coroutineScope.launch { marketplaceStateHolder.applySearch() } },
                    modifier = Modifier.weight(1f),
                    enabled = !state.isLoading && !state.isRefreshing,
                )
                SecondaryButton(
                    text = "Clear",
                    onClick = { coroutineScope.launch { marketplaceStateHolder.clearSearch() } },
                    modifier = Modifier.weight(1f),
                    enabled = state.searchInput.isNotBlank() || state.appliedSearch.isNotBlank(),
                )
            }
            SecondaryButton(
                text = if (filtersExpanded) {
                    "Hide filters"
                } else if (activeFilterCount > 0) {
                    "Filters ($activeFilterCount)"
                } else {
                    "Filters"
                },
                onClick = { filtersExpanded = !filtersExpanded },
            )
            if (!filtersExpanded && activeFilterCount > 0) {
                Text(
                    text = activeFilterSummary(
                        categoryName = state.categories.firstOrNull { it.id == state.selectedCategoryId }?.name,
                        brand = state.selectedBrand,
                        sortOption = state.sortOption,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (filtersExpanded) {
            MarketplaceFilterPanel(
                categories = state.categories,
                brands = state.brands,
                selectedCategoryId = state.selectedCategoryId,
                selectedBrand = state.selectedBrand,
                selectedSort = state.sortOption,
                onCategorySelected = { categoryId ->
                    coroutineScope.launch { marketplaceStateHolder.selectCategory(categoryId) }
                },
                onBrandSelected = { brand ->
                    coroutineScope.launch { marketplaceStateHolder.selectBrand(brand) }
                },
                onSortSelected = { sort ->
                    coroutineScope.launch { marketplaceStateHolder.selectSort(sort) }
                },
                onClearFilters = {
                    coroutineScope.launch { marketplaceStateHolder.clearFilters() }
                },
            )
        }

        if (state.isLoading) {
            repeat(3) {
                ProductSkeletonCard()
            }
            return@ScreenSection
        }

        if (state.errorMessage != null && state.products.isEmpty()) {
            ErrorStateCard(
                title = "Could not load marketplace",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { marketplaceStateHolder.refresh() } },
            )
            return@ScreenSection
        }

        MarketplaceResultsSummary(
            totalElements = state.totalElements,
            currentCount = state.products.size,
            page = state.currentPage + 1,
            totalPages = state.totalPages,
            isRefreshing = state.isRefreshing,
        )

        if (state.products.isEmpty()) {
            EmptyStateCard(
                title = "No products found",
                message = "Try a different search, category, brand, or sort option.",
                actionText = "Reset filters",
                onAction = { coroutineScope.launch { marketplaceStateHolder.clearFilters() } },
            )
        } else {
            state.products.forEach { product ->
                MarketplaceProductCard(
                    product = product,
                    isWishlisted = wishlistStateHolder.isInWishlist(product.id),
                    wishlistEnabled = wishlistStateHolder.isEnabled,
                    isWishlistBusy = product.id in wishlistStateHolder.state.busyProductIds,
                    onOpenProduct = { onOpenProduct(product.id) },
                    onToggleWishlist = {
                        coroutineScope.launch {
                            val result = wishlistStateHolder.toggle(product.id)
                            result.message?.let(onShowMessage)
                        }
                    },
                )
            }

            if (!state.isLast) {
                SecondaryButton(
                    text = if (state.isLoadingMore) "Loading more..." else "Load more",
                    enabled = !state.isLoadingMore,
                    onClick = { coroutineScope.launch { marketplaceStateHolder.loadMore() } },
                )
            }
        }
    }
}

@Composable
private fun MarketplaceFilterPanel(
    categories: List<CategoryDTO>,
    brands: List<String>,
    selectedCategoryId: String?,
    selectedBrand: String?,
    selectedSort: MarketplaceSortOption,
    onCategorySelected: (String?) -> Unit,
    onBrandSelected: (String?) -> Unit,
    onSortSelected: (MarketplaceSortOption) -> Unit,
    onClearFilters: () -> Unit,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Category, brand, and sort",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClearFilters) {
                Text("Reset")
            }
        }

        Text("Sort", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        SortOptionRows(selectedSort = selectedSort, onSortSelected = onSortSelected)

        if (categories.isNotEmpty()) {
            Text("Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            CategoryOptionRows(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = onCategorySelected,
            )
        }

        if (brands.isNotEmpty()) {
            Text("Brand", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            BrandOptionRows(
                brands = brands,
                selectedBrand = selectedBrand,
                onBrandSelected = onBrandSelected,
            )
        }
    }
}

@Composable
private fun SortOptionRows(
    selectedSort: MarketplaceSortOption,
    onSortSelected: (MarketplaceSortOption) -> Unit,
) {
    MarketplaceSortOption.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { option ->
                FilterOptionButton(
                    text = option.label,
                    selected = option == selectedSort,
                    onClick = { onSortSelected(option) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryOptionRows(
    categories: List<CategoryDTO>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    val options = listOf<CategoryDTO?>(null) + categories
    options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { category ->
                val value = category?.id
                FilterOptionButton(
                    text = category?.name?.takeIf { it.isNotBlank() } ?: "All categories",
                    selected = selectedCategoryId == value,
                    onClick = { onCategorySelected(value) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BrandOptionRows(
    brands: List<String>,
    selectedBrand: String?,
    onBrandSelected: (String?) -> Unit,
) {
    val options = listOf<String?>(null) + brands
    options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { brand ->
                FilterOptionButton(
                    text = brand ?: "All brands",
                    selected = selectedBrand == brand,
                    onClick = { onBrandSelected(brand) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FilterOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MarketplaceResultsSummary(
    totalElements: Long,
    currentCount: Int,
    page: Int,
    totalPages: Int,
    isRefreshing: Boolean,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (totalElements > 0) "$currentCount of $totalElements products" else "Fresh marketplace results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (totalPages > 0) "Page $page of $totalPages" else "Use search and filters to narrow the catalog.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isRefreshing) {
                StatusChip(text = "Updating", tone = StatusTone.Warning)
            }
        }
    }
}

@Composable
private fun ProductSkeletonCard() {
    AppCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun MarketplaceProductCard(
    product: ProductDTO,
    isWishlisted: Boolean,
    wishlistEnabled: Boolean,
    isWishlistBusy: Boolean,
    onOpenProduct: () -> Unit,
    onToggleWishlist: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProduct),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProductImageFallback(product = product, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${product.brand} • ${product.category.ifBlank { "Uncategorized" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (wishlistEnabled) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onToggleWishlist,
                            enabled = !isWishlistBusy,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                                tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = when {
                                isWishlistBusy -> "Saving"
                                isWishlisted -> "Saved"
                                else -> "Save"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatPrice(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
                StatusChip(text = stockStatusText(product), tone = stockStatusTone(product))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip(text = "MOQ ${product.minimumOrderQuantity.coerceAtLeast(1)}", tone = StatusTone.Neutral)
                StatusChip(text = "Rating ${product.rating}", tone = StatusTone.Neutral)
            }

            SecondaryButton(text = "View details", onClick = onOpenProduct)
        }
    }
}

@Composable
internal fun ProductDetailMainScreen(
    productId: String,
    detailStateHolder: ProductDetailStateHolder,
    wishlistStateHolder: WishlistStateHolder,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = detailStateHolder.state

    LaunchedEffect(productId) {
        detailStateHolder.load(productId)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ProductDetailHeader(onBack = onBack)

        when {
            state.isLoading -> LoadingStateCard(
                title = "Loading product",
                message = "Fetching the latest marketplace detail.",
            )

            state.errorMessage != null -> ErrorStateCard(
                title = "Product unavailable",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { detailStateHolder.load(productId) } },
            )

            state.product != null -> ProductDetailContent(
                product = state.product,
                wishlistStateHolder = wishlistStateHolder,
                onShowMessage = onShowMessage,
            )
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDTO,
    wishlistStateHolder: WishlistStateHolder,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val isWishlisted = wishlistStateHolder.isInWishlist(product.id)
    val isWishlistBusy = product.id in wishlistStateHolder.state.busyProductIds
    val imageCount = product.images.count { it.isNotBlank() } + if (product.image.isNotBlank() && product.image !in product.images) 1 else 0

    ScreenSection(
        title = product.name,
        subtitle = product.brand.ifBlank { "Marketplace product" },
    ) {
        ProductImageFallback(product = product, modifier = Modifier.fillMaxWidth().aspectRatio(1f))

        if (imageCount > 1) {
            AppCard {
                Text("Images", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "$imageCount catalog images available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(formatPrice(product.price), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(
                        "${product.category.ifBlank { "Uncategorized" }} • ${product.supplier}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (wishlistStateHolder.isEnabled) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val result = wishlistStateHolder.toggle(product.id)
                                    result.message?.let(onShowMessage)
                                }
                            },
                            enabled = !isWishlistBusy,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                                tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = when {
                                isWishlistBusy -> "Saving"
                                isWishlisted -> "Saved"
                                else -> "Save"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip(text = stockStatusText(product), tone = stockStatusTone(product))
                StatusChip(text = "MOQ ${product.minimumOrderQuantity.coerceAtLeast(1)}", tone = StatusTone.Neutral)
            }
        }

        AppCard {
            Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = product.description.ifBlank { "No description is available for this product yet." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SecondaryButton(
            text = "Add to cart starts in Phase 4",
            onClick = {},
            enabled = false,
        )
    }
}

@Composable
private fun ProductDetailHeader(
    onBack: () -> Unit,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to marketplace")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Product detail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Back to marketplace",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun WishlistMainScreen(
    wishlistStateHolder: WishlistStateHolder,
    onOpenProduct: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = wishlistStateHolder.state

    LaunchedEffect(Unit) {
        wishlistStateHolder.refresh()
    }

    ScreenSection(
        title = "Wishlist",
        subtitle = if (state.productIds.isEmpty()) "Saved products will appear here." else "${state.productIds.size} saved products",
    ) {
        when {
            state.isLoading -> LoadingStateCard(
                title = "Loading wishlist",
                message = "Checking your saved marketplace products.",
            )

            state.errorMessage != null && state.items.isEmpty() -> ErrorStateCard(
                title = "Could not load wishlist",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { wishlistStateHolder.refresh() } },
            )

            state.items.isEmpty() -> EmptyStateCard(
                title = "Your wishlist is empty",
                message = "Save products from the marketplace and come back to them later.",
            )

            else -> state.items.forEach { item ->
                WishlistProductCard(
                    item = item,
                    isBusy = item.wishlistItem.productId in state.busyProductIds,
                    onOpenProduct = { onOpenProduct(item.wishlistItem.productId) },
                    onRemove = {
                        coroutineScope.launch {
                            val result = wishlistStateHolder.remove(item.wishlistItem.productId)
                            result.message?.let(onShowMessage)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WishlistProductCard(
    item: WishlistProductItem,
    isBusy: Boolean,
    onOpenProduct: () -> Unit,
    onRemove: () -> Unit,
) {
    val product = item.product
    AppCard {
        if (product != null) {
            ProductImageFallback(product = product, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f))
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            text = product?.name ?: item.wishlistItem.productName.ifBlank { "Product unavailable" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatPrice(product?.price ?: item.wishlistItem.productPrice),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(text = "Details", onClick = onOpenProduct, modifier = Modifier.weight(1f))
            SecondaryButton(
                text = if (isBusy) "Removing..." else "Remove",
                onClick = onRemove,
                modifier = Modifier.weight(1f),
                enabled = !isBusy,
            )
        }
    }
}

@Composable
private fun ProductImageFallback(
    product: ProductDTO,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = product.category.ifBlank { "Product" },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (product.image.isNotBlank() || product.images.isNotEmpty()) {
                Text(
                    text = "Image available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun stockStatusText(product: ProductDTO): String {
    val stock = product.stock
    val moq = product.minimumOrderQuantity.coerceAtLeast(1)
    return when {
        stock <= 0 -> "Out of stock"
        stock < moq -> "Below MOQ"
        stock < 20 -> "$stock left"
        else -> "$stock in stock"
    }
}

private fun stockStatusTone(product: ProductDTO): StatusTone {
    val stock = product.stock
    val moq = product.minimumOrderQuantity.coerceAtLeast(1)
    return when {
        stock <= 0 || stock < moq -> StatusTone.Error
        stock < 20 -> StatusTone.Warning
        else -> StatusTone.Success
    }
}

private fun formatPrice(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    return "SAR $rounded"
}

private fun activeFilterCount(
    selectedCategoryId: String?,
    selectedBrand: String?,
    sortOption: MarketplaceSortOption,
): Int {
    var count = 0
    if (selectedCategoryId != null) count += 1
    if (selectedBrand != null) count += 1
    if (sortOption != MarketplaceSortOption.Featured) count += 1
    return count
}

private fun activeFilterSummary(
    categoryName: String?,
    brand: String?,
    sortOption: MarketplaceSortOption,
): String {
    val parts = buildList {
        categoryName?.let { add("Category: $it") }
        brand?.let { add("Brand: $it") }
        if (sortOption != MarketplaceSortOption.Featured) add("Sort: ${sortOption.label}")
    }
    return parts.joinToString(" • ")
}
