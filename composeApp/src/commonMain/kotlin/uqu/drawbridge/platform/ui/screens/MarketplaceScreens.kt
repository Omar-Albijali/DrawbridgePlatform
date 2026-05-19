package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image as SkiaImage
import uqu.drawbridge.platform.CategoryDTO
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.ErrorStateCard
import uqu.drawbridge.platform.ui.components.GlassCard
import uqu.drawbridge.platform.ui.components.GlassPill
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.commerce.CartStateHolder
import uqu.drawbridge.platform.ui.marketplace.MarketplaceSortOption
import uqu.drawbridge.platform.ui.marketplace.MarketplaceStateHolder
import uqu.drawbridge.platform.ui.marketplace.ProductDetailStateHolder
import uqu.drawbridge.platform.ui.marketplace.WishlistProductItem
import uqu.drawbridge.platform.ui.marketplace.WishlistStateHolder

private val MarketText = Color(0xFFF8FAFC)
private val MarketMuted = Color(0xFFA8B7C7)
private val MarketNavy = Color(0xFF03111F)
private val MarketNavyHigh = Color(0xFF102A3D)

@Composable
internal fun MarketplaceMainScreen(
    marketplaceStateHolder: MarketplaceStateHolder,
    wishlistStateHolder: WishlistStateHolder,
    cartStateHolder: CartStateHolder,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit,
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
        cartStateHolder.loadInitial()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MarketplaceCatalogHeader(
            cartItemCount = cartStateHolder.state.itemCount,
            cartEnabled = cartStateHolder.isEnabled,
            onOpenCart = onOpenCart,
        )

        MarketplaceBrowseControls(
            searchInput = state.searchInput,
            selectedCategoryId = state.selectedCategoryId,
            categories = state.categories,
            activeFilterCount = activeFilterCount,
            isBusy = state.isLoading || state.isRefreshing,
            onSearchInputChanged = marketplaceStateHolder::updateSearchInput,
            onSearch = { coroutineScope.launch { marketplaceStateHolder.applySearch() } },
            onClearSearch = { coroutineScope.launch { marketplaceStateHolder.clearSearch() } },
            onToggleFilters = { filtersExpanded = !filtersExpanded },
            onCategorySelected = { categoryId ->
                coroutineScope.launch { marketplaceStateHolder.selectCategory(categoryId) }
            },
        )

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
                onDone = { filtersExpanded = false },
            )
        }

        if (state.isLoading) {
            MarketplaceProductGridSkeleton()
            return@Column
        }

        if (state.errorMessage != null && state.products.isEmpty()) {
            ErrorStateCard(
                title = "Could not load marketplace",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { marketplaceStateHolder.refresh() } },
            )
            return@Column
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
            MarketplaceProductGrid(
                products = state.products,
                wishlistStateHolder = wishlistStateHolder,
                cartStateHolder = cartStateHolder,
                imageLoader = marketplaceStateHolder::fetchImageBytes,
                onOpenProduct = onOpenProduct,
                onShowMessage = onShowMessage,
            )

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
private fun MarketplaceCatalogHeader(
    cartItemCount: Int,
    cartEnabled: Boolean,
    onOpenCart: () -> Unit,
) {
    GlassCard(contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "Marketplace",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MarketText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Browse verified supplier products",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MarketMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (cartEnabled) {
                MarketplaceCartButton(
                    itemCount = cartItemCount,
                    onClick = onOpenCart,
                )
            }
        }
    }
}

