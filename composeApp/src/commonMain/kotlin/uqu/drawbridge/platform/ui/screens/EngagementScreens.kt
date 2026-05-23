package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.AddressResponseDto
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationDTO
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.NotificationType
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentMethodDTO
import uqu.drawbridge.platform.PaymentMethodType
import uqu.drawbridge.platform.SupportTicketCategory
import uqu.drawbridge.platform.SupportTicketDTO
import uqu.drawbridge.platform.SupportTicketStatus
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppPageHeader
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.DeferredFeatureCard
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.ServerErrorCard
import uqu.drawbridge.platform.ui.components.StatCard
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.common.ServerNotFoundMessage
import uqu.drawbridge.platform.ui.engagement.AddressFormState
import uqu.drawbridge.platform.ui.engagement.NotificationsStateHolder
import uqu.drawbridge.platform.ui.engagement.PaymentMethodFormState
import uqu.drawbridge.platform.ui.engagement.PasswordFormState
import uqu.drawbridge.platform.ui.engagement.ProfileFormState
import uqu.drawbridge.platform.ui.engagement.ReportsStateHolder
import uqu.drawbridge.platform.ui.engagement.SettingsSection
import uqu.drawbridge.platform.ui.engagement.SettingsStateHolder
import uqu.drawbridge.platform.ui.engagement.SupportStateHolder
import uqu.drawbridge.platform.ui.platform.FilePhotoPicker
import uqu.drawbridge.platform.ui.platform.NativeOptionPicker
import uqu.drawbridge.platform.ui.platform.PickedFile
import uqu.drawbridge.platform.ui.theme.AppMutedText
import uqu.drawbridge.platform.ui.theme.AppNavySurfaceHigh
import uqu.drawbridge.platform.ui.theme.ErrorColor
import uqu.drawbridge.platform.ui.theme.Primary500
import uqu.drawbridge.platform.ui.theme.WarningColor

private val NotificationsText = Color(0xFFF8FAFC)
private val NotificationsPanel = AppNavySurfaceHigh.copy(alpha = 0.92f)
private val NotificationsBorder = Color.White.copy(alpha = 0.12f)
private val SupportText = Color(0xFFF8FAFC)
private val SupportPanel = AppNavySurfaceHigh.copy(alpha = 0.92f)
private val SupportBorder = Color.White.copy(alpha = 0.12f)

@Composable
internal fun ReportsMainScreen(
    reportsStateHolder: ReportsStateHolder,
    role: UserRole,
) {
    val state = reportsStateHolder.state
    val scope = rememberCoroutineScope()

    LaunchedEffect(reportsStateHolder) {
        reportsStateHolder.load()
    }

    ScreenSection(
        title = "Reports",
        subtitle = "Live summaries derived from orders and ${if (role == UserRole.RETAILER) "inventory" else "catalog"} data.",
    ) {
        if (state.isLoading) {
            LoadingStateCard(title = "Loading reports", message = "Fetching current platform data.")
        }
        state.errorMessage?.let { ErrorCard(it) { scope.launch { reportsStateHolder.load() } } }
        if (!state.isLoading && state.errorMessage == null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), value = "${state.orderCount}", label = "Orders")
                StatCard(modifier = Modifier.weight(1f), value = "${state.pendingOrders}", label = "Pending")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), value = "${state.processingOrders}", label = "Processing")
                StatCard(modifier = Modifier.weight(1f), value = "${state.lowStockCount}", label = "Low stock")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), value = "SAR ${state.totalValue}", label = "Order value")
                StatCard(modifier = Modifier.weight(1f), value = "${state.outOfStockCount}", label = "Out")
            }
            if (role == UserRole.WHOLESALER) {
                AppCard {
                    Text("Catalog health", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${state.publishedProducts} published products from ${state.products.size} managed products.")
                }
            }
            DeferredFeatureCard(
                destination = uqu.drawbridge.platform.ui.model.AppDestination.Reports,
                title = "Dedicated report endpoint deferred",
                message = "The web app currently exposes reports as a placeholder. This mobile view uses real orders, inventory, and product APIs until a report endpoint exists.",
            )
            SecondaryButton(text = "Refresh reports", onClick = { scope.launch { reportsStateHolder.load() } })
        }
    }
}

