package uqu.drawbridge.platform.ui.screens

import coil3.compose.AsyncImage
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.AddressResponseDto
import uqu.drawbridge.platform.InvoiceDTO
import uqu.drawbridge.platform.MobileApiConfig
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderGroupDTO
import uqu.drawbridge.platform.OrderItemDTO
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentMethodDTO
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.commerce.CartProductItem
import uqu.drawbridge.platform.ui.commerce.CartStateHolder
import uqu.drawbridge.platform.ui.commerce.CheckoutAddressForm
import uqu.drawbridge.platform.ui.commerce.CheckoutPaymentForm
import uqu.drawbridge.platform.ui.commerce.CheckoutStep
import uqu.drawbridge.platform.ui.commerce.OrderStatusFilter
import uqu.drawbridge.platform.ui.commerce.OrdersStateHolder
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppPageHeader
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.ErrorStateCard
import uqu.drawbridge.platform.ui.components.GlassCard
import uqu.drawbridge.platform.ui.components.GlassPill
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.ServerErrorCard
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.common.ServerNotFoundMessage
import uqu.drawbridge.platform.ui.model.SessionState

private val CommerceText = Color(0xFFF8FAFC)
private val CommerceMuted = Color(0xFFA8B7C7)
private val CommerceNavy = Color(0xFF03111F)
private val CommerceNavyHigh = Color(0xFF102A3D)
private val OrdersCardBg = Color(0xFF102A3D).copy(alpha = 0.92f)
private val OrdersInk = CommerceText
private val OrdersSubtle = CommerceMuted
private val OrdersMuted = Color(0xFF9BADBF)
private val OrdersBorder = Color.White.copy(alpha = 0.12f)
private val activeOrderStatuses = setOf(OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED)

private enum class OrderActionKind {
    Cancel,
    Confirm,
    Process,
    Ship,
    ConfirmReceipt,
    Invoice,
}

private data class OrderActionSpec(
    val kind: OrderActionKind,
    val label: String,
    val helper: String,
    val primary: Boolean = false,
    val destructive: Boolean = false,
    val requiresConfirmation: Boolean = false,
    val confirmTitle: String = label,
    val confirmMessage: String = "Continue with this order action?",
)

@Composable
internal fun CartMainScreen(
    cartStateHolder: CartStateHolder,
    onOpenMarketplace: () -> Unit,
    onOpenCheckout: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = cartStateHolder.state
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cartStateHolder.loadInitial()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CartHeader(
            itemCount = state.items.size,
            isClearing = state.isClearing,
            onBack = onOpenMarketplace,
            onClear = {
                if (!confirmClear) {
                    confirmClear = true
                } else {
                    coroutineScope.launch {
                        val result = cartStateHolder.clearCart()
                        confirmClear = false
                        result.message?.let(onShowMessage)
                    }
                }
            },
        )

        when {
            state.isLoading -> {
                repeat(3) {
                    LoadingStateCard(title = "Loading cart", message = "Checking latest quantities and stock.")
                }
            }

            state.errorMessage != null && state.items.isEmpty() -> ErrorStateCard(
                title = "Could not load cart",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { cartStateHolder.refresh() } },
            )

            state.items.isEmpty() -> CartEmptyState(onOpenMarketplace = onOpenMarketplace)

            else -> {
                if (state.errorMessage != null) {
                    ErrorStateCard(
                        title = "Cart needs attention",
                        message = state.errorMessage,
                        actionText = "Refresh",
                        onAction = { coroutineScope.launch { cartStateHolder.refresh() } },
                    )
                }
                if (confirmClear) {
                    ErrorStateCard(
                        title = "Clear cart?",
                        message = "Tap the cart icon again to confirm, or keep browsing to cancel.",
                        actionText = "Keep cart",
                        onAction = { confirmClear = false },
                    )
                }

                state.items.forEach { item ->
                    CartItemCard(
                        item = item,
                        isBusy = item.productId in state.busyProductIds,
                        onDecrease = {
                            coroutineScope.launch {
                                val result = cartStateHolder.updateQuantity(item, item.quantity - 1)
                                result.message?.let(onShowMessage)
                            }
                        },
                        onIncrease = {
                            coroutineScope.launch {
                                val result = cartStateHolder.updateQuantity(item, item.quantity + 1)
                                result.message?.let(onShowMessage)
                            }
                        },
                        onRemove = {
                            coroutineScope.launch {
                                val result = cartStateHolder.removeItem(item)
                                result.message?.let(onShowMessage)
                            }
                        },
                    )
                }

                CartSummaryCard(
                    estimatedSubtotal = state.estimatedSubtotal,
                    hasInvalidItems = state.hasInvalidItems,
                    isRefreshing = state.isRefreshing,
                    isClearing = state.isClearing,
                    onCheckout = onOpenCheckout,
                    onRefresh = { coroutineScope.launch { cartStateHolder.refresh() } },
                )
            }
        }
    }
}

@Composable
private fun CartEmptyState(
    onOpenMarketplace: () -> Unit,
) {
    CommerceStatePanel(
        label = "Empty",
        title = "Your cart is empty",
        message = "Add products from the marketplace when you are ready to order.",
        actionText = "Browse marketplace",
        onAction = onOpenMarketplace,
    )
}

@Composable
private fun CartHeader(
    itemCount: Int,
    isClearing: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
) {
    AppPageHeader(
        title = "Shopping Cart",
        subtitle = "$itemCount ${if (itemCount == 1) "item" else "items"}",
        leading = {
            CheckoutIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
        },
        trailing = {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = itemCount > 0 && !isClearing, onClick = onClear),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.055f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Clear cart",
                        tint = if (itemCount > 0) CommerceText else CommerceMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
    )
}