@Composable
private fun MarketplaceCartButton(
    itemCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.065f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Open cart",
                tint = MarketText,
                modifier = Modifier.size(24.dp),
            )
            if (itemCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(24.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, MarketNavy.copy(alpha = 0.6f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = itemCount.coerceAtMost(99).toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceBrowseControls(
    searchInput: String,
    selectedCategoryId: String?,
    categories: List<CategoryDTO>,
    activeFilterCount: Int,
    isBusy: Boolean,
    onSearchInputChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onToggleFilters: () -> Unit,
    onCategorySelected: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MarketplaceSearchField(
                value = searchInput,
                onValueChange = onSearchInputChanged,
                onClear = onClearSearch,
                onSearch = onSearch,
                isBusy = isBusy,
                modifier = Modifier.weight(1f),
            )
            MarketplaceFilterIconButton(
                activeFilterCount = activeFilterCount,
                onClick = onToggleFilters,
            )
        }

        MarketplaceCategoryStrip(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = onCategorySelected,
        )
    }
}

@Composable
private fun MarketplaceFilterIconButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.065f),
        border = BorderStroke(
            1.dp,
            if (activeFilterCount > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filters",
                tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MarketMuted,
                modifier = Modifier.size(22.dp),
            )
            if (activeFilterCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(14.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = activeFilterCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketplaceCategoryStrip(
    categories: List<CategoryDTO>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MarketplaceCategoryChip(
            text = "All",
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
        )
        categories.forEach { category ->
            MarketplaceCategoryChip(
                text = category.name.ifBlank { "Category" },
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
            )
        }
    }
}

@Composable
private fun MarketplaceCategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.055f),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 17.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MarketText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MarketplaceSearchPanel(
    searchInput: String,
    appliedSearch: String,
    activeFilterCount: Int,
    filtersExpanded: Boolean,
    activeFilterSummary: String,
    isBusy: Boolean,
    onSearchInputChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onToggleFilters: () -> Unit,
) {
    GlassCard(contentPadding = 10.dp) {
        MarketplaceSearchField(
            value = searchInput,
            onValueChange = onSearchInputChanged,
            onClear = onClearSearch,
            onSearch = onSearch,
            isBusy = isBusy,
        )

        if (!filtersExpanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MarketplaceToolbarButton(
                    text = sortLabelFromSummary(activeFilterSummary),
                    icon = Icons.AutoMirrored.Filled.Sort,
                    onClick = onToggleFilters,
                    modifier = Modifier.weight(1f),
                    primary = false,
                )
                MarketplaceToolbarButton(
                    text = if (activeFilterCount > 0) "Filters $activeFilterCount" else "Filters",
                    icon = Icons.Default.FilterList,
                    onClick = onToggleFilters,
                    modifier = Modifier.weight(1f),
                    primary = false,
                )
            }
        }
        if (!filtersExpanded && activeFilterCount > 0) {
            Text(
                text = activeFilterSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MarketMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MarketplaceSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    isBusy: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.065f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MarketMuted,
                modifier = Modifier.size(18.dp),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        text = "Search products",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MarketMuted,
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
                        color = MarketText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
            if (value.isNotBlank()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            IconButton(
                onClick = onSearch,
                enabled = !isBusy,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun MarketplaceToolbarButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean,
    enabled: Boolean = true,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.045f)
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MarketText
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) container else Color.White.copy(alpha = 0.035f),
        border = BorderStroke(
            1.dp,
            if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.09f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) content else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) content else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
    onDone: () -> Unit,
) {
    GlassCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Refine products",
                    style = MaterialTheme.typography.titleMedium,
                    color = MarketText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Sort, category, and brand",
                    style = MaterialTheme.typography.bodySmall,
                    color = MarketMuted,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onClearFilters) {
                    Text("Reset", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onDone) {
                    Text("Done", color = MarketText)
                }
            }
        }

        FilterSectionLabel("Sort")
        SortOptionRows(selectedSort = selectedSort, onSortSelected = onSortSelected)

        if (categories.isNotEmpty()) {
            FilterSectionLabel("Category")
            CategoryOptionRows(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = onCategorySelected,
            )
        }

        if (brands.isNotEmpty()) {
            FilterSectionLabel("Brand")
            BrandOptionRows(
                brands = brands,
                selectedBrand = selectedBrand,
                onBrandSelected = onBrandSelected,
            )
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MarketText,
        fontWeight = FontWeight.Black,
    )
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
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.035f),
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (totalElements > 0) "Showing 1-$currentCount of $totalElements" else "Showing products",
            style = MaterialTheme.typography.labelLarge,
            color = MarketMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (totalPages > 0) "Page $page of $totalPages" else "Page 1 of 1",
            style = MaterialTheme.typography.labelLarge,
            color = MarketMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isRefreshing) {
            Spacer(Modifier.width(8.dp))
            GlassPill(text = "Updating", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MarketplaceProductGridSkeleton() {
    repeat(2) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProductSkeletonCard(modifier = Modifier.weight(1f))
            ProductSkeletonCard(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProductSkeletonCard(
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier, contentPadding = 10.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp)),
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
private fun MarketplaceProductGrid(
    products: List<ProductDTO>,
    wishlistStateHolder: WishlistStateHolder,
    cartStateHolder: CartStateHolder,
    imageLoader: suspend (String) -> ByteArray,
    onOpenProduct: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        products.chunked(2).forEach { rowProducts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                rowProducts.forEach { product ->
                    MarketplaceProductCard(
                        product = product,
                        isWishlisted = wishlistStateHolder.isInWishlist(product.id),
                        wishlistEnabled = wishlistStateHolder.isEnabled,
                        isWishlistBusy = product.id in wishlistStateHolder.state.busyProductIds,
                        cartEnabled = cartStateHolder.isEnabled,
                        isCartBusy = product.id in cartStateHolder.state.busyProductIds,
                        imageLoader = imageLoader,
                        onOpenProduct = { onOpenProduct(product.id) },
                        onToggleWishlist = {
                            coroutineScope.launch {
                                val result = wishlistStateHolder.toggle(product.id)
                                result.message?.let(onShowMessage)
                            }
                        },
                        onAddToCart = {
                            coroutineScope.launch {
                                val result = cartStateHolder.addProduct(product)
                                result.message?.let(onShowMessage)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowProducts.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MarketplaceProductCard(
    product: ProductDTO,
    isWishlisted: Boolean,
    wishlistEnabled: Boolean,
    isWishlistBusy: Boolean,
    cartEnabled: Boolean,
    isCartBusy: Boolean,
    imageLoader: suspend (String) -> ByteArray,
    onOpenProduct: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minimumOrderQuantity = product.minimumOrderQuantity.coerceAtLeast(1)
    val canAddToCart = cartEnabled && product.stock >= minimumOrderQuantity

    Surface(
        modifier = modifier
            .clickable(onClick = onOpenProduct),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.075f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column {
            ProductImageSurface(
                product = product,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                showCategoryLabel = false,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MarketText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            productBrandLine(product),
                            style = MaterialTheme.typography.bodySmall,
                            color = MarketMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            ratingLabel(product),
                            style = MaterialTheme.typography.labelSmall,
                            color = MarketMuted,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (wishlistEnabled) {
                        IconButton(
                            onClick = onToggleWishlist,
                            enabled = !isWishlistBusy,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                                tint = if (isWishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProductStatusMiniChip(
                        text = shortStockStatusText(product),
                        tone = stockStatusTone(product),
                        modifier = Modifier.weight(1f),
                    )
                    ProductMetaChip(text = "MOQ $minimumOrderQuantity", modifier = Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "SAR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MarketText,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                        )
                        Text(
                            text = formatMoneyAmount(product.price),
                            style = MaterialTheme.typography.titleLarge,
                            color = MarketText,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (cartEnabled) {
                        ProductAddButton(
                            text = when {
                                isCartBusy -> "Adding..."
                                product.stock <= 0 -> "Out"
                                product.stock < minimumOrderQuantity -> "Below MOQ"
                                else -> "Add"
                            },
                            enabled = canAddToCart && !isCartBusy,
                            onClick = onAddToCart,
                            modifier = Modifier.width(84.dp),
                        )
                    } else {
                        ProductCompactButton(
                            text = "Details",
                            primary = false,
                            enabled = true,
                            onClick = onOpenProduct,
                            modifier = Modifier.width(84.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductImageSurface(
    product: ProductDTO,
    modifier: Modifier = Modifier,
    imageLoader: (suspend (String) -> ByteArray)? = null,
    showCategoryLabel: Boolean = true,
) {
    val category = product.category.ifBlank { "Product" }
    val imageUrl = remember(product.id, product.image, product.images.contentHashCode()) {
        primaryImageUrl(product)
    }
    val imageState by produceState<ProductImageLoadState>(
        initialValue = if (imageUrl != null && imageLoader != null) ProductImageLoadState.Loading else ProductImageLoadState.Unavailable,
        key1 = imageUrl,
        key2 = imageLoader,
    ) {
        value = if (imageUrl == null || imageLoader == null) {
            ProductImageLoadState.Unavailable
        } else {
            runCatching {
                SkiaImage.makeFromEncoded(imageLoader(imageUrl)).toComposeImageBitmap()
            }.fold(
                onSuccess = { ProductImageLoadState.Loaded(it) },
                onFailure = { ProductImageLoadState.Unavailable },
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(productImageBrush()),
    ) {
        when (val state = imageState) {
            is ProductImageLoadState.Loaded -> {
                Image(
                    bitmap = state.bitmap,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxWidth().matchParentSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    MarketNavy.copy(alpha = 0.34f),
                                ),
                            ),
                        ),
                )
            }

            ProductImageLoadState.Loading,
            ProductImageLoadState.Unavailable -> {
                ProductImageFallbackContent(product = product)
            }
        }

        if (showCategoryLabel) {
            Surface(
                modifier = Modifier.align(Alignment.BottomStart),
                shape = RoundedCornerShape(999.dp),
                color = MarketNavyHigh.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            ) {
                Text(
                    text = category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MarketText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProductImageFallbackContent(product: ProductDTO) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.075f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                tint = MarketMuted.copy(alpha = 0.72f),
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun productImageBrush(): Brush {
    return Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            Color(0xFF102A3D).copy(alpha = 0.88f),
            Color(0xFF03111F),
        ),
    )
}

private sealed interface ProductImageLoadState {
    data object Loading : ProductImageLoadState
    data object Unavailable : ProductImageLoadState
    data class Loaded(val bitmap: ImageBitmap) : ProductImageLoadState
}

@Composable
private fun ProductMetaChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MarketMuted,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductStatusMiniChip(
    text: String,
    tone: StatusTone,
    modifier: Modifier = Modifier,
) {
    val color = when (tone) {
        StatusTone.Neutral -> MarketMuted
        StatusTone.Success -> MaterialTheme.colorScheme.primary
        StatusTone.Warning -> Color(0xFFF4B740)
        StatusTone.Error -> MaterialTheme.colorScheme.error
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.24f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductCompactButton(
    text: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (primary) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.1f)
    }
    val containerColor = when {
        !enabled -> Color.White.copy(alpha = 0.04f)
        primary -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.055f)
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> MarketText
    }

    Surface(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProductAddButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(
            1.dp,
            if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.09f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ProductDetailMainScreen(
    productId: String,
    detailStateHolder: ProductDetailStateHolder,
    wishlistStateHolder: WishlistStateHolder,
    cartStateHolder: CartStateHolder,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = detailStateHolder.state

    LaunchedEffect(productId) {
        detailStateHolder.load(productId)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(180)) +
            slideInVertically(animationSpec = tween(220)) { it / 10 },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    cartStateHolder = cartStateHolder,
                    imageLoader = detailStateHolder::fetchImageBytes,
                    onShowMessage = onShowMessage,
                )
            }
        }
    }
}

@Composable
private fun ProductDetailContent(
    product: ProductDTO,
    wishlistStateHolder: WishlistStateHolder,
    cartStateHolder: CartStateHolder,
    imageLoader: suspend (String) -> ByteArray,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val isWishlisted = wishlistStateHolder.isInWishlist(product.id)
    val isWishlistBusy = product.id in wishlistStateHolder.state.busyProductIds
    val isCartBusy = product.id in cartStateHolder.state.busyProductIds
    val minimumOrderQuantity = product.minimumOrderQuantity.coerceAtLeast(1)
    var quantity by remember(product.id) { mutableStateOf(minimumOrderQuantity) }
    val canAddToCart = cartStateHolder.isEnabled && product.stock >= minimumOrderQuantity

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassCard(contentPadding = 10.dp) {
            ProductImageSurface(
                product = product,
                imageLoader = imageLoader,
                showCategoryLabel = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassPill(text = product.category.ifBlank { "Uncategorized" }, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = product.brand.ifBlank { product.supplier.ifBlank { "Marketplace product" } },
                    style = MaterialTheme.typography.bodySmall,
                    color = MarketMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = product.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MarketText,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = product.supplier.ifBlank { "Verified supplier" },
                style = MaterialTheme.typography.bodyMedium,
                color = MarketMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = product.description.ifBlank { "No description is available for this product yet." },
                style = MaterialTheme.typography.bodyMedium,
                color = MarketMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        GlassCard(contentPadding = 14.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = formatPrice(product.price),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(text = stockStatusText(product), tone = stockStatusTone(product))
                        ProductMetaChip(text = "MOQ $minimumOrderQuantity")
                    }
                }
                if (wishlistStateHolder.isEnabled) {
                    ProductCompactButton(
                        text = when {
                            isWishlistBusy -> "Saving"
                            isWishlisted -> "Saved"
                            else -> "Save"
                        },
                        primary = false,
                        enabled = !isWishlistBusy,
                        onClick = {
                            coroutineScope.launch {
                                val result = wishlistStateHolder.toggle(product.id)
                                result.message?.let(onShowMessage)
                            }
                        },
                        modifier = Modifier.width(92.dp),
                    )
                }
            }

            ProductMetaRow(product = product)

            if (cartStateHolder.isEnabled) {
                QuantityPicker(
                    quantity = quantity,
                    minimumOrderQuantity = minimumOrderQuantity,
                    stock = product.stock,
                    enabled = !isCartBusy,
                    onQuantityChange = { quantity = it },
                )

                PrimaryButton(
                    text = when {
                        isCartBusy -> "Adding..."
                        product.stock <= 0 -> "Out of stock"
                        product.stock < minimumOrderQuantity -> "Below MOQ"
                        else -> "Add to cart"
                    },
                    onClick = {
                        coroutineScope.launch {
                            val result = cartStateHolder.addProduct(product, quantity)
                            result.message?.let(onShowMessage)
                        }
                    },
                    enabled = canAddToCart && !isCartBusy,
                )
            } else {
                StatusChip(text = "Browsing only", tone = StatusTone.Neutral)
                Text(
                    "Cart checkout is available for retailer accounts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MarketMuted,
                )
            }
        }

    }
}

@Composable
private fun ProductMetaRow(product: ProductDTO) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductMetaChip(text = ratingLabel(product), modifier = Modifier.weight(1f))
        ProductMetaChip(text = product.gtin.takeIf { it.isNotBlank() }?.let { "GTIN $it" } ?: "No GTIN", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuantityPicker(
    quantity: Int,
    minimumOrderQuantity: Int,
    stock: Int,
    enabled: Boolean,
    onQuantityChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Quantity", style = MaterialTheme.typography.titleSmall, color = MarketText, fontWeight = FontWeight.Black)
            Text(
                "Starts at MOQ $minimumOrderQuantity",
                style = MaterialTheme.typography.bodySmall,
                color = MarketMuted,
            )
        }
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.055f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onQuantityChange((quantity - 1).coerceAtLeast(minimumOrderQuantity)) },
                    enabled = enabled && quantity > minimumOrderQuantity,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity", modifier = Modifier.size(17.dp))
                }
                Text(
                    quantity.toString(),
                    modifier = Modifier.width(34.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MarketText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                )
                IconButton(
                    onClick = { onQuantityChange((quantity + 1).coerceAtMost(stock)) },
                    enabled = enabled && quantity < stock,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity", modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
private fun ProductDetailHeader(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.06f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to marketplace",
                    tint = MarketText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "Product detail",
                style = MaterialTheme.typography.titleMedium,
                color = MarketText,
                fontWeight = FontWeight.Black,
            )
            Text(
                "Back to marketplace",
                style = MaterialTheme.typography.bodySmall,
                color = MarketMuted,
            )
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
    ProductImageSurface(product = product, modifier = modifier)
}

private fun productBrandLine(product: ProductDTO): String {
    val parts = buildList {
        product.brand.takeIf { it.isNotBlank() }?.let(::add)
        product.supplier.takeIf { it.isNotBlank() && it != product.brand }?.let(::add)
    }
    return parts.ifEmpty { listOf("Marketplace product") }.joinToString(" • ")
}

private fun ratingLabel(product: ProductDTO): String {
    if (product.rating <= 0.0) return "No rating"
    val rating = roundedOneDecimal(product.rating)
    return if (product.reviews > 0) "$rating rating" else "Rating $rating"
}

private fun roundedOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

private fun productInitials(product: ProductDTO): String {
    val source = product.name.ifBlank { product.category.ifBlank { "Product" } }
    val initials = source
        .split(' ', '-', '_')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
    return initials.ifBlank { "P" }
}

private fun primaryImageUrl(product: ProductDTO): String? {
    return product.images
        .firstOrNull { it.isNotBlank() }
        ?: product.image.takeIf { it.isNotBlank() }
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

private fun shortStockStatusText(product: ProductDTO): String {
    val stock = product.stock
    val moq = product.minimumOrderQuantity.coerceAtLeast(1)
    return when {
        stock <= 0 -> "Out"
        stock < moq -> "Below MOQ"
        else -> "In stock"
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

private fun formatMoneyAmount(value: Double): String {
    val cents = kotlin.math.round(value * 100.0).toLong()
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$whole.$fraction"
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

private fun sortLabelFromSummary(summary: String): String {
    return summary
        .split(" • ")
        .firstOrNull { it.startsWith("Sort: ") }
        ?.removePrefix("Sort: ")
        ?: "Featured"
}