@Composable
internal fun SupportMainScreen(
    supportStateHolder: SupportStateHolder,
    filePhotoPicker: FilePhotoPicker,
    optionPicker: NativeOptionPicker,
    onBack: (() -> Unit)? = null,
) {
    val state = supportStateHolder.state
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(SupportTab.Create) }

    LaunchedEffect(supportStateHolder) {
        supportStateHolder.loadIfNeeded()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SupportHeader(
            selectedTab = selectedTab,
            ticketCount = state.tickets.size,
            onBack = onBack,
            onTabSelected = { selectedTab = it },
        )
        state.errorMessage?.let { ErrorCard(it) { scope.launch { supportStateHolder.load() } } }
        state.successMessage?.let { SuccessCard(it) }

        if (selectedTab == SupportTab.Create) {
            SupportCreateTicketContent(
                state = state,
                onSubjectChange = supportStateHolder::updateSubject,
                onDescriptionChange = supportStateHolder::updateDescription,
                onPickCategory = {
                    scope.launch {
                        val categories = SupportTicketCategory.entries
                        val selectedIndex = categories.indexOf(state.category)
                        val pickedIndex = optionPicker.pickOption(
                            title = "Support category",
                            options = categories.map { it.supportLabel() },
                            selectedIndex = selectedIndex,
                        )
                        if (pickedIndex != null && pickedIndex in categories.indices) {
                            supportStateHolder.updateCategory(categories[pickedIndex])
                        }
                    }
                },
                onPickAttachment = {
                    scope.launch {
                        filePhotoPicker.pickFile()?.let(supportStateHolder::updateAttachment)
                    }
                },
                onClearAttachment = supportStateHolder::clearAttachment,
                onSubmit = {
                    scope.launch {
                        if (supportStateHolder.submit()) {
                            selectedTab = SupportTab.Tickets
                        }
                    }
                },
            )
        } else {
            SupportTicketsContent(
                state = state,
            )
        }
    }
}

private enum class SupportTab {
    Create,
    Tickets
}

@Composable
private fun SupportHeader(
    selectedTab: SupportTab,
    ticketCount: Int,
    onBack: (() -> Unit)?,
    onTabSelected: (SupportTab) -> Unit,
) {
    AppPageHeader(
        title = "Support",
        subtitle = "Create tickets and track your requests",
        leading = onBack?.let { { SupportBackSquare(onClick = it) } },
        action = {
            SupportTabSwitch(
                selectedTab = selectedTab,
                ticketCount = ticketCount,
                onTabSelected = onTabSelected,
            )
        },
    )
}

@Composable
private fun SupportTabSwitch(
    selectedTab: SupportTab,
    ticketCount: Int,
    onTabSelected: (SupportTab) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, SupportBorder),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SupportTabButton(
                modifier = Modifier.weight(1f),
                selected = selectedTab == SupportTab.Create,
                icon = Icons.Default.Add,
                label = "Create Ticket",
                badge = null,
                onClick = { onTabSelected(SupportTab.Create) },
            )
            SupportTabButton(
                modifier = Modifier.weight(1f),
                selected = selectedTab == SupportTab.Tickets,
                icon = Icons.AutoMirrored.Filled.List,
                label = "My Tickets",
                badge = ticketCount.takeIf { it > 0 }?.toString(),
                onClick = { onTabSelected(SupportTab.Tickets) },
            )
        }
    }
}

