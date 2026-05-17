package uqu.drawbridge.platform.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.AddressResponseDto
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationDTO
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentMethodDTO
import uqu.drawbridge.platform.PaymentMethodType
import uqu.drawbridge.platform.SupportTicketCategory
import uqu.drawbridge.platform.SupportTicketDTO
import uqu.drawbridge.platform.SupportTicketStatus
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.components.AppCard
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.DeferredFeatureCard
import uqu.drawbridge.platform.ui.components.EmptyStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.PrimaryButton
import uqu.drawbridge.platform.ui.components.ScreenSection
import uqu.drawbridge.platform.ui.components.SecondaryButton
import uqu.drawbridge.platform.ui.components.StatCard
import uqu.drawbridge.platform.ui.components.StatusChip
import uqu.drawbridge.platform.ui.components.StatusTone
import uqu.drawbridge.platform.ui.engagement.AddressFormState
import uqu.drawbridge.platform.ui.engagement.NotificationsStateHolder
import uqu.drawbridge.platform.ui.engagement.PaymentMethodFormState
import uqu.drawbridge.platform.ui.engagement.PasswordFormState
import uqu.drawbridge.platform.ui.engagement.ProfileFormState
import uqu.drawbridge.platform.ui.engagement.ReportsStateHolder
import uqu.drawbridge.platform.ui.engagement.SettingsSection
import uqu.drawbridge.platform.ui.engagement.SettingsStateHolder
import uqu.drawbridge.platform.ui.engagement.SupportStateHolder

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
) {
    val state = supportStateHolder.state
    val scope = rememberCoroutineScope()

    LaunchedEffect(supportStateHolder) {
        supportStateHolder.load()
    }

    ScreenSection(title = "Support", subtitle = "Create tickets and track your existing requests.") {
        state.errorMessage?.let { ErrorCard(it) { scope.launch { supportStateHolder.load() } } }
        state.successMessage?.let { SuccessCard(it) }
        AppCard {
            Text("Create ticket", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AppTextField(value = state.subject, onValueChange = supportStateHolder::updateSubject, label = "Subject")
            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SupportTicketCategory.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { category ->
                            FilterChip(
                                modifier = Modifier.weight(1f),
                                selected = state.category == category,
                                onClick = { supportStateHolder.updateCategory(category) },
                                label = { Text(category.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                }
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = supportStateHolder::updateDescription,
                label = { Text("Description") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Attachments are deferred until the native picker/upload flow is ready.", style = MaterialTheme.typography.bodySmall)
            PrimaryButton(
                text = if (state.isSubmitting) "Submitting..." else "Submit ticket",
                enabled = !state.isSubmitting,
                onClick = { scope.launch { supportStateHolder.submit() } },
            )
        }
        AppCard {
            Text("My tickets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), value = "${state.tickets.size}", label = "Tickets")
                StatCard(modifier = Modifier.weight(1f), value = "${state.openCount}", label = "Open")
            }
            when {
                state.isLoading -> LoadingStateCard(title = "Loading tickets", message = "Fetching your support history.")
                state.errorMessage != null -> Text("Resolve the error above, then retry.")
                state.tickets.isEmpty() -> Text("No support tickets yet.")
                else -> state.tickets.forEach { ticket ->
                    TicketCard(ticket = ticket, selected = state.selectedTicket?.id == ticket.id) {
                        scope.launch { supportStateHolder.selectTicket(ticket.id) }
                    }
                }
            }
        }
        state.selectedTicket?.let { ticket ->
            AppCard {
                Text("Ticket ${ticket.ticketNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusChip(text = ticket.status.name.replace('_', ' '), tone = supportTone(ticket.status))
                Text(ticket.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(ticket.description, style = MaterialTheme.typography.bodyMedium)
                Text("Updated ${ticket.updatedAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ticket.attachmentUrl?.takeIf { it.isNotBlank() }?.let {
                    Text("Attachment: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun NotificationsMainScreen(
    notificationsStateHolder: NotificationsStateHolder,
) {
    val state = notificationsStateHolder.state
    val scope = rememberCoroutineScope()

    LaunchedEffect(notificationsStateHolder) {
        notificationsStateHolder.load()
    }

    ScreenSection(title = "Notifications", subtitle = "Review in-app platform updates.") {
        state.errorMessage?.let { ErrorCard(it) { scope.launch { notificationsStateHolder.load() } } }
        state.successMessage?.let { SuccessCard(it) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(modifier = Modifier.weight(1f), value = "${state.notifications.size}", label = "Total")
            StatCard(modifier = Modifier.weight(1f), value = "${state.unreadCount}", label = "Unread")
        }
        SecondaryButton(
            text = if (state.isMutating) "Updating..." else "Mark all as read",
            enabled = !state.isMutating && state.unreadCount > 0,
            onClick = { scope.launch { notificationsStateHolder.markAllRead() } },
        )
        when {
            state.isLoading -> LoadingStateCard(title = "Loading inbox", message = "Fetching notifications.")
            state.errorMessage != null -> Text("Resolve the error above, then retry.")
            state.notifications.isEmpty() -> EmptyStateCard(title = "No notifications", message = "Platform updates will appear here.")
            else -> state.notifications.forEach { notification ->
                NotificationCard(notification = notification) {
                    scope.launch { notificationsStateHolder.markAsRead(notification) }
                }
            }
        }
        DeferredFeatureCard(
            destination = uqu.drawbridge.platform.ui.model.AppDestination.Notifications,
            title = "iOS push deferred",
            message = "The backend currently supports web push subscriptions. Native APNs setup is not implemented in this phase.",
        )
    }
}

@Composable
internal fun SettingsMainScreen(
    settingsStateHolder: SettingsStateHolder,
    onLogout: () -> Unit,
) {
    val state = settingsStateHolder.state
    val scope = rememberCoroutineScope()

    LaunchedEffect(settingsStateHolder) {
        settingsStateHolder.load()
    }

    ScreenSection(title = "Settings", subtitle = "Manage profile, security, addresses, payments, and preferences.") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsSection.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { section ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = state.selectedSection == section,
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
        when (state.selectedSection) {
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
private fun NotificationCard(notification: NotificationDTO, onMarkRead: () -> Unit) {
    AppCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(notification.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            StatusChip(text = if (notification.read) "Read" else "Unread", tone = if (notification.read) StatusTone.Neutral else StatusTone.Warning)
        }
        Text(notification.message)
        Text(notification.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!notification.read) {
            SecondaryButton(text = "Mark as read", onClick = onMarkRead)
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
    AppCard {
        Text(message, color = MaterialTheme.colorScheme.error)
        SecondaryButton(text = "Retry", onClick = onRetry)
    }
}

@Composable
private fun SuccessCard(message: String) {
    AppCard {
        Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

private fun supportTone(status: SupportTicketStatus): StatusTone = when (status) {
    SupportTicketStatus.OPEN -> StatusTone.Warning
    SupportTicketStatus.IN_PROGRESS -> StatusTone.Warning
    SupportTicketStatus.CLOSED -> StatusTone.Neutral
}
