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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderGroupDTO
import uqu.drawbridge.platform.OrderItemDTO
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentStatus
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.commerce.CartProductItem
import uqu.drawbridge.platform.ui.commerce.CartStateHolder
import uqu.drawbridge.platform.ui.commerce.OrderStatusFilter
import uqu.drawbridge.platform.ui.commerce.OrdersStateHolder
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.ErrorStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatCard
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.model.SessionState

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

    ScreenSection(
        title = "Cart",
        subtitle = if (state.itemCount == 0) "Review products before checkout." else "${state.itemCount} units ready for review.",
    ) {
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

            state.items.isEmpty() -> EmptyStateCard(
                title = "Your cart is empty",
                message = "Add products from the marketplace when you are ready to order.",
                actionText = "Browse marketplace",
                onAction = onOpenMarketplace,
            )

            else -> {
                if (state.errorMessage != null) {
                    ErrorStateCard(
                        title = "Cart needs attention",
                        message = state.errorMessage,
                        actionText = "Refresh",
                        onAction = { coroutineScope.launch { cartStateHolder.refresh() } },
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
                    itemCount = state.itemCount,
                    estimatedSubtotal = state.estimatedSubtotal,
                    hasInvalidItems = state.hasInvalidItems,
                    isRefreshing = state.isRefreshing,
                        isClearing = state.isClearing,
                        isConfirmingClear = confirmClear,
                        onCheckout = onOpenCheckout,
                        onRefresh = { coroutineScope.launch { cartStateHolder.refresh() } },
                        onCancelClear = { confirmClear = false },
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
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            ProductTile(product = product, modifier = Modifier.size(82.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = product?.name ?: "Product unavailable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(product?.brand, product?.category).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatMoney(product?.price ?: 0.0)} each",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            IconButton(onClick = onRemove, enabled = !isBusy, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error)
            }
        }

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
            Column(horizontalAlignment = Alignment.End) {
                Text("Estimated line total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(item.lineTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatusChip(text = "MOQ ${item.minimumOrderQuantity}", tone = StatusTone.Neutral)
            item.availableStock?.let { stock ->
                StatusChip(
                    text = "$stock in stock",
                    tone = if (stock < item.quantity) StatusTone.Error else StatusTone.Success,
                )
            }
        }

        when {
            item.hasMissingProduct -> StatusChip(
                text = "Unavailable item",
                tone = StatusTone.Error,
            )
            item.isBelowMinimumOrder -> StatusChip(
                text = "Below minimum order quantity",
                tone = StatusTone.Error,
            )
            item.isAboveStock -> StatusChip(
                text = "Quantity exceeds stock",
                tone = StatusTone.Error,
            )
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onDecrease,
            enabled = !isBusy && quantity > minimum,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(
            onClick = onIncrease,
            enabled = !isBusy && maximum?.let { quantity < it } ?: true,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f), RoundedCornerShape(10.dp)),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}

@Composable
private fun CartSummaryCard(
    itemCount: Int,
    estimatedSubtotal: Double,
    hasInvalidItems: Boolean,
    isRefreshing: Boolean,
    isClearing: Boolean,
    isConfirmingClear: Boolean,
    onCheckout: () -> Unit,
    onRefresh: () -> Unit,
    onCancelClear: () -> Unit,
    onClear: () -> Unit,
) {
    AppCard {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Estimated subtotal", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(estimatedSubtotal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }
            StatusChip(text = if (isRefreshing) "Updating" else "$itemCount units", tone = StatusTone.Neutral)
        }
        Text(
            text = "Final totals, stock, and MOQ are checked again by Drawbridge when you place the order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = "Review checkout",
            onClick = onCheckout,
            enabled = !hasInvalidItems && !isRefreshing && !isClearing,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SecondaryButton(text = "Refresh", onClick = onRefresh, modifier = Modifier.weight(1f), enabled = !isRefreshing)
            SecondaryButton(
                text = when {
                    isClearing -> "Clearing..."
                    isConfirmingClear -> "Confirm clear"
                    else -> "Clear"
                },
                onClick = onClear,
                modifier = Modifier.weight(1f),
                enabled = !isClearing,
            )
        }
        if (isConfirmingClear) {
            SecondaryButton(text = "Keep cart", onClick = onCancelClear)
        }
    }
}

@Composable
internal fun CheckoutMainScreen(
    cartStateHolder: CartStateHolder,
    onBackToCart: () -> Unit,
    onViewOrders: () -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val cartState = cartStateHolder.state
    val checkoutState = cartStateHolder.checkoutState

    LaunchedEffect(Unit) {
        cartStateHolder.loadInitial()
    }

    ScreenSection(
        title = "Checkout",
        subtitle = "Confirm your cart and place the order.",
    ) {
        SecondaryButton(text = "Back to cart", onClick = onBackToCart)

        checkoutState.successGroup?.let { group ->
            CheckoutSuccessCard(
                group = group,
                onViewOrders = onViewOrders,
            )
            return@ScreenSection
        }

        if (cartState.items.isEmpty() && cartState.hasLoaded) {
            EmptyStateCard(
                title = "No items to checkout",
                message = "Add products to your cart before placing an order.",
            )
            return@ScreenSection
        }

        if (checkoutState.errorMessage != null) {
            ErrorStateCard(
                title = "Checkout needs attention",
                message = checkoutState.errorMessage,
            )
        }

        AppCard {
            Text("Order summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            cartState.items.forEach { item ->
                CheckoutLineItem(item = item)
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Estimated subtotal", style = MaterialTheme.typography.titleMedium)
                Text(formatMoney(cartState.estimatedSubtotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            }
            Text(
                "Drawbridge recalculates order totals and creates separate wholesaler orders at submission.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppCard {
            StatusChip(text = "Payment pending", tone = StatusTone.Warning)
            Text("Payment method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "This checkout creates an order with ${statusLabel(PaymentStatus.PENDING.name)} payment status. No card charge is processed here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryButton(
            text = if (checkoutState.isSubmitting) "Placing order..." else "Place order",
            onClick = {
                coroutineScope.launch {
                    val result = cartStateHolder.checkout()
                    result.message?.let(onShowMessage)
                }
            },
            enabled = !checkoutState.isSubmitting && cartState.items.isNotEmpty() && !cartState.hasInvalidItems,
        )
    }
}

@Composable
private fun CheckoutLineItem(item: CartProductItem) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.product?.name ?: "Product unavailable",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Qty ${item.quantity} • MOQ ${item.minimumOrderQuantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatMoney(item.lineTotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CheckoutSuccessCard(
    group: OrderGroupDTO,
    onViewOrders: () -> Unit,
) {
    AppCard {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(46.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text("Order placed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(
            "Order ${shortId(group.id)} was created with ${group.orders.size} wholesaler order${if (group.orders.size == 1) "" else "s"}.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Order total", style = MaterialTheme.typography.titleMedium)
            Text(formatMoney(group.groupTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        StatusChip(text = "Payment ${statusLabel(group.paymentStatus.name)}", tone = paymentTone(group.paymentStatus))
        PrimaryButton(text = "View orders", onClick = onViewOrders)
    }
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

    ScreenSection(
        title = if (session.user.role == UserRole.WHOLESALER) "Customer orders" else "Orders",
        subtitle = "Track order status and fulfillment.",
    ) {
        if (state.isLoading) {
            repeat(3) {
                LoadingStateCard(title = "Loading orders", message = "Fetching your latest order history.")
            }
            return@ScreenSection
        }

        OrdersStatsRow(orders = state.orders)

        AppCard {
            AppTextField(
                value = state.searchInput,
                onValueChange = ordersStateHolder::updateSearchInput,
                label = "Search orders",
            )
            OrderFilterRows(
                selected = state.statusFilter,
                onSelected = ordersStateHolder::selectStatusFilter,
            )
            SecondaryButton(
                text = if (state.isRefreshing) "Refreshing..." else "Refresh orders",
                onClick = { coroutineScope.launch { ordersStateHolder.refresh() } },
                enabled = !state.isRefreshing,
            )
        }

        when {
            state.errorMessage != null && state.orders.isEmpty() -> ErrorStateCard(
                title = "Could not load orders",
                message = state.errorMessage,
                actionText = "Try again",
                onAction = { coroutineScope.launch { ordersStateHolder.refresh() } },
            )

            state.filteredOrders.isEmpty() -> EmptyStateCard(
                title = "No orders found",
                message = if (state.orders.isEmpty()) {
                    "Completed checkouts will appear here."
                } else {
                    "Try a different search or status filter."
                },
            )

            else -> state.filteredOrders.forEach { order ->
                OrderCard(
                    order = order,
                    showRetailer = session.user.role == UserRole.WHOLESALER,
                    onOpenOrder = { onOpenOrder(order.id) },
                )
            }
        }
    }
}

@Composable
private fun OrdersStatsRow(orders: List<OrderDTO>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard(value = orders.size.toString(), label = "All", modifier = Modifier.weight(1f))
        StatCard(value = orders.count { it.status == OrderStatus.PENDING }.toString(), label = "Pending", modifier = Modifier.weight(1f))
        StatCard(value = orders.count { it.status == OrderStatus.DELIVERED }.toString(), label = "Done", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OrderFilterRows(
    selected: OrderStatusFilter,
    onSelected: (OrderStatusFilter) -> Unit,
) {
    OrderStatusFilter.entries.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            row.forEach { filter ->
                SecondaryButton(
                    text = if (selected == filter) "${filter.label} selected" else filter.label,
                    onClick = { onSelected(filter) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size < 2) {
                repeat(2 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderDTO,
    showRetailer: Boolean,
    onOpenOrder: () -> Unit,
) {
    AppCard(
        modifier = Modifier.clickable(onClick = onOpenOrder),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("#${shortId(order.id)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(
                    formatDate(order.placedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (showRetailer) {
                    Text(
                        order.retailerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            StatusChip(text = statusLabel(order.status.name), tone = orderStatusTone(order.status))
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("${order.items.sumOf { it.quantity }} units", style = MaterialTheme.typography.bodyMedium)
            Text(formatMoney(order.subtotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
        }
        if (order.items.isNotEmpty()) {
            Text(
                order.items.take(2).joinToString { it.productName },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SecondaryButton(text = "View detail", onClick = onOpenOrder)
    }
}

@Composable
internal fun OrderDetailMainScreen(
    orderId: String,
    ordersStateHolder: OrdersStateHolder,
    session: SessionState,
    onBack: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val state = ordersStateHolder.detailState

    LaunchedEffect(orderId) {
        ordersStateHolder.loadOrder(orderId)
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

            state.order != null -> OrderDetailContent(order = state.order, session = session)
        }
    }
}

@Composable
private fun DetailHeader(
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

@Composable
private fun OrderDetailContent(
    order: OrderDTO,
    session: SessionState,
) {
    ScreenSection(
        title = "#${shortId(order.id)}",
        subtitle = "Placed ${formatDate(order.placedAt)}",
    ) {
        AppCard {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusChip(text = statusLabel(order.status.name), tone = orderStatusTone(order.status))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(order.subtotal), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
            }
            if (session.user.role == UserRole.WHOLESALER) {
                Text("Retailer: ${order.retailerName}", style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Payment status is tracked on the checkout order group. This order can move through fulfillment after wholesaler processing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AppCard {
            Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            order.items.forEach { item ->
                OrderItemRow(item = item)
            }
        }

        AppCard {
            Text("Fulfillment timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TimelineLine(label = "Placed", value = order.placedAt)
            TimelineLine(label = "Shipped", value = order.shippedAt)
            TimelineLine(label = "Delivered", value = order.deliveredAt)
            if (order.trackingNumber != null) {
                Text(
                    "Tracking ${order.trackingNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItemDTO) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(item.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(
                "${item.productCategory} • Qty ${item.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatMoney(item.unitPrice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(item.unitPrice * item.quantity), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black)
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
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
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