@Composable
private fun SupportTabButton(
    modifier: Modifier,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    badge: String?,
    onClick: () -> Unit,
) {
    val contentColor by animateColorAsState(if (selected) SupportText else AppMutedText)
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) SupportPanel else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, SupportBorder) else null,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            badge?.let {
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(999.dp), color = Primary500) {
                    Text(
                        it,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportCreateTicketContent(
    state: uqu.drawbridge.platform.ui.engagement.SupportUiState,
    onSubjectChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickCategory: () -> Unit,
    onPickAttachment: () -> Unit,
    onClearAttachment: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SupportNoticeCard()
        SupportSectionCard {
            SupportFieldLabel("Subject *")
            SupportTextInput(
                value = state.subject,
                onValueChange = onSubjectChange,
                placeholder = "Short summary of the issue",
                singleLine = true,
            )
        }
        SupportSectionCard {
            SupportFieldLabel("Category *")
            SupportCategorySelector(category = state.category, onClick = onPickCategory)
            Text(
                state.category.supportHint(),
                style = MaterialTheme.typography.bodySmall,
                color = AppMutedText,
                fontWeight = FontWeight.SemiBold,
            )
        }
        SupportSectionCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SupportFieldLabel("Description *")
                Text(
                    "${state.description.length} / 500",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppMutedText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            SupportTextInput(
                value = state.description,
                onValueChange = onDescriptionChange,
                placeholder = "Describe the issue in detail...",
                singleLine = false,
                minLines = 6,
                modifier = Modifier.heightIn(min = 150.dp),
            )
        }
        SupportAttachmentCard(
            attachment = state.attachment,
            onPickAttachment = onPickAttachment,
            onClearAttachment = onClearAttachment,
        )
        SupportSubmitButton(
            text = if (state.isSubmitting) "Submitting..." else "Submit Ticket",
            enabled = !state.isSubmitting,
            onClick = onSubmit,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = AppMutedText, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "We aim to respond within 24-48 business hours",
                style = MaterialTheme.typography.bodySmall,
                color = AppMutedText,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SupportTicketsContent(
    state: uqu.drawbridge.platform.ui.engagement.SupportUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "${state.tickets.size} total tickets",
            style = MaterialTheme.typography.titleSmall,
            color = AppMutedText,
            fontWeight = FontWeight.Black,
        )
        when {
            state.isLoading -> LoadingStateCard(title = "Loading tickets", message = "Fetching your support history.")
            state.errorMessage != null -> Unit
            state.tickets.isEmpty() -> SupportEmptyTickets()
            else -> state.tickets.forEach { ticket ->
                SupportTicketRow(
                    ticket = ticket,
                )
            }
        }
    }
}

@Composable
private fun SupportNoticeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Primary500.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Primary500.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Primary500, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    "Share enough detail for a faster resolution",
                    style = MaterialTheme.typography.titleSmall,
                    color = SupportText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Include what happened, when it happened, and any order or payment context you already have.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMutedText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SupportSectionCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SupportPanel,
        border = BorderStroke(1.dp, SupportBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun SupportFieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = AppMutedText,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun SupportTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = AppMutedText, fontWeight = FontWeight.SemiBold)
        },
        minLines = minLines,
        singleLine = singleLine,
        shape = RoundedCornerShape(8.dp),
        colors = supportTextFieldColors(),
    )
}

