package uqu.drawbridge.platform.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uqu.drawbridge.platform.DashboardSummary
import uqu.drawbridge.platform.NotificationDTO
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.commerce.OrdersStateHolder
import uqu.drawbridge.platform.ui.components.GlassCard
import uqu.drawbridge.platform.ui.components.GlassIconLabelRow
import uqu.drawbridge.platform.ui.components.GlassIconTile
import uqu.drawbridge.platform.ui.components.GlassPill
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.engagement.NotificationsStateHolder
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.theme.AppDarkLine
import uqu.drawbridge.platform.ui.theme.AppMutedText
import uqu.drawbridge.platform.ui.theme.AppNavyBase
import uqu.drawbridge.platform.ui.theme.AppNavySurfaceHigh
import uqu.drawbridge.platform.ui.theme.ErrorColor
import uqu.drawbridge.platform.ui.theme.Primary500
import uqu.drawbridge.platform.ui.theme.SuccessColor
import uqu.drawbridge.platform.ui.theme.WarningColor
import kotlin.math.round

private val DashboardNavy = AppNavyBase
private val DashboardSurfaceHigh = AppNavySurfaceHigh
private val DashboardLine = AppDarkLine
private val DashboardText = Color(0xFFF8FAFC)
private val DashboardMuted = AppMutedText

@Composable
internal fun DashboardMainScreen(
    dashboardSummary: DashboardSummary?,
    session: SessionState,
    ordersStateHolder: OrdersStateHolder,
    notificationsStateHolder: NotificationsStateHolder,
    onRefresh: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val ordersState = ordersStateHolder.state
    val notificationsState = notificationsStateHolder.state
    val orders = ordersState.orders.sortedByDescending { it.placedAt }
    val hasOrderData = ordersState.hasLoaded || orders.isNotEmpty()
    val isOrdersInitialLoading = !ordersState.hasLoaded && orders.isEmpty()
    val isOrdersLoadingEmpty = isOrdersInitialLoading || (ordersState.isLoading && !hasOrderData)
    val isRetailer = session.user.role == UserRole.RETAILER
    val businessName = session.user.company
        .ifBlank { session.user.name }
        .ifBlank { session.user.email.substringBefore("@") }

    LaunchedEffect(ordersStateHolder) {
        ordersStateHolder.loadInitial()
    }
    LaunchedEffect(notificationsStateHolder) {
        notificationsStateHolder.load()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DashboardHeroCard(
            businessName = businessName,
            subtitle = if (isRetailer) {
                "Buying activity, spend, and order movement across your workspace."
            } else {
                "Sales, fulfillment, and order movement across your workspace."
            },
            roleLabel = if (isRetailer) "Retailer workspace" else "Wholesaler workspace",
            onRefresh = onRefresh,
        )

        DashboardKpiGrid(
            items = dashboardKpis(
                isRetailer = isRetailer,
                summary = dashboardSummary,
                orders = orders,
                hasOrderData = hasOrderData,
            ),
        )

        MonthlySummaryCard(
            title = if (isRetailer) "Monthly Expenses" else "Monthly Sales",
            orders = orders,
            summaryTotal = dashboardSummary?.totalAmount,
            isLoading = isOrdersLoadingEmpty,
        )

        if (isRetailer) {
            PurchaseMixCard(orders = orders, hasLoaded = hasOrderData)
        } else {
            OrderStatusSummaryCard(orders = orders, hasLoaded = hasOrderData)
        }

        RecentOrdersCard(
            orders = orders.take(4),
            isLoading = isOrdersLoadingEmpty,
            errorMessage = ordersState.errorMessage,
            onViewAll = onOpenOrders,
            onOpenOrder = onOpenOrder,
        )

        RecentActivityCard(
            notifications = notificationsState.notifications.take(4),
            fallbackOrders = orders.take(4),
            isLoading = notificationsState.isLoading,
            errorMessage = notificationsState.errorMessage,
        )
    }
}

