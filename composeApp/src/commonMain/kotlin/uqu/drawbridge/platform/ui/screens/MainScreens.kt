package uqu.drawbridge.platform.ui.screens

import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.MobileApiConfig
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppPageHeader
import uqu.drawbridge.platform.ui.components.DeferredFeatureCard
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.ServerErrorCard
import uqu.drawbridge.platform.ui.common.ServerNotFoundMessage
import uqu.drawbridge.platform.ui.engagement.ProfileFormState
import uqu.drawbridge.platform.ui.engagement.SettingsSection
import uqu.drawbridge.platform.ui.engagement.SettingsStateHolder
import uqu.drawbridge.platform.ui.model.AppDestination
import uqu.drawbridge.platform.ui.model.MoreDestination
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.platform.FilePhotoPicker
import uqu.drawbridge.platform.ui.theme.AppMutedText
import uqu.drawbridge.platform.ui.theme.AppNavySurfaceHigh
import uqu.drawbridge.platform.ui.theme.ErrorColor
import uqu.drawbridge.platform.ui.theme.Primary500

private val AccountText = Color(0xFFF8FAFC)
private val AccountPanel = AppNavySurfaceHigh.copy(alpha = 0.92f)
private val AccountBorder = Color.White.copy(alpha = 0.12f)

