package uqu.drawbridge.platform.ui.engagement

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uqu.drawbridge.platform.AddressResponseDto
import uqu.drawbridge.platform.ChangePasswordRequest
import uqu.drawbridge.platform.CreateAddressRequest
import uqu.drawbridge.platform.CreatePaymentMethodRequest
import uqu.drawbridge.platform.CreateTicketRequest
import uqu.drawbridge.platform.InventoryItemDTO
import uqu.drawbridge.platform.MobileAuthApi
import uqu.drawbridge.platform.NotificationChannel
import uqu.drawbridge.platform.NotificationDTO
import uqu.drawbridge.platform.NotificationPreferenceDTO
import uqu.drawbridge.platform.NotificationPreferenceKey
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.PaymentMethodDTO
import uqu.drawbridge.platform.PaymentMethodType
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.SupportTicketCategory
import uqu.drawbridge.platform.SupportTicketDTO
import uqu.drawbridge.platform.SupportTicketStatus
import uqu.drawbridge.platform.UpdateUserProfileRequest
import uqu.drawbridge.platform.UserDTO
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.UpsertNotificationPreferenceRequest
import uqu.drawbridge.platform.ui.common.userReadableMessage
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.platform.PickedFile

internal data class ReportsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val orders: List<OrderDTO> = emptyList(),
    val inventoryItems: List<InventoryItemDTO> = emptyList(),
    val products: List<ProductDTO> = emptyList(),
) {
    val orderCount: Int get() = orders.size
    val pendingOrders: Int get() = orders.count { it.status == OrderStatus.PENDING }
    val processingOrders: Int get() = orders.count { it.status == OrderStatus.PROCESSING }
    val totalValue: Double get() = orders.sumOf { it.subtotal }
    val lowStockCount: Int
        get() = inventoryItems.count { it.currentStock <= (it.autoOrderConfig?.minThreshold ?: it.minimumOrderQuantity.coerceAtLeast(1)) } +
            products.count { it.stock in 1..9 || it.stock < it.minimumOrderQuantity.coerceAtLeast(1) }
    val outOfStockCount: Int
        get() = inventoryItems.count { it.currentStock <= 0 } + products.count { it.stock <= 0 }
    val publishedProducts: Int get() = products.count { it.published }
}

internal class ReportsStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: ReportsUiState by mutableStateOf(ReportsUiState())
        private set

    suspend fun load() {
        state = state.copy(isLoading = true, errorMessage = null)
        runCatching {
            val orders = when (session.user.role) {
                UserRole.RETAILER -> api.fetchOrdersByRetailer(session.user.id)
                UserRole.WHOLESALER -> api.fetchOrdersByWholesaler(session.user.id)
            }
            val inventory = if (session.user.role == UserRole.RETAILER) {
                api.fetchInventoryByRetailer(session.user.id)
            } else {
                emptyList()
            }
            val products = if (session.user.role == UserRole.WHOLESALER) {
                api.fetchProductsByWholesaler(session.user.id)
            } else {
                emptyList()
            }
            state = ReportsUiState(isLoading = false, orders = orders, inventoryItems = inventory, products = products)
        }.onFailure { error ->
            state = state.copy(
                isLoading = false,
                errorMessage = userReadableMessage(error, "Unable to load reports right now."),
            )
        }
    }
}

internal data class SupportUiState(
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val isSubmitting: Boolean = false,
    val tickets: List<SupportTicketDTO> = emptyList(),
    val selectedTicket: SupportTicketDTO? = null,
    val subject: String = "",
    val category: SupportTicketCategory = SupportTicketCategory.ORDER,
    val description: String = "",
    val attachment: PickedFile? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val openCount: Int get() = tickets.count { it.status != SupportTicketStatus.CLOSED }
}