@Composable
private fun DashboardHeroCard(
    businessName: String,
    subtitle: String,
    roleLabel: String,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Primary500.copy(alpha = 0.18f),
                            DashboardSurfaceHigh.copy(alpha = 0.92f),
                            DashboardNavy.copy(alpha = 0.96f),
                        ),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassPill(text = roleLabel)
                    Text(
                        text = "Welcome back, $businessName",
                        style = MaterialTheme.typography.titleLarge,
                        color = DashboardText,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DashboardMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Surface(
                    modifier = Modifier
                        .size(46.dp)
                        .clickable(onClick = onRefresh),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Primary500.copy(alpha = 0.28f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh dashboard",
                            tint = Primary500,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiGrid(items: List<DashboardKpi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    DashboardKpiCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiCard(
    item: DashboardKpi,
    modifier: Modifier = Modifier,
) {
    DashboardCard(modifier = modifier.height(94.dp), contentPadding = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashboardIconTile(icon = item.icon, tint = item.tint, size = 30.dp)
            Text(
                text = item.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = DashboardMuted,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = item.value,
            style = MaterialTheme.typography.headlineSmall,
            color = DashboardText,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MonthlySummaryCard(
    title: String,
    orders: List<OrderDTO>,
    summaryTotal: Double?,
    isLoading: Boolean,
) {
    val buckets = monthlyAnalyticsBuckets(orders)
    val latestMonth = buckets.lastOrNull()
    DashboardSectionCard(
        title = title,
        actionLabel = latestMonth?.label ?: "Latest month",
        icon = Icons.Default.CheckCircle,
        modifier = Modifier.heightIn(min = 176.dp),
    ) {
        when {
            isLoading -> {
                DashboardMonthlySkeleton()
            }
            buckets.isEmpty() -> DashboardEmptyLine("Monthly totals will appear after orders are placed.")
            else -> DashboardMonthlyFinanceChart(
                buckets = buckets,
                summaryTotal = summaryTotal,
            )
        }
    }
}

@Composable
private fun PurchaseMixCard(
    orders: List<OrderDTO>,
    hasLoaded: Boolean,
) {
    val categories = orders
        .flatMap { it.items.toList() }
        .groupBy { it.productCategory.ifBlank { "Uncategorized" } }
        .map { (category, items) -> DashboardBucket(category, items.sumOf { it.quantity }.toDouble()) }
        .sortedByDescending { it.amount }
        .take(4)
    val maxAmount = categories.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val totalAmount = categories.sumOf { it.amount }.coerceAtLeast(1.0)
    val colors = dashboardSegmentColors()

    DashboardSectionCard(
        title = "Purchase Mix",
        actionLabel = "By units",
        icon = Icons.Default.Search,
        modifier = Modifier.heightIn(min = 164.dp),
    ) {
        if (categories.isEmpty()) {
            DashboardEmptyLine(if (hasLoaded) "Category mix will appear after buying activity." else "Loading buying mix from orders.")
        } else {
            DashboardSegmentedBar(buckets = categories, colors = colors)
            categories.forEachIndexed { index, bucket ->
                val share = (bucket.amount / totalAmount) * 100.0
                DashboardProgressRow(
                    label = bucket.label,
                    value = "${bucket.amount.toInt()} units • ${roundCompact(share)}%",
                    fraction = (bucket.amount / maxAmount).toFloat(),
                    tint = colors.getOrElse(index) { Primary500 },
                )
            }
        }
    }
}

@Composable
private fun OrderStatusSummaryCard(
    orders: List<OrderDTO>,
    hasLoaded: Boolean,
) {
    val statuses = listOf(
        OrderStatus.DELIVERED,
        OrderStatus.SHIPPED,
        OrderStatus.CONFIRMED,
        OrderStatus.PROCESSING,
        OrderStatus.PENDING,
        OrderStatus.CANCELLED,
        OrderStatus.RETURNED,
    ).map { status -> DashboardBucket(status.label(), orders.count { it.status == status }.toDouble()) }
        .filter { it.amount > 0.0 }
    val maxAmount = statuses.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val totalAmount = statuses.sumOf { it.amount }.coerceAtLeast(1.0)

    DashboardSectionCard(
        title = "Order Status",
        actionLabel = "Live mix",
        icon = Icons.Default.CheckCircle,
        modifier = Modifier.heightIn(min = 164.dp),
    ) {
        if (statuses.isEmpty()) {
            DashboardEmptyLine(if (hasLoaded) "Order status mix will appear after orders arrive." else "Loading order status.")
        } else {
            DashboardSegmentedBar(
                buckets = statuses,
                colors = statuses.map { statusColor(it.label) },
            )
            statuses.forEach { bucket ->
                val share = (bucket.amount / totalAmount) * 100.0
                DashboardProgressRow(
                    label = bucket.label,
                    value = "${bucket.amount.toInt()} • ${roundCompact(share)}%",
                    fraction = (bucket.amount / maxAmount).toFloat(),
                    tint = statusColor(bucket.label),
                )
            }
        }
    }
}

@Composable
private fun RecentOrdersCard(
    orders: List<OrderDTO>,
    isLoading: Boolean,
    errorMessage: String?,
    onViewAll: () -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    DashboardSectionCard(
        title = "Recent Orders",
        actionLabel = "View all",
        icon = Icons.Default.ShoppingCart,
        onAction = onViewAll,
        modifier = Modifier.heightIn(min = 188.dp),
    ) {
        when {
            isLoading -> {
                DashboardSkeletonRow()
                DashboardSkeletonRow()
            }
            errorMessage != null && orders.isEmpty() -> DashboardEmptyLine(errorMessage)
            orders.isEmpty() -> DashboardEmptyLine("No recent orders yet.")
            else -> orders.forEach { order ->
                DashboardOrderRow(order = order, onClick = { onOpenOrder(order.id) })
            }
        }
    }
}

@Composable
private fun DashboardOrderRow(
    order: OrderDTO,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashboardIconTile(icon = Icons.Default.ShoppingCart, tint = Primary500, size = 36.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Order #${shortId(order.id)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = DashboardText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatDate(order.placedAt)} • ${order.items.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = DashboardMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(
                modifier = Modifier.width(108.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = formatCompactMoney(order.subtotal),
                    style = MaterialTheme.typography.labelLarge,
                    color = DashboardText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusChip(text = order.status.label(), tone = order.status.tone())
            }
        }
    }
}

@Composable
private fun RecentActivityCard(
    notifications: List<NotificationDTO>,
    fallbackOrders: List<OrderDTO>,
    isLoading: Boolean,
    errorMessage: String?,
) {
    DashboardSectionCard(
        title = "Recent Activity",
        actionLabel = "Timeline",
        icon = Icons.Default.Notifications,
        modifier = Modifier.heightIn(min = 164.dp),
    ) {
        val hasActivityContent = notifications.isNotEmpty() || fallbackOrders.isNotEmpty()
        when {
            isLoading && !hasActivityContent -> {
                DashboardSkeletonRow()
                DashboardSkeletonRow()
            }
            notifications.isNotEmpty() -> notifications.forEach { notification ->
                DashboardActivityRow(
                    title = notification.title,
                    detail = "${notification.time} • ${notification.message}",
                    tint = if (notification.read) DashboardMuted else Primary500,
                )
            }
            fallbackOrders.isNotEmpty() -> fallbackOrders.forEach { order ->
                DashboardActivityRow(
                    title = "Order movement #${shortId(order.id)}",
                    detail = "${formatDate(order.placedAt)} • ${formatMoney(order.subtotal)}",
                    tint = order.status.color(),
                )
            }
            errorMessage != null -> DashboardEmptyLine(errorMessage)
            else -> DashboardEmptyLine("No recent activity yet.")
        }
        if (isLoading && hasActivityContent) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DashboardActivityRow(
    title: String,
    detail: String,
    tint: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(tint, CircleShape),
            )
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .width(2.dp)
                    .background(DashboardLine.copy(alpha = 0.45f)),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = DashboardText,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = DashboardMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DashboardSectionCard(
    title: String,
    actionLabel: String,
    icon: ImageVector,
    tint: Color = Primary500,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    DashboardCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashboardSectionLabel(
                icon = icon,
                text = title,
                tint = tint,
                modifier = Modifier.weight(1f),
            )
            if (onAction == null) {
                DashboardGlassLabel(text = actionLabel)
            } else {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = Primary500, fontWeight = FontWeight.Bold)
                }
            }
        }
        content()
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = modifier,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
private fun DashboardSectionLabel(
    icon: ImageVector,
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    GlassIconLabelRow(icon = icon, text = text, tint = tint, modifier = modifier)
}

@Composable
private fun DashboardGlassLabel(
    text: String,
) {
    GlassPill(text = text, tint = DashboardMuted)
}

@Composable
private fun DashboardIconTile(
    icon: ImageVector,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    GlassIconTile(icon = icon, tint = tint, size = size)
}

@Composable
private fun DashboardValuePanel(
    value: String,
    insight: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Primary500.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, Primary500.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = DashboardText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (insight != null) {
                Text(
                    text = insight,
                    style = MaterialTheme.typography.bodySmall,
                    color = DashboardMuted,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DashboardMonthlyFinanceChart(
    buckets: List<DashboardMonthBucket>,
    summaryTotal: Double?,
) {
    val maxAmount = chartAxisCeiling(buckets.maxOfOrNull { it.amount } ?: 0.0)
    val latest = buckets.last()
    val previous = buckets.dropLast(1).lastOrNull()
    val comparison = monthlyComparisonText(latest, previous, summaryTotal)
    val axisTop = maxAmount
    val axisMid = maxAmount / 2.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Current month",
                        style = MaterialTheme.typography.labelMedium,
                        color = DashboardMuted,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = latest.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = DashboardText,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatMoney(latest.amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = DashboardText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    buckets.forEachIndexed { index, bucket ->
                        val isCurrent = index == buckets.lastIndex
                        DashboardStackedMonthBar(
                            bucket = bucket,
                            maxAmount = maxAmount,
                            selected = isCurrent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .height(82.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    DashboardAxisLabel(formatAxisMoney(axisTop))
                    DashboardAxisLabel(formatAxisMoney(axisMid))
                    DashboardAxisLabel(formatAxisMoney(0.0))
                }
            }

            if (comparison != null) {
                Surface(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(999.dp),
                    color = Primary500.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Primary500.copy(alpha = 0.14f)),
                ) {
                    Text(
                        text = comparison,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF8EF3C5),
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
private fun DashboardStackedMonthBar(
    bucket: DashboardMonthBucket,
    maxAmount: Double,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val barFraction = if (bucket.amount <= 0.0) {
                0f
            } else {
                (bucket.amount / maxAmount).toFloat().coerceIn(0.08f, 1f)
            }
            if (bucket.amount <= 0.0) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(barFraction)
                        .width(if (selected) 18.dp else 14.dp)
                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp)),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    val segments = normalizedSegments(bucket)
                    segments.forEach { segment ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(segment.weight)
                                .background(segment.color.copy(alpha = if (selected) 0.96f else 0.36f)),
                        )
                    }
                }
            }
        }
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF8EF3C5) else DashboardMuted,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DashboardAxisLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = DashboardMuted,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

@Composable
private fun DashboardProgressRow(
    label: String,
    value: String,
    fraction: Float,
    tint: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = DashboardText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = DashboardMuted,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                    .background(tint, RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun DashboardSkeletonBars() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(0.42f, 0.68f, 0.54f, 0.82f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(fraction)
                        .fillMaxWidth(0.62f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                )
            }
        }
    }
}

@Composable
private fun DashboardMonthlySkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                listOf(0.22f, 0.48f, 0.32f, 0.72f).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(76.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(fraction)
                                .width(14.dp)
                                .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSkeletonRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.1f)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.075f)),
                )
            }
        }
    }
}