@Composable
private fun SupportCategorySelector(
    category: SupportTicketCategory,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, SupportBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                category.supportLabel(),
                style = MaterialTheme.typography.titleSmall,
                color = SupportText,
                fontWeight = FontWeight.Black,
            )
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = AppMutedText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SupportAttachmentCard(
    attachment: PickedFile?,
    onPickAttachment: () -> Unit,
    onClearAttachment: () -> Unit,
) {
    SupportSectionCard {
        SupportFieldLabel("Attachment (optional)")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 118.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onPickAttachment),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.035f),
            border = BorderStroke(1.dp, SupportBorder),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Primary500.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, tint = Primary500, modifier = Modifier.size(24.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        attachment?.name ?: "Add a screenshot, invoice, or related file",
                        style = MaterialTheme.typography.titleSmall,
                        color = SupportText,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        attachment?.let { "${formatAttachmentSize(it.bytes.size)} attached" }
                            ?: "Max upload size follows the platform upload settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppMutedText,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (attachment != null) {
                    IconButton(onClick = onClearAttachment) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove attachment", tint = ErrorColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportSubmitButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (enabled) Primary500 else Color.White.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun SupportTicketRow(
    ticket: SupportTicketDTO,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SupportPanel,
        border = BorderStroke(1.dp, SupportBorder),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    ticket.subject,
                    style = MaterialTheme.typography.titleMedium,
                    color = SupportText,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ticket.ticketNumber,
                    style = MaterialTheme.typography.labelLarge,
                    color = AppMutedText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    ticket.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMutedText,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(text = ticket.status.supportLabel(), tone = supportTone(ticket.status))
                    SupportMiniChip(ticket.category.supportLabel())
                    Text(
                        supportDate(ticket.updatedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppMutedText,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportMiniChip(text: String) {
    Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.08f)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = AppMutedText,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

@Composable
private fun SupportEmptyTickets() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = AppMutedText, modifier = Modifier.size(38.dp))
        }
        Text("No tickets yet", style = MaterialTheme.typography.titleLarge, color = SupportText, fontWeight = FontWeight.Black)
        Text(
            "Create your first support request and it will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = AppMutedText,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SupportBackSquare(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = SupportPanel,
        border = BorderStroke(1.dp, SupportBorder),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SupportText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun NotificationsMainScreen(
    notificationsStateHolder: NotificationsStateHolder,
    onBack: (() -> Unit)? = null,
) {
    val state = notificationsStateHolder.state
    val scope = rememberCoroutineScope()
    var selectedFilter by remember { mutableStateOf(NotificationInboxFilter.All) }

    LaunchedEffect(notificationsStateHolder) {
        notificationsStateHolder.loadIfNeeded()
    }

    val filters = remember(state.notifications, state.unreadCount) {
        NotificationInboxFilter.entries.map { filter ->
            filter to filter.count(state.notifications, state.unreadCount)
        }
    }
    val visibleNotifications = remember(state.notifications, selectedFilter) {
        selectedFilter.apply(state.notifications)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NotificationsHeader(
            totalCount = state.notifications.size,
            unreadCount = state.unreadCount,
            isUpdating = state.isMutating,
            onBack = onBack,
            onMarkAllRead = { scope.launch { notificationsStateHolder.markAllRead() } },
        )
        NotificationsFilterRow(
            filters = filters,
            selectedFilter = selectedFilter,
            onSelect = { selectedFilter = it },
        )
        state.errorMessage?.let { ErrorCard(it) { scope.launch { notificationsStateHolder.load() } } }
        when {
            state.isLoading -> LoadingStateCard(title = "Loading inbox", message = "Fetching notifications.")
            state.errorMessage != null -> Unit
            state.notifications.isEmpty() -> NotificationsEmptyState()
            visibleNotifications.isEmpty() -> EmptyStateCard(
                title = "No ${selectedFilter.label.lowercase()} notifications",
                message = "New updates will appear here when available.",
            )
            else -> visibleNotifications.forEach { notification ->
                NotificationCard(
                    notification = notification,
                    onMarkRead = { scope.launch { notificationsStateHolder.markAsRead(notification) } },
                    onDelete = { scope.launch { notificationsStateHolder.delete(notification) } },
                )
            }
        }
    }
}

private enum class NotificationInboxFilter(val label: String) {
    All("All"),
    Unread("Unread"),
    Orders("Orders"),
    Stock("Stock"),
    Payments("Payments"),
    System("System");

    fun apply(notifications: List<NotificationDTO>): List<NotificationDTO> = when (this) {
        All -> notifications
        Unread -> notifications.filterNot { it.read }
        Orders -> notifications.filter { it.type == NotificationType.ORDER }
        Stock -> notifications.filter { it.type == NotificationType.STOCK }
        Payments -> notifications.filter { it.type == NotificationType.PAYMENT }
        System -> notifications.filter { it.type == NotificationType.SYSTEM }
    }

    fun count(notifications: List<NotificationDTO>, unreadCount: Int): Int = when (this) {
        All -> notifications.size
        Unread -> unreadCount
        else -> apply(notifications).size
    }
}

@Composable
private fun NotificationsHeader(
    totalCount: Int,
    unreadCount: Int,
    isUpdating: Boolean,
    onBack: (() -> Unit)?,
    onMarkAllRead: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = NotificationsPanel,
        border = BorderStroke(1.dp, NotificationsBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                NotificationsBackSquare(onClick = onBack)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = NotificationsText,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = ErrorColor,
                        ) {
                            Text(
                                unreadCount.toString(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                            )
                        }
                    }
                }
                Text(
                    "$totalCount total notifications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NotificationsMarkAllButton(
                enabled = unreadCount > 0 && !isUpdating,
                loading = isUpdating,
                onClick = onMarkAllRead,
            )
        }
    }
}