internal class SupportStateHolder(
    private val api: MobileAuthApi,
) {
    var state: SupportUiState by mutableStateOf(SupportUiState())
        private set

    suspend fun loadIfNeeded() {
        if (!state.hasLoaded) {
            load()
        }
    }

    suspend fun load() {
        if (state.isLoading) return
        state = state.copy(isLoading = true, errorMessage = null)
        runCatching { api.fetchSupportTickets() }.fold(
            onSuccess = { tickets ->
                state = state.copy(
                    isLoading = false,
                    hasLoaded = true,
                    tickets = tickets,
                    selectedTicket = tickets.firstOrNull(),
                )
            },
            onFailure = { error ->
                state = state.copy(
                    isLoading = false,
                    hasLoaded = true,
                    tickets = emptyList(),
                    selectedTicket = null,
                    errorMessage = userReadableMessage(error, "Unable to load support tickets."),
                )
            },
        )
    }

    suspend fun selectTicket(ticketId: String) {
        state = state.copy(errorMessage = null)
        runCatching { api.fetchSupportTicket(ticketId) }.fold(
            onSuccess = { state = state.copy(selectedTicket = it) },
            onFailure = { error -> state = state.copy(errorMessage = userReadableMessage(error, "Unable to load ticket details.")) },
        )
    }

    fun updateSubject(value: String) {
        state = state.copy(subject = value, errorMessage = null, successMessage = null)
    }

    fun updateCategory(value: SupportTicketCategory) {
        state = state.copy(category = value, errorMessage = null, successMessage = null)
    }

    fun updateDescription(value: String) {
        state = state.copy(description = value.take(500), errorMessage = null, successMessage = null)
    }

    fun updateAttachment(value: PickedFile?) {
        val validationMessage = when {
            value == null -> null
            value.bytes.isEmpty() -> "Selected attachment is empty."
            value.bytes.size > MaxSupportAttachmentBytes -> "Attachment must be 10MB or smaller."
            else -> null
        }
        state = if (validationMessage != null) {
            state.copy(errorMessage = validationMessage, successMessage = null)
        } else {
            state.copy(attachment = value, errorMessage = null, successMessage = null)
        }
    }

    fun clearAttachment() {
        state = state.copy(attachment = null, errorMessage = null, successMessage = null)
    }

    suspend fun submit(): Boolean {
        val subject = state.subject.trim()
        val description = state.description.trim()
        if (subject.isBlank() || description.isBlank()) {
            state = state.copy(errorMessage = "Subject and description are required.")
            return false
        }

        state = state.copy(isSubmitting = true, errorMessage = null, successMessage = null)
        return runCatching {
            api.createSupportTicket(
                CreateTicketRequest(
                    subject = subject,
                    category = state.category,
                    description = description,
                ),
                attachmentFileName = state.attachment?.name,
                attachmentMimeType = state.attachment?.mimeType,
                attachmentBytes = state.attachment?.bytes,
            )
        }.fold(
            onSuccess = { created ->
                val tickets = listOf(created) + state.tickets.filterNot { it.id == created.id }
                state = state.copy(
                    isSubmitting = false,
                    tickets = tickets,
                    selectedTicket = created,
                    subject = "",
                    description = "",
                    category = SupportTicketCategory.ORDER,
                    attachment = null,
                    successMessage = "Support ticket ${created.ticketNumber} was created.",
                )
                true
            },
            onFailure = { error ->
                state = state.copy(
                    isSubmitting = false,
                    errorMessage = userReadableMessage(error, "Unable to create support ticket."),
                )
                false
            },
        )
    }

    private companion object {
        const val MaxSupportAttachmentBytes: Int = 10 * 1024 * 1024
    }
}