@Composable
private fun DashboardSegmentedBar(
    buckets: List<DashboardBucket>,
    colors: List<Color>,
) {
    val visibleBuckets = buckets.filter { it.amount > 0.0 }
    val total = visibleBuckets.sumOf { it.amount }.coerceAtLeast(1.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f)),
    ) {
        visibleBuckets.forEachIndexed { index, bucket ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight((bucket.amount / total).toFloat().coerceAtLeast(0.04f))
                    .background(colors.getOrElse(index) { Primary500 }),
            )
        }
    }
}

@Composable
private fun DashboardEmptyLine(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = DashboardMuted,
        )
    }
}

private data class DashboardKpi(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
)

private data class DashboardBucket(
    val label: String,
    val amount: Double,
)

private data class DashboardMonthBucket(
    val label: String,
    val amount: Double,
    val segments: List<DashboardSegment>,
)

private data class DashboardSegment(
    val amount: Double,
    val color: Color,
)

private data class NormalizedSegment(
    val weight: Float,
    val color: Color,
)

private fun dashboardKpis(
    isRetailer: Boolean,
    summary: DashboardSummary?,
    orders: List<OrderDTO>,
    hasOrderData: Boolean,
): List<DashboardKpi> {
    val loadingValue = "..."
    val totalOrders = summary?.totalOrders?.toString() ?: if (hasOrderData) orders.size.toString() else loadingValue
    val pendingOrders = summary?.pendingOrders?.toString()
        ?: if (hasOrderData) orders.count { it.status == OrderStatus.PENDING }.toString() else loadingValue
    val processingOrders = summary?.processingOrders?.toString()
        ?: if (hasOrderData) orders.count { it.status == OrderStatus.PROCESSING }.toString() else loadingValue
    val totalAmount = summary?.totalAmount ?: if (hasOrderData) orders.sumOf { it.subtotal } else null

    return listOf(
        DashboardKpi("Total Orders", totalOrders, Icons.Default.ShoppingCart, Primary500),
        DashboardKpi("Pending Orders", pendingOrders, Icons.Default.Notifications, WarningColor),
        DashboardKpi(if (isRetailer) "Total Spend" else "Total Revenue", totalAmount?.let(::formatCompactMoney) ?: loadingValue, Icons.Default.CheckCircle, SuccessColor),
        DashboardKpi("Processing", processingOrders, Icons.Default.Search, Color(0xFF7DD3FC)),
    )
}