@Composable
private fun CommerceStatePanel(
    label: String,
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = OrdersCardBg,
        border = BorderStroke(1.dp, OrdersBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassPill(text = label, tint = OrdersMuted)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = OrdersInk,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OrdersSubtle,
            )
            if (actionText != null && onAction != null) {
                SecondaryButton(text = actionText, onClick = onAction)
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartProductItem,
    isBusy: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
) {
    val product = item.product
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.075f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                CartProductImageSurface(
                    product = product,
                    modifier = Modifier.size(112.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                text = product?.name ?: "Product unavailable",
                                style = MaterialTheme.typography.titleMedium,
                                color = CommerceText,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = productMetaLine(product),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CommerceMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = onRemove, enabled = !isBusy, modifier = Modifier.size(34.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove item",
                                tint = CommerceMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = "${formatMoney(product?.price ?: 0.0)} / unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CommerceText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "MOQ: ${item.minimumOrderQuantity} units",
                        style = MaterialTheme.typography.bodySmall,
                        color = CommerceMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuantityStepper(
                    quantity = item.quantity,
                    minimum = item.minimumOrderQuantity,
                    maximum = item.availableStock,
                    isBusy = isBusy,
                    onDecrease = onDecrease,
                    onIncrease = onIncrease,
                )
                Text(
                    formatMoney(item.lineTotal),
                    style = MaterialTheme.typography.titleLarge,
                    color = CommerceText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CartItemIssue(item = item)
        }
    }
}

@Composable
private fun QuantityStepper(
    quantity: Int,
    minimum: Int,
    maximum: Int?,
    isBusy: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuantityStepButton(
            icon = Icons.Default.Remove,
            enabled = !isBusy && quantity > minimum,
            contentDescription = "Decrease quantity",
            onClick = onDecrease,
        )
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = CommerceText,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp),
            maxLines = 1,
        )
        QuantityStepButton(
            icon = Icons.Default.Add,
            enabled = !isBusy && maximum?.let { quantity < it } ?: true,
            contentDescription = "Increase quantity",
            onClick = onIncrease,
        )
    }
}