internal data class NotificationsUiState(
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val hasLoaded: Boolean = false,
    val notifications: List<NotificationDTO> = emptyList(),
    val unreadCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

internal class NotificationsStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: NotificationsUiState by mutableStateOf(NotificationsUiState())
        private set

    suspend fun loadIfNeeded() {
        if (!state.hasLoaded) {
            load()
        }
    }

    suspend fun load() {
        if (state.isLoading) return
        state = state.copy(isLoading = true, errorMessage = null)
        runCatching {
            val notifications = api.fetchNotifications(session.user.id)
            val unread = api.fetchUnreadNotificationCount(session.user.id)
            notifications to unread
        }.fold(
            onSuccess = { (notifications, unread) ->
                state = state.copy(
                    isLoading = false,
                    hasLoaded = true,
                    notifications = notifications,
                    unreadCount = unread,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    isLoading = false,
                    hasLoaded = true,
                    notifications = emptyList(),
                    unreadCount = 0,
                    errorMessage = userReadableMessage(error, "Unable to load notifications."),
                )
            },
        )
    }

    suspend fun markAsRead(notification: NotificationDTO) {
        if (notification.read) return
        state = state.copy(isMutating = true, errorMessage = null)
        runCatching { api.markNotificationRead(notification.id) }.fold(
            onSuccess = { updated ->
                state = state.copy(
                    isMutating = false,
                    notifications = state.notifications.map { if (it.id == updated.id) updated else it },
                    unreadCount = (state.unreadCount - 1).coerceAtLeast(0),
                )
            },
            onFailure = { error ->
                state = state.copy(isMutating = false, errorMessage = userReadableMessage(error, "Unable to mark notification as read."))
            },
        )
    }

    suspend fun markAllRead() {
        if (state.unreadCount == 0) return
        state = state.copy(isMutating = true, errorMessage = null)
        runCatching { api.markAllNotificationsRead(session.user.id) }.fold(
            onSuccess = { unread ->
                state = state.copy(
                    isMutating = false,
                    unreadCount = unread,
                    notifications = state.notifications.map { it.copy(read = true) },
                    successMessage = "All notifications marked as read.",
                )
            },
            onFailure = { error ->
                state = state.copy(isMutating = false, errorMessage = userReadableMessage(error, "Unable to mark all notifications as read."))
            },
        )
    }

    suspend fun delete(notification: NotificationDTO) {
        state = state.copy(isMutating = true, errorMessage = null)
        runCatching { api.deleteNotification(notification.id) }.fold(
            onSuccess = {
                state = state.copy(
                    isMutating = false,
                    notifications = state.notifications.filterNot { it.id == notification.id },
                    unreadCount = if (notification.read) state.unreadCount else (state.unreadCount - 1).coerceAtLeast(0),
                )
            },
            onFailure = { error ->
                state = state.copy(isMutating = false, errorMessage = userReadableMessage(error, "Unable to remove notification."))
            },
        )
    }
}

internal enum class SettingsSection {
    Profile,
    Security,
    Addresses,
    Payments,
    Notifications,
}

internal data class ProfileFormState(
    val company: String = "",
    val phone: String = "",
    val commercialRegister: String = "",
    val representativeName: String = "",
    val representativeTitle: String = "",
    val representativePhone: String = "",
    val representativeEmail: String = "",
)

internal data class PasswordFormState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

internal data class AddressFormState(
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "Saudi Arabia",
)

internal data class PaymentMethodFormState(
    val type: PaymentMethodType = PaymentMethodType.CREDIT_CARD,
    val maskedDetails: String = "",
    val isDefault: Boolean = false,
)

internal data class SettingsUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val selectedSection: SettingsSection = SettingsSection.Profile,
    val user: UserDTO,
    val profileForm: ProfileFormState = ProfileFormState(),
    val passwordForm: PasswordFormState = PasswordFormState(),
    val addresses: List<AddressResponseDto> = emptyList(),
    val addressForm: AddressFormState = AddressFormState(),
    val editingAddressId: String? = null,
    val paymentMethods: List<PaymentMethodDTO> = emptyList(),
    val paymentForm: PaymentMethodFormState = PaymentMethodFormState(),
    val preferences: List<NotificationPreferenceDTO> = emptyList(),
    val pendingDeleteAddressId: String? = null,
    val pendingDeletePaymentMethodId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