private fun monthlyAnalyticsBuckets(orders: List<OrderDTO>): List<DashboardMonthBucket> {
    val groupedOrders = orders
        .mapNotNull { order -> order.monthKeyOrNull()?.let { monthKey -> monthKey to order } }
        .groupBy({ it.first }, { it.second })
    val latestMonthKey = groupedOrders.keys.maxOrNull() ?: return emptyList()
    return lastFourMonthKeys(latestMonthKey).map { key ->
        val bucketOrders = groupedOrders[key].orEmpty()
        DashboardMonthBucket(
            label = formatMonthKey(key),
            amount = bucketOrders.sumOf { it.subtotal },
            segments = bucketOrders
                .groupBy { it.status }
                .map { (status, statusOrders) ->
                    DashboardSegment(
                        amount = statusOrders.sumOf { it.subtotal },
                        color = status.color(),
                    )
                }
                .filter { it.amount > 0.0 },
        )
    }
}

private fun OrderDTO.monthKeyOrNull(): String? {
    val key = placedAt.take(7)
    return if (key.length == 7 && key[4] == '-' && key.take(4).all { it.isDigit() } && key.takeLast(2).all { it.isDigit() }) {
        key
    } else {
        null
    }
}

private fun lastFourMonthKeys(latestKey: String): List<String> {
    val year = latestKey.take(4).toIntOrNull() ?: return listOf(latestKey)
    val month = latestKey.takeLast(2).toIntOrNull() ?: return listOf(latestKey)
    return (3 downTo 0).map { offset ->
        val monthIndex = (year * 12 + (month - 1)) - offset
        val itemYear = monthIndex / 12
        val itemMonth = (monthIndex % 12) + 1
        "$itemYear-${itemMonth.toString().padStart(2, '0')}"
    }
}