@Composable
private fun NotificationsBackSquare(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = NotificationsPanel,
        border = BorderStroke(1.dp, NotificationsBorder),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = NotificationsText,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun NotificationsMarkAllButton(
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) Primary500 else AppMutedText
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = tint.copy(alpha = if (enabled) 0.14f else 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.34f else 0.16f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            Text(
                if (loading) "Updating" else "Mark all read",
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) Color(0xFFB8F7D8) else AppMutedText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun NotificationsFilterRow(
    filters: List<Pair<NotificationInboxFilter, Int>>,
    selectedFilter: NotificationInboxFilter,
    onSelect: (NotificationInboxFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { (filter, count) ->
            NotificationFilterChip(
                label = filter.label,
                count = count,
                selected = filter == selectedFilter,
                onClick = { onSelect(filter) },
            )
        }
    }
}

@Composable
private fun NotificationFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(if (selected) Primary500 else NotificationsPanel)
    val border by animateColorAsState(if (selected) Primary500 else NotificationsBorder)
    Surface(
        modifier = Modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = BorderStroke(1.dp, border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else NotificationsText,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.White.copy(alpha = if (selected) 0.18f else 0.10f),
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) Color.White else AppMutedText,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Composable
private fun NotificationsEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Primary500.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Primary500, modifier = Modifier.size(50.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "No notifications",
            style = MaterialTheme.typography.titleLarge,
            color = NotificationsText,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Order, stock, payment, and system updates will appear here.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = AppMutedText,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun notificationTypeLabel(type: NotificationType): String = when (type) {
    NotificationType.ORDER -> "Orders"
    NotificationType.STOCK -> "Stock"
    NotificationType.PAYMENT -> "Payment"
    NotificationType.SYSTEM -> "System"
}

private fun notificationTint(type: NotificationType): Color = when (type) {
    NotificationType.ORDER -> Color(0xFF60A5FA)
    NotificationType.STOCK -> WarningColor
    NotificationType.PAYMENT -> Primary500
    NotificationType.SYSTEM -> AppMutedText
}

private fun notificationIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.ORDER -> Icons.Default.ShoppingCart
    NotificationType.STOCK -> Icons.Default.Inventory2
    NotificationType.PAYMENT -> Icons.Default.CreditCard
    NotificationType.SYSTEM -> Icons.Default.Notifications
}

@Composable
internal fun SettingsMainScreen(
    settingsStateHolder: SettingsStateHolder,
    onLogout: () -> Unit,
) {
    val state = settingsStateHolder.state
    val scope = rememberCoroutineScope()
    val availableSections = remember(state.user.role) {
        SettingsSection.entries.filter { section ->
            state.user.role == UserRole.RETAILER ||
                section !in listOf(SettingsSection.Addresses, SettingsSection.Payments)
        }
    }
    val selectedSection = if (state.selectedSection in availableSections) {
        state.selectedSection
    } else {
        SettingsSection.Profile
    }

    LaunchedEffect(settingsStateHolder) {
        settingsStateHolder.load()
    }

    ScreenSection(
        title = "Settings",
        subtitle = if (state.user.role == UserRole.RETAILER) {
            "Manage profile, security, addresses, payments, and preferences."
        } else {
            "Manage profile, security, and notification preferences."
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            availableSections.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { section ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedSection == section,
                            onClick = { settingsStateHolder.selectSection(section) },
                            label = { Text(section.name) },
                        )
                    }
                    if (row.size == 1) {
                        Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
        state.errorMessage?.let { ErrorCard(it) { scope.launch { settingsStateHolder.load() } } }
        state.successMessage?.let { SuccessCard(it) }
        if (state.isLoading) {
            LoadingStateCard(title = "Loading settings", message = "Fetching account details.")
            return@ScreenSection
        }
        if (state.errorMessage != null && state.profileForm == ProfileFormState()) {
            SecondaryButton(text = "Retry settings", onClick = { scope.launch { settingsStateHolder.load() } })
            return@ScreenSection
        }
        when (selectedSection) {
            SettingsSection.Profile -> ProfileSettingsSection(settingsStateHolder)
            SettingsSection.Security -> SecuritySettingsSection(settingsStateHolder)
            SettingsSection.Addresses -> AddressesSettingsSection(settingsStateHolder)
            SettingsSection.Payments -> PaymentsSettingsSection(settingsStateHolder)
            SettingsSection.Notifications -> NotificationPreferencesSection(settingsStateHolder)
        }
        AppCard {
            Text("Session", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SecondaryButton(text = "Sign out", onClick = onLogout)
        }
    }
}

@Composable
private fun ProfileSettingsSection(holder: SettingsStateHolder) {
    val state = holder.state
    val form = state.profileForm
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AppTextField(form.company, { holder.updateProfile(form.copy(company = it)) }, "Business name")
        AppTextField(form.phone, { holder.updateProfile(form.copy(phone = it)) }, "Business phone", keyboardType = KeyboardType.Phone)
        AppTextField(form.commercialRegister, { holder.updateProfile(form.copy(commercialRegister = it)) }, "Commercial registration")
        AppTextField(form.representativeName, { holder.updateProfile(form.copy(representativeName = it)) }, "Representative name")
        AppTextField(form.representativeTitle, { holder.updateProfile(form.copy(representativeTitle = it)) }, "Representative title")
        AppTextField(form.representativeEmail, { holder.updateProfile(form.copy(representativeEmail = it)) }, "Representative email", keyboardType = KeyboardType.Email)
        AppTextField(form.representativePhone, { holder.updateProfile(form.copy(representativePhone = it)) }, "Representative phone", keyboardType = KeyboardType.Phone)
        PrimaryButton(
            text = if (state.isSaving) "Saving..." else "Save profile",
            enabled = !state.isSaving,
            onClick = { scope.launch { holder.saveProfile() } },
        )
    }
}

@Composable
private fun SecuritySettingsSection(holder: SettingsStateHolder) {
    val state = holder.state
    val form = state.passwordForm
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        PasswordField("Current password", form.currentPassword) { holder.updatePassword(form.copy(currentPassword = it)) }
        PasswordField("New password", form.newPassword) { holder.updatePassword(form.copy(newPassword = it)) }
        PasswordField("Confirm new password", form.confirmPassword) { holder.updatePassword(form.copy(confirmPassword = it)) }
        PrimaryButton(
            text = if (state.isSaving) "Updating..." else "Update password",
            enabled = !state.isSaving,
            onClick = { scope.launch { holder.changePassword() } },
        )
        DeferredFeatureCard(
            destination = uqu.drawbridge.platform.ui.model.AppDestination.Settings,
            title = "Two-factor setup deferred",
            message = "The web UI displays local 2FA controls, but there is no backend 2FA contract to enable here yet.",
        )
    }
}

@Composable
private fun AddressesSettingsSection(holder: SettingsStateHolder) {
    val state = holder.state
    val form = state.addressForm
    val scope = rememberCoroutineScope()
    AppCard {
        Text(if (state.editingAddressId == null) "Add address" else "Edit address", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AddressFields(form = form, onChange = holder::updateAddressForm)
        PrimaryButton(
            text = if (state.isSaving) "Saving..." else "Save address",
            enabled = !state.isSaving,
            onClick = { scope.launch { holder.saveAddress() } },
        )
        SecondaryButton(text = "New blank address", onClick = { holder.startAddAddress() })
    }
    if (state.addresses.isEmpty()) {
        EmptyStateCard(title = "No addresses", message = "Saved addresses will appear here.")
    } else {
        state.addresses.forEach { address ->
            AddressCard(
                address = address,
                isConfirmingDelete = state.pendingDeleteAddressId == address.id,
                onEdit = { holder.startEditAddress(address) },
                onDelete = { holder.requestDeleteAddress(address.id) },
                onCancelDelete = { holder.requestDeleteAddress(null) },
                onConfirmDelete = { scope.launch { holder.deleteAddress(address.id) } },
            )
        }
    }
}

@Composable
private fun PaymentsSettingsSection(holder: SettingsStateHolder) {
    val state = holder.state
    val form = state.paymentForm
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Add payment method", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Store masked payment details only. Real payment processing is not performed here.", style = MaterialTheme.typography.bodySmall)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethodType.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { type ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = form.type == type,
                            onClick = { holder.updatePaymentForm(form.copy(type = type)) },
                            label = { Text(type.name.replace('_', ' ')) },
                        )
                    }
                    if (row.size == 1) {
                        Column(modifier = Modifier.weight(1f)) {}
                    }
                }
            }
        }
        AppTextField(form.maskedDetails, { holder.updatePaymentForm(form.copy(maskedDetails = it)) }, "Masked details")
        PrimaryButton(
            text = if (state.isSaving) "Saving..." else "Add payment method",
            enabled = !state.isSaving,
            onClick = { scope.launch { holder.addPaymentMethod() } },
        )
    }
    if (state.paymentMethods.isEmpty()) {
        EmptyStateCard(title = "No payment methods", message = "Payment methods saved through the backend will appear here.")
    } else {
        state.paymentMethods.forEach { method ->
            PaymentMethodCard(
                method = method,
                isConfirmingDelete = state.pendingDeletePaymentMethodId == method.id,
                onDefault = { scope.launch { holder.setDefaultPaymentMethod(method.id) } },
                onDelete = { holder.requestDeletePaymentMethod(method.id) },
                onCancelDelete = { holder.requestDeletePaymentMethod(null) },
                onConfirmDelete = { scope.launch { holder.deletePaymentMethod(method.id) } },
            )
        }
    }
}