@Composable
private fun QuantityStepButton(
    icon: ImageVector,
    enabled: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (enabled) 0.065f else 0.035f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) CommerceText else CommerceMuted.copy(alpha = 0.55f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CartItemIssue(item: CartProductItem) {
    val issue = when {
        item.hasMissingProduct -> "Unavailable item"
        item.isBelowMinimumOrder -> "Below minimum order quantity"
        item.isAboveStock -> "Quantity exceeds stock"
        else -> null
    }
    if (issue != null) {
        StatusChip(text = issue, tone = StatusTone.Error)
    }
}

@Composable
private fun CartProductImageSurface(
    product: ProductDTO?,
    modifier: Modifier = Modifier,
) {
    val imageUrl = remember(product?.id, product?.image, product?.images?.contentHashCode()) {
        product?.let(::primaryProductImageUrl)
    }
    val resolvedImageUrl = remember(imageUrl) {
        imageUrl?.let(MobileApiConfig::resolveResourceUrl)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cartImageBrush()),
        contentAlignment = Alignment.Center,
    ) {
        ProductTile(product = product, modifier = Modifier.fillMaxSize())
        if (resolvedImageUrl != null) {
            AsyncImage(
                model = resolvedImageUrl,
                contentDescription = product?.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun cartImageBrush(): Brush {
    return Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            CommerceNavyHigh.copy(alpha = 0.9f),
            CommerceNavy,
        ),
    )
}

@Composable
private fun CartSummaryCard(
    estimatedSubtotal: Double,
    hasInvalidItems: Boolean,
    isRefreshing: Boolean,
    isClearing: Boolean,
    onCheckout: () -> Unit,
    onRefresh: () -> Unit,
) {
    val total = estimatedSubtotal
    GlassCard(contentPadding = 16.dp) {
        SummaryLine(label = "Subtotal", value = formatMoney(estimatedSubtotal))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Total", style = MaterialTheme.typography.titleMedium, color = CommerceText, fontWeight = FontWeight.Black)
            Text(formatMoney(total), style = MaterialTheme.typography.headlineSmall, color = CommerceText, fontWeight = FontWeight.Black)
        }
        PrimaryButton(
            text = if (isRefreshing) "Updating..." else "Proceed to Checkout",
            onClick = onCheckout,
            enabled = !hasInvalidItems && !isRefreshing && !isClearing,
        )
        SecondaryButton(text = if (isRefreshing) "Refreshing..." else "Refresh cart", onClick = onRefresh, enabled = !isRefreshing)
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = CommerceMuted, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = CommerceText, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun CheckoutMainScreen(
    cartStateHolder: CartStateHolder,
    onBackToCart: () -> Unit,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val cartState = cartStateHolder.state
    val checkoutState = cartStateHolder.checkoutState

    LaunchedEffect(Unit) {
        cartStateHolder.loadInitial()
        cartStateHolder.prepareCheckout()
    }

    val subtotal = checkoutSubtotal(cartState.items)
    val total = checkoutTotal(subtotal)
    val selectedAddress = checkoutState.addresses.firstOrNull { it.id == checkoutState.selectedAddressId }
    val selectedPayment = checkoutState.paymentMethods.firstOrNull { it.id == checkoutState.selectedPaymentMethodId }
    val backAction = {
        when (checkoutState.step) {
            CheckoutStep.Delivery -> onBackToCart()
            CheckoutStep.Payment -> cartStateHolder.goToCheckoutStep(CheckoutStep.Delivery)
            CheckoutStep.Confirm -> cartStateHolder.goToCheckoutStep(CheckoutStep.Payment)
            CheckoutStep.Success -> onContinueShopping()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (checkoutState.step != CheckoutStep.Success) {
            CheckoutHeader(
                itemCount = cartState.itemCount,
                total = total,
                onBack = backAction,
            )
            CheckoutStepper(step = checkoutState.step)
        }

        checkoutState.successGroup?.let { group ->
            CheckoutSuccessScreen(
                group = group,
                address = selectedAddress,
                payment = selectedPayment,
                onViewOrders = onViewOrders,
                onContinueShopping = onContinueShopping,
            )
            return@Column
        }

        if (cartState.items.isEmpty() && cartState.hasLoaded) {
            EmptyStateCard(
                title = "No items to checkout",
                message = "Add products to your cart before placing an order.",
            )
            return@Column
        }

        if (checkoutState.errorMessage != null) {
            ErrorStateCard(
                title = "Checkout needs attention",
                message = checkoutState.errorMessage,
            )
        }

        if (cartState.isLoading || checkoutState.isLoadingDetails) {
            LoadingStateCard(title = "Preparing checkout", message = "Loading delivery and payment details.")
            return@Column
        }

        AnimatedContent(
            targetState = checkoutState.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = slideInHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                ) { if (forward) it / 3 else -it / 3 } + fadeIn(animationSpec = tween(180))
                val exit = slideOutHorizontally(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) { if (forward) -it / 4 else it / 4 } + fadeOut(animationSpec = tween(140))
                enter.togetherWith(exit).using(SizeTransform(clip = false))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { step ->
            when (step) {
                CheckoutStep.Delivery -> CheckoutDeliveryStep(
                    addresses = checkoutState.addresses,
                    selectedAddressId = checkoutState.selectedAddressId,
                    notes = checkoutState.notes,
                    addressForm = checkoutState.addressForm,
                    isAddingAddress = checkoutState.isAddingAddress,
                    isSavingDetails = checkoutState.isSavingDetails,
                    items = cartState.items,
                    total = total,
                    onSelectAddress = cartStateHolder::selectCheckoutAddress,
                    onNotesChange = cartStateHolder::updateCheckoutNotes,
                    onToggleAddressForm = cartStateHolder::toggleCheckoutAddressForm,
                    onAddressFormChange = cartStateHolder::updateCheckoutAddressForm,
                    onSaveAddress = {
                        coroutineScope.launch {
                            val result = cartStateHolder.saveCheckoutAddress()
                            result.message?.let(onShowMessage)
                        }
                    },
                    onContinue = {
                        if (checkoutState.selectedAddressId == null) {
                            onShowMessage("Choose a delivery address.")
                        } else {
                            cartStateHolder.goToCheckoutStep(CheckoutStep.Payment)
                        }
                    },
                )

                CheckoutStep.Payment -> CheckoutPaymentStep(
                    paymentMethods = checkoutState.paymentMethods,
                    selectedPaymentMethodId = checkoutState.selectedPaymentMethodId,
                    paymentForm = checkoutState.paymentForm,
                    isAddingPaymentMethod = checkoutState.isAddingPaymentMethod,
                    isSavingDetails = checkoutState.isSavingDetails,
                    total = total,
                    onSelectPayment = cartStateHolder::selectCheckoutPaymentMethod,
                    onTogglePaymentForm = cartStateHolder::toggleCheckoutPaymentForm,
                    onPaymentFormChange = cartStateHolder::updateCheckoutPaymentForm,
                    onSavePaymentMethod = {
                        coroutineScope.launch {
                            val result = cartStateHolder.saveCheckoutPaymentMethod()
                            result.message?.let(onShowMessage)
                        }
                    },
                    onContinue = {
                        if (checkoutState.selectedPaymentMethodId == null) {
                            onShowMessage("Choose a payment card.")
                        } else {
                            cartStateHolder.goToCheckoutStep(CheckoutStep.Confirm)
                        }
                    },
                )

                CheckoutStep.Confirm -> CheckoutConfirmStep(
                    address = selectedAddress,
                    payment = selectedPayment,
                    items = cartState.items,
                    subtotal = subtotal,
                    total = total,
                    isSubmitting = checkoutState.isSubmitting,
                    onEditDelivery = { cartStateHolder.goToCheckoutStep(CheckoutStep.Delivery) },
                    onEditPayment = { cartStateHolder.goToCheckoutStep(CheckoutStep.Payment) },
                    onPlaceOrder = {
                        coroutineScope.launch {
                            val result = cartStateHolder.checkout()
                            result.message?.let(onShowMessage)
                        }
                    },
                )

                CheckoutStep.Success -> Unit
            }
        }
    }
}

@Composable
private fun CheckoutHeader(
    itemCount: Int,
    total: Double,
    onBack: () -> Unit,
) {
    GlassCard(contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CheckoutIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Checkout",
                    style = MaterialTheme.typography.headlineSmall,
                    color = CommerceText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = "$itemCount ${if (itemCount == 1) "item" else "items"} · ${formatMoney(total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CommerceMuted,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CheckoutStepper(step: CheckoutStep) {
    val steps = listOf(CheckoutStep.Delivery, CheckoutStep.Payment, CheckoutStep.Confirm)
    val activeIndex = when (step) {
        CheckoutStep.Success -> steps.size
        else -> step.ordinal
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            steps.forEachIndexed { index, checkoutStep ->
                CheckoutStepNode(
                    number = index + 1,
                    label = checkoutStep.name,
                    active = activeIndex == index,
                    completed = activeIndex > index,
                    modifier = Modifier.weight(1f),
                )
                if (index != steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(0.75f)
                            .padding(top = 19.dp)
                            .height(2.dp)
                            .background(
                                if (activeIndex > index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutStepNode(
    number: Int,
    label: String,
    active: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(999.dp),
            color = when {
                completed || active -> MaterialTheme.colorScheme.primary
                else -> Color.White.copy(alpha = 0.08f)
            },
            border = BorderStroke(1.dp, if (completed || active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                } else {
                    Text(
                        number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (active) Color.White else CommerceMuted,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active || completed) CommerceText else CommerceMuted,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun CheckoutDeliveryStep(
    addresses: List<AddressResponseDto>,
    selectedAddressId: String?,
    notes: String,
    addressForm: CheckoutAddressForm,
    isAddingAddress: Boolean,
    isSavingDetails: Boolean,
    items: List<CartProductItem>,
    total: Double,
    onSelectAddress: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleAddressForm: () -> Unit,
    onAddressFormChange: (CheckoutAddressForm) -> Unit,
    onSaveAddress: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        CheckoutSectionCard(title = "Delivery Address", icon = Icons.Default.LocationOn) {
            addresses.forEachIndexed { index, address ->
                CheckoutAddressOption(
                    address = address,
                    label = checkoutAddressLabel(index),
                    selected = address.id == selectedAddressId,
                    onClick = { onSelectAddress(address.id) },
                )
            }
            CheckoutDashedRow(
                text = if (isAddingAddress) "Close New Address" else "Add New Address",
                onClick = onToggleAddressForm,
            )
            if (isAddingAddress) {
                CheckoutAddressFormCard(
                    form = addressForm,
                    isSaving = isSavingDetails,
                    onChange = onAddressFormChange,
                    onSave = onSaveAddress,
                )
            }
        }
        CheckoutSectionCard(title = "Order Notes", icon = Icons.AutoMirrored.Filled.Notes) {
            CheckoutNotesField(value = notes, onValueChange = onNotesChange)
        }
        CheckoutItemsCard(items = items)
        CheckoutActionCard(total = total, buttonText = "Continue to Payment", onClick = onContinue)
    }
}

@Composable
private fun CheckoutPaymentStep(
    paymentMethods: List<PaymentMethodDTO>,
    selectedPaymentMethodId: String?,
    paymentForm: CheckoutPaymentForm,
    isAddingPaymentMethod: Boolean,
    isSavingDetails: Boolean,
    total: Double,
    onSelectPayment: (String) -> Unit,
    onTogglePaymentForm: () -> Unit,
    onPaymentFormChange: (CheckoutPaymentForm) -> Unit,
    onSavePaymentMethod: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        CheckoutSectionCard(title = "Payment Method", icon = Icons.Default.CreditCard) {
            paymentMethods.forEach { paymentMethod ->
                CheckoutPaymentOption(
                    payment = paymentMethod,
                    selected = paymentMethod.id == selectedPaymentMethodId,
                    onClick = { onSelectPayment(paymentMethod.id) },
                )
            }
            CheckoutDashedRow(
                text = if (isAddingPaymentMethod) "Close New Payment Method" else "Add New Payment Method",
                onClick = onTogglePaymentForm,
            )
            if (isAddingPaymentMethod) {
                CheckoutPaymentFormCard(
                    form = paymentForm,
                    isSaving = isSavingDetails,
                    onChange = onPaymentFormChange,
                    onSave = onSavePaymentMethod,
                )
            }
        }
        CheckoutSectionCard(title = "Card Details", icon = Icons.Default.Security) {
            val selected = paymentMethods.firstOrNull { it.id == selectedPaymentMethodId }
            Text(
                text = selected?.maskedDetails ?: "Saved card",
                style = MaterialTheme.typography.titleMedium,
                color = CommerceText,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "Payment is handled securely with the saved demo card.",
                style = MaterialTheme.typography.bodyMedium,
                color = CommerceMuted,
            )
        }
        CheckoutActionCard(total = total, buttonText = "Review Order", onClick = onContinue)
    }
}

@Composable
private fun CheckoutConfirmStep(
    address: AddressResponseDto?,
    payment: PaymentMethodDTO?,
    items: List<CartProductItem>,
    subtotal: Double,
    total: Double,
    isSubmitting: Boolean,
    onEditDelivery: () -> Unit,
    onEditPayment: () -> Unit,
    onPlaceOrder: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
        CheckoutSectionCard(title = "Delivery Address", icon = Icons.Default.LocationOn) {
            CheckoutSummaryBlock(
                icon = Icons.Default.LocationOn,
                title = "HQ Office",
                body = address?.let { checkoutAddressBody(it) } ?: "No address selected",
                action = "Edit",
                onAction = onEditDelivery,
            )
        }
        CheckoutSectionCard(title = "Payment", icon = Icons.Default.CreditCard) {
            CheckoutSummaryBlock(
                icon = Icons.Default.CreditCard,
                title = "Credit / Debit Card",
                body = payment?.maskedDetails ?: "Saved card",
                action = "Edit",
                onAction = onEditPayment,
            )
        }
        CheckoutItemsCard(items = items, title = "Order Items (${items.size})")
        CheckoutTotalsCard(subtotal = subtotal, total = total)
        PrimaryButton(
            text = if (isSubmitting) "Processing payment..." else "Place Order · ${formatMoney(total)}",
            onClick = onPlaceOrder,
            enabled = !isSubmitting && items.isNotEmpty(),
        )
    }
}

@Composable
private fun CheckoutSuccessScreen(
    group: OrderGroupDTO,
    address: AddressResponseDto?,
    payment: PaymentMethodDTO?,
    onViewOrders: () -> Unit,
    onContinueShopping: () -> Unit,
) {
    val total = checkoutTotal(group.groupTotal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = "Order Placed!",
            style = MaterialTheme.typography.headlineMedium,
            color = CommerceText,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Your order has been confirmed.\nOrder #${shortId(group.id)}",
            style = MaterialTheme.typography.bodyLarge,
            color = CommerceMuted,
            textAlign = TextAlign.Center,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CheckoutSummaryLine("Deliver to", address?.city?.let { "HQ Office, $it" } ?: "HQ Office")
                CheckoutSummaryLine("Payment", payment?.maskedDetails ?: "Credit / Debit Card")
                CheckoutSummaryLine("Total", formatMoney(total), strong = true)
            }
        }
        PrimaryButton(text = "Track My Order", onClick = onViewOrders)
        SecondaryButton(text = "Continue Shopping", onClick = onContinueShopping)
    }
}

@Composable
private fun CheckoutSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.075f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CheckoutIconTile(icon = icon)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CommerceText,
                    fontWeight = FontWeight.Black,
                )
            }
            CheckoutDivider()
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun CheckoutAddressOption(
    address: AddressResponseDto,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CheckoutSelectableRow(selected = selected, onClick = onClick) {
        CheckoutRadioDot(selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = CommerceText, fontWeight = FontWeight.Black)
                if (selected) {
                    StatusChip(text = "Default", tone = StatusTone.Success)
                }
            }
            Text(
                text = checkoutAddressBody(address),
                style = MaterialTheme.typography.bodyMedium,
                color = CommerceMuted,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CheckoutPaymentOption(
    payment: PaymentMethodDTO,
    selected: Boolean,
    onClick: () -> Unit,
) {
    CheckoutSelectableRow(selected = selected, onClick = onClick) {
        CheckoutRadioDot(selected = selected)
        CheckoutIconTile(icon = Icons.Default.CreditCard, selected = selected)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Credit / Debit Card", style = MaterialTheme.typography.titleMedium, color = CommerceText, fontWeight = FontWeight.Black)
            Text(payment.maskedDetails, style = MaterialTheme.typography.bodyMedium, color = CommerceMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CheckoutSelectableRow(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.045f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.13f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun CheckoutRadioDot(selected: Boolean) {
    Surface(
        modifier = Modifier.size(28.dp),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        border = BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.primary else CommerceMuted.copy(alpha = 0.55f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(Color.White, RoundedCornerShape(999.dp)),
                )
            }
        }
    }
}

@Composable
private fun CheckoutDashedRow(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = CommerceMuted, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = CommerceMuted, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun CheckoutAddressFormCard(
    form: CheckoutAddressForm,
    isSaving: Boolean,
    onChange: (CheckoutAddressForm) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(13.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CheckoutInputField(form.street, { onChange(form.copy(street = it)) }, "Street")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CheckoutInputField(form.city, { onChange(form.copy(city = it)) }, "City", modifier = Modifier.weight(1f))
            CheckoutInputField(form.state, { onChange(form.copy(state = it)) }, "State", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CheckoutInputField(form.zipCode, { onChange(form.copy(zipCode = it)) }, "Zip code", modifier = Modifier.weight(1f))
            CheckoutInputField(form.country, { onChange(form.copy(country = it)) }, "Country", modifier = Modifier.weight(1f))
        }
        PrimaryButton(
            text = if (isSaving) "Saving address..." else "Save Address",
            onClick = onSave,
            enabled = !isSaving,
        )
    }
}

@Composable
private fun CheckoutPaymentFormCard(
    form: CheckoutPaymentForm,
    isSaving: Boolean,
    onChange: (CheckoutPaymentForm) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(13.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CheckoutInputField(form.cardNumber, { onChange(form.copy(cardNumber = it)) }, "Card number")
        CheckoutInputField(form.cardholderName, { onChange(form.copy(cardholderName = it)) }, "Cardholder name")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            CheckoutInputField(form.expiry, { onChange(form.copy(expiry = it)) }, "MM/YY", modifier = Modifier.weight(1f))
            CheckoutInputField(form.cvv, { onChange(form.copy(cvv = it)) }, "CVV", modifier = Modifier.weight(1f))
        }
        Text(
            text = "Only masked card details are saved.",
            style = MaterialTheme.typography.bodySmall,
            color = CommerceMuted,
            fontWeight = FontWeight.SemiBold,
        )
        PrimaryButton(
            text = if (isSaving) "Saving payment..." else "Save Payment Method",
            onClick = onSave,
            enabled = !isSaving,
        )
    }
}

@Composable
private fun CheckoutInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CommerceText, fontWeight = FontWeight.SemiBold),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth(),
    ) { innerField ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(Color.White.copy(alpha = 0.055f), RoundedCornerShape(12.dp))
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isBlank()) {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = CommerceMuted.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            innerField()
        }
    }
}

@Composable
private fun CheckoutNotesField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = CommerceText),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
    ) { innerField ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .background(Color.White.copy(alpha = 0.045f), RoundedCornerShape(12.dp))
                .padding(14.dp),
        ) {
            if (value.isBlank()) {
                Text(
                    "Special instructions for this order...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CommerceMuted,
                )
            }
            innerField()
        }
    }
}

@Composable
private fun CheckoutItemsCard(
    items: List<CartProductItem>,
    title: String = "Items (${items.size})",
) {
    CheckoutSectionCard(title = title, icon = Icons.Default.Inventory2) {
        items.forEach { item ->
            CheckoutLineItem(item = item)
        }
    }
}

@Composable
private fun CheckoutLineItem(item: CartProductItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.product?.name ?: "Product unavailable",
                style = MaterialTheme.typography.titleSmall,
                color = CommerceText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.product?.supplier ?: "Supplier"} · x${item.quantity}",
                style = MaterialTheme.typography.bodyMedium,
                color = CommerceMuted,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(formatMoney(item.lineTotal), style = MaterialTheme.typography.titleSmall, color = CommerceText, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CheckoutTotalsCard(
    subtotal: Double,
    total: Double,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CheckoutSummaryLine("Subtotal", formatMoney(subtotal))
            CheckoutSummaryLine("Shipping", "Free")
            CheckoutDivider()
            CheckoutSummaryLine("Total", formatMoney(total), strong = true)
        }
    }
}

@Composable
private fun CheckoutActionCard(
    total: Double,
    buttonText: String,
    onClick: () -> Unit,
) {
    GlassCard(contentPadding = 16.dp) {
        CheckoutSummaryLine("Total", formatMoney(total), strong = true)
        PrimaryButton(text = buttonText, onClick = onClick)
    }
}

@Composable
private fun CheckoutSummaryBlock(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckoutIconTile(icon = icon)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CommerceText, fontWeight = FontWeight.Black)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = CommerceMuted, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = action,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
            modifier = Modifier.clickable(onClick = onAction),
        )
    }
}