@Composable
internal fun HomeMainScreen(
    session: SessionState,
    onOpenDashboard: () -> Unit,
    onLogout: () -> Unit,
) {
    ScreenSection(
        title = "Hello, ${session.user.name.ifBlank { session.user.email }}",
        subtitle = "Your operational workspace is ready.",
    ) {
        AppCard {
            Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(session.user.role.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            
            Text("Company", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(session.user.company.ifBlank { "Not provided" }, style = MaterialTheme.typography.titleMedium)
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                PrimaryButton(text = "Open Dashboard", onClick = onOpenDashboard)
                SecondaryButton(text = "Logout", onClick = onLogout)
            }
        }
    }
}

@Composable
internal fun AccountMainScreen(
    settingsStateHolder: SettingsStateHolder,
    filePhotoPicker: FilePhotoPicker,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onOpenSettingsSection: (SettingsSection) -> Unit,
) {
    val state = settingsStateHolder.state
    val form = state.profileForm
    val scope = rememberCoroutineScope()
    val isRetailer = state.user.role == UserRole.RETAILER

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccountHeaderCard(
            loading = state.isSaving,
            saveEnabled = !state.isSaving,
            onBack = onBack,
            onSave = { scope.launch { settingsStateHolder.saveProfile() } },
        )
        state.errorMessage?.let {
            AccountErrorCard(message = it, onRetry = { scope.launch { settingsStateHolder.load() } })
        }
        state.successMessage?.let {
            AppCard {
                Text(it, color = Primary500, fontWeight = FontWeight.Black)
            }
        }
        AccountIdentityCard(
            form = form,
            role = state.user.role,
            avatarUrl = state.user.avatar,
            isUploadingAvatar = state.isUploadingAvatar,
            onPickAvatar = {
                scope.launch {
                    filePhotoPicker.pickPhoto()?.let { settingsStateHolder.uploadProfileImage(it) }
                }
            },
        )
        AccountStatsRow(
            isRetailer = isRetailer,
            addressCount = state.addresses.size,
            paymentCount = state.paymentMethods.size,
            alertCount = state.preferences.count { it.enabled },
            verification = state.user.verificationStatus?.name?.replace('_', ' ') ?: "Pending",
        )
        AccountSectionCard(
            icon = Icons.Default.Person,
            title = "Representative Information",
        ) {
            AccountFieldPair {
                AccountTextField(
                    label = "Representative Name",
                    value = form.representativeName,
                    modifier = Modifier.weight(1f),
                    onValueChange = { settingsStateHolder.updateProfile(form.copy(representativeName = it)) },
                )
                AccountTextField(
                    label = "Job Title",
                    value = form.representativeTitle,
                    modifier = Modifier.weight(1f),
                    onValueChange = { settingsStateHolder.updateProfile(form.copy(representativeTitle = it)) },
                )
            }
            AccountFieldPair {
                AccountTextField(
                    label = "Work Email (Personal)",
                    value = form.representativeEmail,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Email,
                    onValueChange = { settingsStateHolder.updateProfile(form.copy(representativeEmail = it)) },
                )
                AccountTextField(
                    label = "Mobile Number",
                    value = form.representativePhone,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Phone,
                    onValueChange = { settingsStateHolder.updateProfile(form.copy(representativePhone = it)) },
                )
            }
        }
        AccountSectionCard(
            icon = Icons.Default.Business,
            title = "Organization Details",
        ) {
            AccountTextField(
                label = "Business / Company Name",
                value = form.company,
                onValueChange = { settingsStateHolder.updateProfile(form.copy(company = it)) },
            )
            AccountFieldPair {
                AccountTextField(
                    label = "Account Email (Login)",
                    value = state.user.email,
                    modifier = Modifier.weight(1f),
                    onValueChange = {},
                    enabled = false,
                    helperText = "Login email cannot be changed",
                    keyboardType = KeyboardType.Email,
                )
                AccountTextField(
                    label = "Business Phone",
                    value = form.phone,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Phone,
                    onValueChange = { settingsStateHolder.updateProfile(form.copy(phone = it)) },
                )
            }
            AccountTextField(
                label = "CR Number",
                value = form.commercialRegister,
                onValueChange = { settingsStateHolder.updateProfile(form.copy(commercialRegister = it)) },
            )
        }
        AccountSignOutButton(onLogout = onLogout)
        Text(
            "Drawbridge Platform · v1.0.0",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = AppMutedText,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AccountHeaderCard(
    loading: Boolean,
    saveEnabled: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = AccountPanel,
        border = BorderStroke(1.dp, AccountBorder),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AccountBackSquare(onClick = onBack)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    "Account Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = AccountText,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "Manage profile and organization details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppMutedText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AccountSaveButton(
                loading = loading,
                enabled = saveEnabled,
                onClick = onSave,
            )
        }
    }
}

@Composable
private fun AccountBackSquare(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(50.dp),
        shape = RoundedCornerShape(12.dp),
        color = AccountPanel,
        border = BorderStroke(1.dp, AccountBorder),
    ) {
        IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AccountText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AccountSaveButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(50.dp)
            .width(104.dp)
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
            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                if (loading) "Saving" else "Save",
                color = Color.White,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountIdentityCard(
    form: ProfileFormState,
    role: UserRole,
    avatarUrl: String?,
    isUploadingAvatar: Boolean,
    onPickAvatar: () -> Unit,
) {
    val resolvedAvatarUrl = remember(avatarUrl) {
        avatarUrl
            ?.takeIf { it.isNotBlank() }
            ?.let(MobileApiConfig::resolveResourceUrl)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = AccountPanel,
        border = BorderStroke(1.dp, AccountBorder),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(104.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary500.copy(alpha = 0.95f))
                        .clickable(enabled = !isUploadingAvatar, onClick = onPickAvatar),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        accountInitials(form.company.ifBlank { form.representativeName }),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                    )
                    if (resolvedAvatarUrl != null) {
                        AsyncImage(
                            model = resolvedAvatarUrl,
                            contentDescription = form.company.ifBlank { "Account image" },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(34.dp)
                        .clickable(enabled = !isUploadingAvatar, onClick = onPickAvatar),
                    shape = RoundedCornerShape(10.dp),
                    color = AccountPanel,
                    border = BorderStroke(1.dp, AccountBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Change account image",
                            tint = if (isUploadingAvatar) AppMutedText else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    form.company.ifBlank { "Organization name" },
                    style = MaterialTheme.typography.titleLarge,
                    color = AccountText,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${role.accountRoleLabel()} Account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMutedText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Rep: ${form.representativeName.ifBlank { "Not assigned" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AccountStatsRow(
    isRetailer: Boolean,
    addressCount: Int,
    paymentCount: Int,
    alertCount: Int,
    verification: String,
) {
    val cards = if (isRetailer) {
        listOf(
            AccountStat("Addresses", addressCount.toString(), Icons.Default.Home, Color(0xFF60A5FA)),
            AccountStat("Payments", paymentCount.toString(), Icons.Default.CreditCard, Primary500),
            AccountStat("Alerts", alertCount.toString(), Icons.Default.Notifications, Color(0xFFFFB020)),
        )
    } else {
        listOf(
            AccountStat("Role", "B2B", Icons.Default.Business, Color(0xFF60A5FA)),
            AccountStat("Alerts", alertCount.toString(), Icons.Default.Notifications, Color(0xFFFFB020)),
            AccountStat("Status", verification.lowercase().replaceFirstChar { it.uppercase() }, Icons.Default.CheckCircle, Primary500),
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.forEach { card ->
            AccountStatCard(card = card, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccountStatCard(
    card: AccountStat,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.height(118.dp),
        shape = RoundedCornerShape(8.dp),
        color = AccountPanel,
        border = BorderStroke(1.dp, AccountBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(card.icon, contentDescription = null, tint = card.tint, modifier = Modifier.size(24.dp))
            Text(card.value, style = MaterialTheme.typography.titleLarge, color = AccountText, fontWeight = FontWeight.Black, maxLines = 1)
            Text(card.label, style = MaterialTheme.typography.labelMedium, color = AppMutedText, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun AccountSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = AccountPanel,
        border = BorderStroke(1.dp, AccountBorder),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary500.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Primary500, modifier = Modifier.size(22.dp))
                }
                Text(title, style = MaterialTheme.typography.titleMedium, color = AccountText, fontWeight = FontWeight.Black)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(AccountBorder))
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AccountFieldPair(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun AccountTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    helperText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AppMutedText, fontWeight = FontWeight.Black)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = accountTextFieldColors(),
        )
        helperText?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = AppMutedText, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun AccountManagementCard(
    isRetailer: Boolean,
    onOpenSettingsSection: (SettingsSection) -> Unit,
) {
    AccountSectionCard(icon = Icons.Default.Lock, title = "Account Management") {
        AccountManagementRow(
            title = "Login & Security",
            subtitle = "Password and 2FA",
            icon = Icons.Default.Lock,
            onClick = { onOpenSettingsSection(SettingsSection.Security) },
        )
        if (isRetailer) {
            AccountManagementRow(
                title = "Payment Methods",
                subtitle = "Manage saved cards",
                icon = Icons.Default.CreditCard,
                onClick = { onOpenSettingsSection(SettingsSection.Payments) },
            )
            AccountManagementRow(
                title = "Address Management",
                subtitle = "Shipping and billing addresses",
                icon = Icons.Default.Home,
                onClick = { onOpenSettingsSection(SettingsSection.Addresses) },
            )
        }
        AccountManagementRow(
            title = "Notification Preferences",
            subtitle = "Email, SMS, push alerts",
            icon = Icons.Default.Notifications,
            onClick = { onOpenSettingsSection(SettingsSection.Notifications) },
        )
    }
}

@Composable
private fun AccountManagementRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, AccountBorder),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = AppMutedText, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AccountText, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppMutedText, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppMutedText, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun AccountSignOutButton(onLogout: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onLogout),
        shape = RoundedCornerShape(8.dp),
        color = ErrorColor.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, ErrorColor.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ErrorColor, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Sign Out", color = ErrorColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AccountErrorCard(message: String, onRetry: () -> Unit) {
    if (message == ServerNotFoundMessage) {
        ServerErrorCard(actionText = "Try again", onAction = onRetry)
    } else {
        AppCard {
            Text("Could not load account", color = AccountText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Text(message, color = AppMutedText, style = MaterialTheme.typography.bodyMedium)
            SecondaryButton(text = "Try again", onClick = onRetry)
        }
    }
}

@Composable
private fun accountTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White.copy(alpha = 0.05f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.035f),
    disabledContainerColor = Color.White.copy(alpha = 0.055f),
    focusedIndicatorColor = Primary500,
    unfocusedIndicatorColor = AccountBorder,
    disabledIndicatorColor = AccountBorder,
    cursorColor = Primary500,
    focusedTextColor = AccountText,
    unfocusedTextColor = AccountText,
    disabledTextColor = AppMutedText,
)

private data class AccountStat(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val tint: Color,
)

private fun UserRole.accountRoleLabel(): String = when (this) {
    UserRole.RETAILER -> "Retailer"
    UserRole.WHOLESALER -> "Wholesaler"
}

private fun accountInitials(value: String): String {
    val parts = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "DB"
    }
}

@Composable
internal fun FeaturePlaceholderMainScreen(
    destination: AppDestination,
) {
    val (title, subtitle, body) = when (destination) {
        AppDestination.Marketplace -> Triple(
            "Marketplace",
            "Product discovery, filters, and wishlist actions.",
            "Phase 3 will connect the shared product DTOs and API client to native marketplace cards, search, filters, pagination, product detail, and wishlist actions.",
        )
        AppDestination.Cart -> Triple(
            "Cart",
            "MOQ-aware cart and checkout foundation.",
            "Phase 4 will add cart lines, quantity validation, checkout, order creation, and success or failure states with the existing shared cart and order DTOs.",
        )
        AppDestination.Orders -> Triple(
            "Orders",
            "Mobile order tracking for the active role.",
            "Phase 4 will add order lists, detail screens, status cards, tracking metadata, and role-appropriate actions.",
        )
        AppDestination.Inventory -> Triple(
            "Inventory",
            "Stock health and reorder controls.",
            "Phase 5 will add retailer inventory cards, low-stock indicators, detail views, quantity updates, and auto-restock controls.",
        )
        AppDestination.Products -> Triple(
            "Products",
            "Wholesaler catalog management.",
            "Phase 5 will add product list, create/edit forms, validation, publish state, and image upload when the platform picker and backend upload flow are connected.",
        )
        else -> Triple(
            destination.name,
            "Native workflow foundation.",
            "This destination is reserved in the role-aware mobile navigation and will be implemented in a later feature phase.",
        )
    }

    ScreenSection(title = title, subtitle = subtitle) {
        DeferredFeatureCard(destination = destination, title = "$title is mapped", message = body)
        EmptyStateCard(
            title = "No local data loaded yet",
            message = "The foundation is in place without duplicating web-only contracts or iOS-only business logic.",
        )
    }
}

@Composable
internal fun MoreMainScreen(
    destinations: List<MoreDestination>,
    onOpenDestination: (AppDestination) -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        MoreHeaderCard()
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            destinations.chunked(2).forEach { rowDestinations ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowDestinations.forEach { destination ->
                        MoreMenuCard(
                            destination = destination,
                            onClick = { onOpenDestination(destination.destination) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowDestinations.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            MoreLogoutButton(onClick = onLogout)
        }
    }
}

@Composable
private fun MoreHeaderCard() {
    AppPageHeader(
        title = "More",
        subtitle = "Account, tools, and support",
    )
}

@Composable
private fun MoreMenuCard(
    destination: MoreDestination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = moreDestinationTint(destination.destination)
    Card(
        onClick = onClick,
        modifier = modifier.height(154.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppNavySurfaceHigh.copy(alpha = 0.92f)),
        border = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.12f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(tint.copy(alpha = 0.13f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(destination.icon, contentDescription = null, tint = tint)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                        )
                        Text(
                            text = destination.description,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreLogoutButton(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = "Logout",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

private fun moreDestinationTint(destination: AppDestination): Color {
    return when (destination) {
        AppDestination.Account -> Color(0xFF10B981)
        AppDestination.Settings -> Color(0xFF93C5FD)
        AppDestination.Wishlist -> Color(0xFFFF5A8A)
        AppDestination.Notifications -> Color(0xFFFFB020)
        AppDestination.POS -> Color(0xFF60A5FA)
        AppDestination.Reports -> Color(0xFF7DD3FC)
        AppDestination.Support -> Color(0xFFC084FC)
        AppDestination.Inventory -> Color(0xFF34D399)
        else -> Color(0xFFA8B7C7)
    }
}

@Composable
internal fun MoreDestinationScreen(
    destination: AppDestination,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    accountContent: @Composable () -> Unit,
    wishlistContent: @Composable () -> Unit,
    posContent: @Composable () -> Unit,
    supportContent: @Composable () -> Unit,
    notificationsContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    inventoryContent: @Composable () -> Unit,
) {
    when (destination) {
        AppDestination.Inventory -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                inventoryContent()
            }
        }
        AppDestination.Wishlist -> {
            wishlistContent()
        }
        AppDestination.POS -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                posContent()
            }
        }
        AppDestination.Account -> {
            accountContent()
        }
        AppDestination.Support -> {
            supportContent()
        }
        AppDestination.Notifications -> {
            notificationsContent()
        }
        AppDestination.Settings -> {
            settingsContent()
        }
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton(text = "Back to More", onClick = onBack)
                FeaturePlaceholderMainScreen(destination = destination)
            }
        }
    }
}