@Composable
private fun NotificationPreferencesSection(holder: SettingsStateHolder) {
    val state = holder.state
    val scope = rememberCoroutineScope()
    AppCard {
        Text("Notification preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        NotificationPreferenceKey.entries.forEach { key ->
            Text(key.name.replace('_', ' '), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(NotificationChannel.EMAIL, NotificationChannel.SMS, NotificationChannel.PUSH).forEach { channel ->
                    val enabled = state.preferences.firstOrNull { it.preferenceKey == key && it.channel == channel }?.enabled
                        ?: (channel != NotificationChannel.SMS)
                    FilterChip(
                        selected = enabled,
                        onClick = { scope.launch { holder.togglePreference(key, channel) } },
                        label = { Text(channel.name) },
                    )
                }
            }
        }
        Text("Native APNs push is deferred; PUSH here controls existing backend web-push preferences.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TicketCard(ticket: SupportTicketDTO, selected: Boolean, onClick: () -> Unit) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ticket.ticketNumber, fontWeight = FontWeight.Bold)
            StatusChip(text = ticket.status.name.replace('_', ' '), tone = supportTone(ticket.status))
        }
        Text(ticket.subject, style = MaterialTheme.typography.bodyMedium)
        SecondaryButton(text = if (selected) "Selected" else "View details", enabled = !selected, onClick = onClick)
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationDTO,
    onMarkRead: () -> Unit,
    onDelete: () -> Unit,
) {
    val tint = notificationTint(notification.type)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = !notification.read, onClick = onMarkRead),
        shape = RoundedCornerShape(8.dp),
        color = if (notification.read) NotificationsPanel else tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, if (notification.read) NotificationsBorder else tint.copy(alpha = 0.32f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(notificationIcon(notification.type), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        notification.title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = NotificationsText,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!notification.read) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(tint),
                        )
                    }
                }
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMutedText,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tint.copy(alpha = 0.14f),
                    ) {
                        Text(
                            notificationTypeLabel(notification.type),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = tint,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    Text(
                        notification.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete notification", tint = AppMutedText.copy(alpha = 0.72f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddressFields(form: AddressFormState, onChange: (AddressFormState) -> Unit) {
    AppTextField(form.street, { onChange(form.copy(street = it)) }, "Street")
    AppTextField(form.city, { onChange(form.copy(city = it)) }, "City")
    AppTextField(form.state, { onChange(form.copy(state = it)) }, "State")
    AppTextField(form.zipCode, { onChange(form.copy(zipCode = it)) }, "Zip code")
    AppTextField(form.country, { onChange(form.copy(country = it)) }, "Country")
}

@Composable
private fun AddressCard(
    address: AddressResponseDto,
    isConfirmingDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    AppCard {
        Text(address.street, fontWeight = FontWeight.Bold)
        Text("${address.city}, ${address.state} ${address.zipCode}")
        Text(address.country)
        if (isConfirmingDelete) {
            Text("Delete this address?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "Cancel", modifier = Modifier.weight(1f), onClick = onCancelDelete)
                SecondaryButton(text = "Confirm", modifier = Modifier.weight(1f), onClick = onConfirmDelete)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "Edit", modifier = Modifier.weight(1f), onClick = onEdit)
                SecondaryButton(text = "Delete", modifier = Modifier.weight(1f), onClick = onDelete)
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: PaymentMethodDTO,
    isConfirmingDelete: Boolean,
    onDefault: () -> Unit,
    onDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(method.type.name.replace('_', ' '), fontWeight = FontWeight.Bold)
            if (method.isDefault) StatusChip(text = "Default", tone = StatusTone.Success)
        }
        Text(method.maskedDetails)
        if (isConfirmingDelete) {
            Text("Delete this payment method?", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "Cancel", modifier = Modifier.weight(1f), onClick = onCancelDelete)
                SecondaryButton(text = "Confirm", modifier = Modifier.weight(1f), onClick = onConfirmDelete)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(text = "Make default", modifier = Modifier.weight(1f), enabled = !method.isDefault, onClick = onDefault)
                SecondaryButton(text = "Delete", modifier = Modifier.weight(1f), onClick = onDelete)
            }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    val isServerNotFound = message == ServerNotFoundMessage
    if (isServerNotFound) {
        ServerErrorCard(actionText = "Retry", onAction = onRetry)
    } else {
        AppCard {
            StatusChip(text = "Needs attention", tone = StatusTone.Error)
            Text(message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            SecondaryButton(text = "Retry", onClick = onRetry)
        }
    }
}

@Composable
private fun SuccessCard(message: String) {
    AppCard {
        Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun supportTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.035f),
    disabledContainerColor = Color.White.copy(alpha = 0.03f),
    focusedIndicatorColor = Primary500,
    unfocusedIndicatorColor = SupportBorder,
    focusedLabelColor = Primary500,
    unfocusedLabelColor = AppMutedText,
    cursorColor = Primary500,
    focusedTextColor = SupportText,
    unfocusedTextColor = SupportText,
)

private fun SupportTicketCategory.supportLabel(): String = when (this) {
    SupportTicketCategory.ORDER -> "Order"
    SupportTicketCategory.POS -> "POS"
    SupportTicketCategory.PAYMENT -> "Payment"
    SupportTicketCategory.OTHER -> "Other"
}

private fun SupportTicketCategory.supportHint(): String = when (this) {
    SupportTicketCategory.ORDER -> "Questions about order status, delivery, or order issues."
    SupportTicketCategory.POS -> "Issues related to POS flows, terminals, or store checkout."
    SupportTicketCategory.PAYMENT -> "Payment failures, settlement issues, or payment methods."
    SupportTicketCategory.OTHER -> "Anything else that needs the support team."
}

private fun SupportTicketStatus.supportLabel(): String = when (this) {
    SupportTicketStatus.OPEN -> "Delivered"
    SupportTicketStatus.IN_PROGRESS -> "In Progress"
    SupportTicketStatus.CLOSED -> "Resolved"
}

private fun supportDate(value: String): String = value.take(10).ifBlank { "Date unavailable" }

private fun formatAttachmentSize(bytes: Int): String = when {
    bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

private fun supportTone(status: SupportTicketStatus): StatusTone = when (status) {
    SupportTicketStatus.OPEN -> StatusTone.Success
    SupportTicketStatus.IN_PROGRESS -> StatusTone.Warning
    SupportTicketStatus.CLOSED -> StatusTone.Neutral
}