private fun chartAxisCeiling(value: Double): Double {
    val step = when {
        value <= 100.0 -> 10.0
        value <= 1_000.0 -> 100.0
        value <= 10_000.0 -> 1_000.0
        value <= 100_000.0 -> 5_000.0
        else -> 50_000.0
    }
    return (kotlin.math.ceil(value.coerceAtLeast(1.0) / step) * step).coerceAtLeast(step)
}

private fun monthlyComparisonText(
    latest: DashboardMonthBucket,
    previous: DashboardMonthBucket?,
    summaryTotal: Double?,
): String? {
    return when {
        previous != null && previous.amount > 0.0 -> {
            val delta = latest.amount - previous.amount
            when {
                kotlin.math.abs(delta) < 0.01 -> "In line with last month"
                delta < 0.0 -> "↓ ${formatCompactMoney(kotlin.math.abs(delta))} vs last month"
                else -> "↑ ${formatCompactMoney(delta)} vs last month"
            }
        }
        summaryTotal != null -> "Tracked total ${formatMoney(summaryTotal)}"
        else -> null
    }
}

private fun normalizedSegments(bucket: DashboardMonthBucket): List<NormalizedSegment> {
    val visibleSegments = bucket.segments.filter { it.amount > 0.0 }
    if (visibleSegments.isEmpty()) {
        return listOf(NormalizedSegment(weight = 1f, color = Color(0xFFE7EAF0)))
    }
    val total = visibleSegments.sumOf { it.amount }.coerceAtLeast(1.0)
    return visibleSegments.map { segment ->
        NormalizedSegment(
            weight = (segment.amount / total).toFloat().coerceAtLeast(0.08f),
            color = segment.color,
        )
    }
}