internal class SettingsStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: SettingsUiState by mutableStateOf(SettingsUiState(user = session.user))
        private set

    suspend fun load() {
        state = state.copy(isLoading = true, errorMessage = null)
        runCatching {
            val user = api.fetchUserById(session.user.id)
            SettingsLoadResult(
                user = user,
                addresses = api.fetchAddresses(),
                payments = api.fetchPaymentMethods(session.user.id),
                preferences = api.fetchNotificationPreferences(session.user.id),
            )
        }.fold(
            onSuccess = { result ->
                state = state.copy(
                    isLoading = false,
                    user = result.user,
                    profileForm = result.user.toProfileForm(),
                    addresses = result.addresses,
                    paymentMethods = result.payments,
                    preferences = result.preferences,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    isLoading = false,
                    errorMessage = userReadableMessage(error, "Unable to load settings."),
                )
            },
        )
    }

    fun selectSection(section: SettingsSection) {
        state = state.copy(selectedSection = section, errorMessage = null, successMessage = null)
    }

    fun updateProfile(form: ProfileFormState) {
        state = state.copy(profileForm = form, errorMessage = null, successMessage = null)
    }

    suspend fun saveProfile() {
        val form = state.profileForm
        if (form.company.isBlank() || form.representativeName.isBlank() || form.representativeEmail.isBlank()) {
            state = state.copy(errorMessage = "Company, representative name, and representative email are required.")
            return
        }
        save("Profile updated.") {
            val updated = api.updateUserProfile(
                session.user.id,
                UpdateUserProfileRequest(
                    company = form.company.trim(),
                    phone = form.phone.trim().ifBlank { null },
                    commercialRegister = form.commercialRegister.trim().ifBlank { null },
                    representative = uqu.drawbridge.platform.RepresentativeDto(
                        name = form.representativeName.trim(),
                        jobTitle = form.representativeTitle.trim(),
                        phoneNumber = form.representativePhone.trim(),
                        email = form.representativeEmail.trim(),
                    ),
                ),
            )
            state = state.copy(user = updated, profileForm = updated.toProfileForm())
        }
    }

    fun updatePassword(form: PasswordFormState) {
        state = state.copy(passwordForm = form, errorMessage = null, successMessage = null)
    }

    suspend fun changePassword() {
        val form = state.passwordForm
        when {
            form.currentPassword.isBlank() -> {
                state = state.copy(errorMessage = "Enter your current password.")
                return
            }
            form.newPassword.length < 8 -> {
                state = state.copy(errorMessage = "New password must be at least 8 characters.")
                return
            }
            form.newPassword != form.confirmPassword -> {
                state = state.copy(errorMessage = "New password and confirmation must match.")
                return
            }
        }
        save("Password updated.") {
            api.changePassword(
                session.user.id,
                ChangePasswordRequest(
                    currentPassword = form.currentPassword,
                    newPassword = form.newPassword,
                ),
            )
            state = state.copy(passwordForm = PasswordFormState())
        }
    }

    fun startAddAddress() {
        state = state.copy(addressForm = AddressFormState(), editingAddressId = null, errorMessage = null, successMessage = null)
    }

    fun startEditAddress(address: AddressResponseDto) {
        state = state.copy(
            editingAddressId = address.id,
            addressForm = AddressFormState(
                street = address.street,
                city = address.city,
                state = address.state,
                zipCode = address.zipCode,
                country = address.country,
            ),
            errorMessage = null,
            successMessage = null,
        )
    }

    fun updateAddressForm(form: AddressFormState) {
        state = state.copy(addressForm = form, errorMessage = null, successMessage = null)
    }

    suspend fun saveAddress() {
        val form = state.addressForm
        if (listOf(form.street, form.city, form.state, form.zipCode, form.country).any { it.isBlank() }) {
            state = state.copy(errorMessage = "All address fields are required.")
            return
        }
        val request = CreateAddressRequest(
            street = form.street.trim(),
            city = form.city.trim(),
            state = form.state.trim(),
            zipCode = form.zipCode.trim(),
            country = form.country.trim(),
        )
        save("Address saved.") {
            val saved = state.editingAddressId?.let { api.updateAddress(it, request) } ?: api.createAddress(request)
            state = state.copy(
                addresses = if (state.editingAddressId == null) {
                    state.addresses + saved
                } else {
                    state.addresses.map { if (it.id == saved.id) saved else it }
                },
                addressForm = AddressFormState(),
                editingAddressId = null,
            )
        }
    }

    suspend fun deleteAddress(addressId: String) {
        save("Address deleted.") {
            api.deleteAddress(addressId)
            state = state.copy(
                addresses = state.addresses.filterNot { it.id == addressId },
                pendingDeleteAddressId = null,
            )
        }
    }

    fun requestDeleteAddress(addressId: String?) {
        state = state.copy(pendingDeleteAddressId = addressId, errorMessage = null, successMessage = null)
    }

    fun updatePaymentForm(form: PaymentMethodFormState) {
        state = state.copy(paymentForm = form, errorMessage = null, successMessage = null)
    }

    suspend fun addPaymentMethod() {
        val form = state.paymentForm
        if (form.maskedDetails.isBlank()) {
            state = state.copy(errorMessage = "Enter masked payment details, such as Visa **** 1234.")
            return
        }
        save("Payment method saved.") {
            val saved = api.addPaymentMethod(
                CreatePaymentMethodRequest(
                    ownerId = session.user.id,
                    type = form.type.name,
                    maskedDetails = form.maskedDetails.trim(),
                    isDefault = form.isDefault,
                ),
            )
            state = state.copy(
                paymentMethods = state.paymentMethods.filterNot { it.id == saved.id } + saved,
                paymentForm = PaymentMethodFormState(),
            )
        }
    }

    suspend fun deletePaymentMethod(methodId: String) {
        save("Payment method deleted.") {
            api.deletePaymentMethod(methodId)
            state = state.copy(
                paymentMethods = state.paymentMethods.filterNot { it.id == methodId },
                pendingDeletePaymentMethodId = null,
            )
        }
    }

    fun requestDeletePaymentMethod(methodId: String?) {
        state = state.copy(pendingDeletePaymentMethodId = methodId, errorMessage = null, successMessage = null)
    }

    suspend fun setDefaultPaymentMethod(methodId: String) {
        save("Default payment method updated.") {
            val updated = api.setDefaultPaymentMethod(methodId)
            state = state.copy(paymentMethods = state.paymentMethods.map { method ->
                when (method.id) {
                    updated.id -> updated
                    else -> method.copy(isDefault = false)
                }
            })
        }
    }

    suspend fun togglePreference(key: NotificationPreferenceKey, channel: NotificationChannel) {
        val existing = state.preferences.firstOrNull { it.preferenceKey == key && it.channel == channel }
        val enabled = !(existing?.enabled ?: (channel != NotificationChannel.SMS))
        save("Notification preference updated.") {
            val updated = api.upsertNotificationPreference(
                session.user.id,
                UpsertNotificationPreferenceRequest(
                    preferenceKey = key,
                    channel = channel,
                    enabled = enabled,
                ),
            )
            state = state.copy(preferences = state.preferences.filterNot {
                it.preferenceKey == key && it.channel == channel
            } + updated)
        }
    }

    private suspend fun save(successMessage: String, block: suspend () -> Unit) {
        state = state.copy(isSaving = true, errorMessage = null, successMessage = null)
        runCatching { block() }.fold(
            onSuccess = { state = state.copy(isSaving = false, successMessage = successMessage) },
            onFailure = { error ->
                state = state.copy(
                    isSaving = false,
                    errorMessage = userReadableMessage(error, "Unable to save changes."),
                )
            },
        )
    }

    private data class SettingsLoadResult(
        val user: UserDTO,
        val addresses: List<AddressResponseDto>,
        val payments: List<PaymentMethodDTO>,
        val preferences: List<NotificationPreferenceDTO>,
    )
}

private fun UserDTO.toProfileForm(): ProfileFormState = ProfileFormState(
    company = company,
    phone = phone.orEmpty(),
    commercialRegister = commercialRegister.orEmpty(),
    representativeName = representative?.name.orEmpty(),
    representativeTitle = representative?.jobTitle.orEmpty(),
    representativePhone = representative?.phoneNumber.orEmpty(),
    representativeEmail = representative?.email.orEmpty(),
)