@Composable
private fun CheckoutSummaryLine(
    label: String,
    value: String,
    strong: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (strong) CommerceText else CommerceMuted, fontWeight = if (strong) FontWeight.Black else FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = CommerceText, fontWeight = FontWeight.Black, textAlign = TextAlign.End)
    }
}

@Composable
private fun CheckoutIconTile(
    icon: ImageVector,
    selected: Boolean = false,
) {
    Surface(
        modifier = Modifier.size(42.dp),
        shape = RoundedCornerShape(11.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else CommerceMuted,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun CheckoutIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = CommerceText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun CheckoutDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
    )
}

private fun checkoutAddressLabel(index: Int): String {
    return if (index == 0) "HQ Office" else "Warehouse"
}

private fun checkoutAddressBody(address: AddressResponseDto): String {
    return "${address.street}\n${address.city}, ${address.zipCode}"
}

private fun checkoutSubtotal(items: List<CartProductItem>): Double {
    return checkoutRoundMoney(items.sumOf { it.lineTotal })
}

private fun checkoutTotal(subtotal: Double): Double {
    return checkoutRoundMoney(subtotal)
}

private fun checkoutRoundMoney(value: Double): Double {
    return kotlin.math.round(value * 100.0) / 100.0
}

@Composable
internal fun OrdersMainScreen(
    ordersStateHolder: OrdersStateHolder,
    session: SessionState,
    onOpenOrder: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = ordersStateHolder.state

    LaunchedEffect(Unit) {
        ordersStateHolder.loadInitial()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OrdersCatalogHeader(session = session)

        if (state.isLoading) {
            OrdersEmptyPanel(title = "Loading orders", message = "Fetching your latest order history.", label = "Loading")
            return@Column
        }

        OrdersStatsRow(orders = state.orders)

        OrdersSearchField(
            value = state.searchInput,
            onValueChange = ordersStateHolder::updateSearchInput,
        )

        OrderFilterRows(
            selected = state.statusFilter,
            onSelected = ordersStateHolder::selectStatusFilter,
        )

        if (state.isRefreshing) {
            Text(
                text = "Refreshing orders",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }

        when {
            state.errorMessage != null && state.orders.isEmpty() -> OrdersEmptyPanel(
                title = "Could not load orders",
                message = state.errorMessage,
                label = "Issue",
                actionText = "Try again",
                onAction = { coroutineScope.launch { ordersStateHolder.refresh() } },
            )

            state.filteredOrders.isEmpty() -> OrdersEmptyPanel(
                title = "No orders found",
                message = if (state.orders.isEmpty()) {
                    "Orders will appear here once placed"
                } else {
                    "Try a different search or status filter."
                },
            )

            else -> state.filteredOrders.forEach { order ->
                OrderCard(
                    order = order,
                    role = session.user.role,
                    onOpenOrder = { onOpenOrder(order.id) },
                )
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun OrdersCatalogHeader(session: SessionState) {
    AppPageHeader(
        title = if (session.user.role == UserRole.WHOLESALER) "Customer Orders" else "My Orders",
        subtitle = if (session.user.role == UserRole.WHOLESALER) {
            "Track fulfillment and customer delivery status"
        } else {
            "Track your purchases and delivery status"
        },
    )
}

@Composable
private fun OrdersStatsRow(orders: List<OrderDTO>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OrderStatTile(
            value = orders.count { it.status == OrderStatus.PENDING }.toString(),
            label = "Pending",
            icon = Icons.Default.AccessTime,
            tint = Color(0xFFFFA726),
            modifier = Modifier.weight(1f),
        )
        OrderStatTile(
            value = orders.count { it.status in activeOrderStatuses }.toString(),
            label = "Active",
            icon = Icons.Default.ShoppingCart,
            tint = Color(0xFF4C86FF),
            modifier = Modifier.weight(1f),
        )
        OrderStatTile(
            value = orders.count { it.status == OrderStatus.DELIVERED }.toString(),
            label = "Delivered",
            icon = Icons.Default.CheckCircle,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OrdersEmptyPanel(
    title: String,
    message: String,
    label: String = "Empty",
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val isServerNotFound = message == ServerNotFoundMessage
    if (isServerNotFound) {
        ServerErrorCard(actionText = actionText, onAction = onAction)
        return
    }

    CommerceStatePanel(
        label = label,
        title = title,
        message = message,
        actionText = actionText,
        onAction = onAction,
    )
}

@Composable
private fun OrderStatTile(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(14.dp),
        color = OrdersCardBg,
        border = BorderStroke(1.dp, OrdersBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = OrdersInk,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = OrdersSubtle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OrdersSearchField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = OrdersCardBg,
        border = BorderStroke(1.dp, OrdersBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = OrdersMuted,
                modifier = Modifier.size(20.dp),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isBlank()) {
                    Text(
                        text = "Search orders...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = OrdersMuted,
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
                        color = OrdersInk,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

@Composable
private fun OrderFilterRows(
    selected: OrderStatusFilter,
    onSelected: (OrderStatusFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OrderStatusFilter.entries.forEach { filter ->
            OrderFilterChip(
                text = filter.label,
                selected = selected == filter,
                onClick = { onSelected(filter) },
            )
        }
    }
}

@Composable
private fun OrderFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else OrdersCardBg,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.36f) else OrdersBorder,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else OrdersInk,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderDTO,
    role: UserRole,
    onOpenOrder: () -> Unit,
) {
    val nextAction = nextOrderActionLabel(order = order, role = role)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenOrder),
        shape = RoundedCornerShape(14.dp),
        color = OrdersCardBg,
        border = BorderStroke(1.dp, OrdersBorder),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "ORD-${shortId(order.id).uppercase()}",
                        style = MaterialTheme.typography.titleLarge,
                        color = OrdersInk,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        orderPartyLine(order = order, showRetailer = role == UserRole.WHOLESALER),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OrdersSubtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        formatDate(order.placedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = OrdersSubtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(text = statusLabel(order.status.name), tone = orderStatusTone(order.status))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(OrdersBorder, RoundedCornerShape(999.dp)),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OrderInlineMetric(label = "Items:", value = order.items.size.toString(), modifier = Modifier.weight(0.78f))
                OrderInlineMetric(label = "Total:", value = formatMoney(order.subtotal), modifier = Modifier.weight(1.12f))
                Text(
                    text = "View Details",
                    modifier = Modifier.weight(0.9f),
                    style = MaterialTheme.typography.labelLarge,
                    color = OrdersInk,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            nextAction?.let { label ->
                Text(
                    text = "Next: $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun OrderInlineMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OrdersSubtle,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = OrdersInk,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun OrderDetailMainScreen(
    orderId: String,
    ordersStateHolder: OrdersStateHolder,
    session: SessionState,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = ordersStateHolder.detailState
    var pendingAction by remember { mutableStateOf<OrderActionSpec?>(null) }

    LaunchedEffect(orderId) {
        ordersStateHolder.loadOrder(orderId)
    }

    fun performAction(action: OrderActionSpec) {
        coroutineScope.launch {
            val result = when (action.kind) {
                OrderActionKind.Cancel -> ordersStateHolder.cancelOrder(orderId)
                OrderActionKind.Confirm -> ordersStateHolder.updateOrderStatus(orderId, OrderStatus.CONFIRMED)
                OrderActionKind.Process -> ordersStateHolder.updateOrderStatus(orderId, OrderStatus.PROCESSING)
                OrderActionKind.Ship -> ordersStateHolder.updateOrderStatus(orderId, OrderStatus.SHIPPED)
                OrderActionKind.ConfirmReceipt -> ordersStateHolder.confirmDelivery(orderId)
                OrderActionKind.Invoice -> ordersStateHolder.loadInvoice(orderId)
            }
            result.message?.let(onShowMessage)
        }
    }

    pendingAction?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(action.confirmTitle) },
            text = { Text(action.confirmMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingAction = null
                        performAction(action)
                    },
                ) {
                    Text(action.label)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text(if (action.destructive) "Keep order" else "Not now")
                }
            },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailHeader(title = "Order detail", subtitle = "#${shortId(orderId)}", onBack = onBack)

        when {
            state.isLoading -> LoadingStateCard(
                title = "Loading order",
                message = "Fetching items, totals, and fulfillment status.",
            )

            state.errorMessage != null -> ErrorStateCard(
                title = "Order unavailable",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { ordersStateHolder.loadOrder(orderId) } },
            )

            state.order != null -> OrderDetailContent(
                order = state.order,
                session = session,
                invoice = state.invoice,
                isActionInProgress = state.isActionInProgress,
                onAction = { action ->
                    if (action.requiresConfirmation) {
                        pendingAction = action
                    } else {
                        performAction(action)
                    }
                },
            )
        }
    }
}

@Composable
private fun DetailHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
) {
    GlassCard(contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = CommerceText)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = CommerceText, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = CommerceMuted)
            }
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OrderDTO,
    session: SessionState,
    invoice: InvoiceDTO?,
    isActionInProgress: Boolean,
    onAction: (OrderActionSpec) -> Unit,
) {
    val actions = orderActionsFor(order = order, role = session.user.role)
    ScreenSection(
        title = "#${shortId(order.id)}",
        subtitle = "Placed ${formatDate(order.placedAt)}",
    ) {
        GlassCard(contentPadding = 16.dp) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    StatusChip(text = statusLabel(order.status.name), tone = orderStatusTone(order.status))
                    Text(
                        text = if (session.user.role == UserRole.WHOLESALER) {
                            order.retailerName.ifBlank { "Retail customer" }
                        } else {
                            "Supplier details from order invoice"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CommerceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", style = MaterialTheme.typography.labelLarge, color = CommerceMuted)
                    Text(formatMoney(order.subtotal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = CommerceText)
                }
            }
        }

        GlassCard(contentPadding = 16.dp) {
            Text("Order Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CommerceText)
            OrderTimeline(order = order)
        }

        GlassCard(contentPadding = 16.dp) {
            Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CommerceText)
            order.items.forEach { item ->
                OrderItemRow(item = item)
            }
        }

        GlassCard(contentPadding = 16.dp) {
            Text("Order Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CommerceText)
            SummaryLine(label = "Subtotal (${order.items.sumOf { it.quantity }} items)", value = formatMoney(order.subtotal))
            SummaryLine(label = "Status", value = statusLabel(order.status.name))
            SummaryLine(label = "Placed", value = formatDate(order.placedAt))
        }

        InvoicePreviewCard(order = order, invoice = invoice)

        if (actions.isNotEmpty() || invoice != null) {
            GlassCard(contentPadding = 16.dp) {
                Text("Order Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = CommerceText)
                actions.forEach { action ->
                    OrderActionButton(
                        action = action,
                        enabled = !isActionInProgress,
                        onClick = { onAction(action) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderTimeline(order: OrderDTO) {
    val steps = if (order.status == OrderStatus.CANCELLED) {
        listOf(OrderStatus.PENDING, OrderStatus.CANCELLED)
    } else {
        listOf(OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        steps.forEach { step ->
            val reached = order.status == step || (order.status != OrderStatus.CANCELLED && order.status.ordinal >= step.ordinal)
            val current = order.status == step
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (reached) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(999.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (step == OrderStatus.PENDING) Icons.Default.AccessTime else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (reached) MaterialTheme.colorScheme.onPrimary else CommerceMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timelineLabel(step),
                        style = MaterialTheme.typography.bodyLarge,
                        color = CommerceText,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = timelineDate(order, step) ?: if (current) "Current step" else if (reached) "Completed" else "Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = CommerceMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderActionButton(
    action: OrderActionSpec,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (action.destructive) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = if (action.primary) 0.9f else 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (enabled) action.label else "Working...",
                style = MaterialTheme.typography.labelLarge,
                color = if (action.primary) MaterialTheme.colorScheme.onPrimary else CommerceText,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = action.helper,
                style = MaterialTheme.typography.labelSmall,
                color = if (action.primary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f) else CommerceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun InvoicePreviewCard(order: OrderDTO, invoice: InvoiceDTO?) {
    GlassCard(contentPadding = 16.dp) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text("Drawbridge Invoice", style = MaterialTheme.typography.titleMedium, color = CommerceText, fontWeight = FontWeight.Black)
                Text("Invoice #${invoice?.invoiceNumber ?: shortId(order.id).uppercase()}", style = MaterialTheme.typography.bodySmall, color = CommerceMuted)
                Text("Date ${formatDate(invoice?.issueDate ?: order.placedAt)}", style = MaterialTheme.typography.bodySmall, color = CommerceMuted)
            }
            StatusChip(text = statusLabel(order.status.name), tone = orderStatusTone(order.status))
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            order.items.forEach { item ->
                SummaryLine(
                    label = "${item.productName.take(22)} x ${item.quantity}",
                    value = formatMoney(item.unitPrice * item.quantity),
                )
            }
        }
        SummaryLine(label = "Invoice total", value = invoice?.let { "${it.currency} ${roundMoney(it.totalAmount)}" } ?: formatMoney(order.subtotal))
    }
}

@Composable
private fun OrderItemRow(
    item: OrderItemDTO,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        OrderItemImageSurface(item = item)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.productName,
                style = MaterialTheme.typography.bodyMedium,
                color = CommerceText,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.productCategory} • Qty ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = CommerceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatMoney(item.unitPrice * item.quantity), style = MaterialTheme.typography.bodyMedium, color = CommerceText, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun OrderItemImageSurface(
    item: OrderItemDTO,
) {
    val imageUrl = item.productImageUrl?.takeIf { it.isNotBlank() }
    val resolvedImageUrl = remember(imageUrl) {
        imageUrl?.let(MobileApiConfig::resolveResourceUrl)
    }

    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cartImageBrush()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.productName.split(' ').mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
        )
        if (resolvedImageUrl != null) {
            AsyncImage(
                model = resolvedImageUrl,
                contentDescription = item.productName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun TimelineLine(label: String, value: String?) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value?.takeIf { it.isNotBlank() }?.let(::formatDate) ?: "Not yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProductTile(product: ProductDTO?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(cartImageBrush(), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                product?.category?.ifBlank { "Item" } ?: "Item",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun productMetaLine(product: ProductDTO?): String {
    if (product == null) return "Item unavailable"
    val parts = buildList {
        product.brand.takeIf { it.isNotBlank() }?.let(::add)
        product.category.takeIf { it.isNotBlank() }?.let(::add)
    }
    return parts.ifEmpty { listOf("Marketplace product") }.joinToString(" • ")
}

private fun primaryProductImageUrl(product: ProductDTO): String? {
    return product.images
        .firstOrNull { it.isNotBlank() }
        ?: product.image.takeIf { it.isNotBlank() }
}

private fun orderActionsFor(order: OrderDTO, role: UserRole): List<OrderActionSpec> {
    val status = order.status
    val roleAction = when (role) {
        UserRole.WHOLESALER -> when (status) {
            OrderStatus.PENDING -> OrderActionSpec(
                kind = OrderActionKind.Confirm,
                label = "Confirm order",
                helper = "Accept request",
                primary = true,
            )
            OrderStatus.CONFIRMED -> OrderActionSpec(
                kind = OrderActionKind.Process,
                label = "Mark processing",
                helper = "Prepare items",
                primary = true,
            )
            OrderStatus.PROCESSING -> OrderActionSpec(
                kind = OrderActionKind.Ship,
                label = "Mark shipped",
                helper = "Move forward",
                primary = true,
            )
            else -> null
        }
        UserRole.RETAILER -> when (status) {
            OrderStatus.PENDING, OrderStatus.CONFIRMED -> OrderActionSpec(
                kind = OrderActionKind.Cancel,
                label = "Cancel order",
                helper = "Before shipment",
                destructive = true,
                requiresConfirmation = true,
                confirmTitle = "Cancel this order?",
                confirmMessage = "This will cancel the order with the wholesaler.",
            )
            OrderStatus.SHIPPED -> OrderActionSpec(
                kind = OrderActionKind.ConfirmReceipt,
                label = "Confirm receipt",
                helper = "Mark delivered",
                primary = true,
                requiresConfirmation = true,
                confirmTitle = "Confirm receipt?",
                confirmMessage = "This marks the order delivered and updates retailer inventory.",
            )
            else -> null
        }
    }

    return buildList {
        roleAction?.let(::add)
        add(
            OrderActionSpec(
                kind = OrderActionKind.Invoice,
                label = "View invoice",
                helper = "Order billing",
            ),
        )
    }
}

private fun nextOrderActionLabel(order: OrderDTO, role: UserRole): String? {
    return orderActionsFor(order, role).firstOrNull { it.kind != OrderActionKind.Invoice }?.label
}

private fun timelineLabel(status: OrderStatus): String {
    return when (status) {
        OrderStatus.PENDING -> "Order placed"
        OrderStatus.CONFIRMED -> "Confirmed"
        OrderStatus.PROCESSING -> "Processing"
        OrderStatus.SHIPPED -> "Shipped"
        OrderStatus.DELIVERED -> "Delivered"
        OrderStatus.CANCELLED -> "Cancelled"
        OrderStatus.RETURNED -> "Returned"
    }
}

private fun timelineDate(order: OrderDTO, status: OrderStatus): String? {
    return when (status) {
        OrderStatus.PENDING -> order.placedAt
        OrderStatus.SHIPPED -> order.shippedAt
        OrderStatus.DELIVERED -> order.deliveredAt
        else -> null
    }?.takeIf { it.isNotBlank() }?.let(::formatDate)
}

private fun formatMoney(value: Double): String = "SAR ${roundMoney(value)}"

private fun roundMoney(value: Double): String {
    val rounded = kotlin.math.round(value * 100.0) / 100.0
    val text = rounded.toString()
    val decimals = text.substringAfter('.', "")
    return when (decimals.length) {
        0 -> "$text.00"
        1 -> "${text}0"
        else -> text
    }
}

private fun shortId(id: String): String = id.take(8).ifBlank { "pending" }

private fun formatDate(value: String): String = value.take(10).ifBlank { "Unknown date" }

private fun orderPartyLine(order: OrderDTO, showRetailer: Boolean): String {
    if (showRetailer) {
        return order.retailerName.ifBlank { "Retail customer" }
    }
    return order.items.firstOrNull()?.productCategory?.takeIf { it.isNotBlank() } ?: "Marketplace order"
}

private fun statusLabel(value: String): String {
    return value.lowercase()
        .split('_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { token -> token.replaceFirstChar { it.uppercase() } }
}

private fun orderStatusTone(status: OrderStatus): StatusTone {
    return when (status) {
        OrderStatus.DELIVERED -> StatusTone.Success
        OrderStatus.SHIPPED, OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> StatusTone.Warning
        OrderStatus.CANCELLED, OrderStatus.RETURNED -> StatusTone.Error
        OrderStatus.PENDING -> StatusTone.Neutral
    }
}

private fun paymentTone(status: PaymentStatus): StatusTone {
    return when (status) {
        PaymentStatus.COMPLETED -> StatusTone.Success
        PaymentStatus.FAILED, PaymentStatus.CANCELLED -> StatusTone.Error
        PaymentStatus.PENDING, PaymentStatus.PROCESSING -> StatusTone.Warning
        PaymentStatus.REFUNDED -> StatusTone.Neutral
    }
}