private fun formatAxisMoney(value: Double): String {
    val absolute = kotlin.math.abs(value)
    return when {
        absolute >= 1_000_000.0 -> "${roundCompact(value / 1_000_000.0)}M"
        absolute >= 1_000.0 -> "${roundCompact(value / 1_000.0)}K"
        else -> roundCompact(value)
    }
}

private fun formatMonthKey(key: String): String {
    val month = key.substringAfter("-", missingDelimiterValue = key).toIntOrNull()
    val name = when (month) {
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
        else -> key
    }
    return name
}

private fun formatMoney(value: Double): String = "SAR ${roundMoney(value)}"

private fun formatCompactMoney(value: Double): String {
    val absolute = kotlin.math.abs(value)
    return when {
        absolute >= 1_000_000.0 -> "SAR ${roundCompact(value / 1_000_000.0)}M"
        absolute >= 1_000.0 -> "SAR ${roundCompact(value / 1_000.0)}K"
        else -> formatMoney(value)
    }
}

private fun roundCompact(value: Double): String {
    val rounded = round(value * 10.0) / 10.0
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

private fun roundMoney(value: Double): String {
    val rounded = round(value * 100.0) / 100.0
    val text = rounded.toString()
    val decimals = text.substringAfter('.', "")
    return when (decimals.length) {
        0 -> "$text.00"
        1 -> "${text}0"
        else -> text
    }
}

private fun formatDate(value: String): String = value.take(10).ifBlank { "Date unavailable" }

private fun shortId(value: String): String = value.takeLast(4).uppercase()

private fun OrderStatus.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

private fun OrderStatus.tone(): StatusTone = when (this) {
    OrderStatus.DELIVERED -> StatusTone.Success
    OrderStatus.SHIPPED, OrderStatus.PROCESSING, OrderStatus.CONFIRMED -> StatusTone.Warning
    OrderStatus.CANCELLED, OrderStatus.RETURNED -> StatusTone.Error
    OrderStatus.PENDING -> StatusTone.Neutral
}

private fun OrderStatus.color(): Color = when (this) {
    OrderStatus.DELIVERED -> SuccessColor
    OrderStatus.SHIPPED, OrderStatus.CONFIRMED -> Color(0xFF7DD3FC)
    OrderStatus.PROCESSING, OrderStatus.PENDING -> WarningColor
    OrderStatus.CANCELLED, OrderStatus.RETURNED -> ErrorColor
}

private fun statusColor(label: String): Color = when (label) {
    OrderStatus.DELIVERED.label() -> SuccessColor
    OrderStatus.SHIPPED.label(), OrderStatus.CONFIRMED.label() -> Color(0xFF7DD3FC)
    OrderStatus.PROCESSING.label(), OrderStatus.PENDING.label() -> WarningColor
    else -> ErrorColor
}

private fun dashboardSegmentColors(): List<Color> = listOf(
    Primary500,
    Color(0xFF7DD3FC),
    WarningColor,
    Color(0xFF14B8A6),
)
