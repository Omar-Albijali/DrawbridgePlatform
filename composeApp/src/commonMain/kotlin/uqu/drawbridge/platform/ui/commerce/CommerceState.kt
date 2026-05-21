package uqu.drawbridge.platform.ui.commerce

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uqu.drawbridge.platform.CartItemDTO
import uqu.drawbridge.platform.InvoiceDTO
import uqu.drawbridge.platform.MobileApiException
import uqu.drawbridge.platform.MobileAuthApi
import uqu.drawbridge.platform.OrderDTO
import uqu.drawbridge.platform.OrderGroupDTO
import uqu.drawbridge.platform.OrderStatus
import uqu.drawbridge.platform.ProductDTO
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.auth.AuthActionResult
import uqu.drawbridge.platform.ui.common.userReadableMessage
import uqu.drawbridge.platform.ui.model.SessionState

internal data class CartProductItem(
    val cartItem: CartItemDTO,
    val product: ProductDTO?,
) {
    val productId: String = cartItem.productId
    val quantity: Int = cartItem.quantity
    val minimumOrderQuantity: Int = product?.minimumOrderQuantity?.coerceAtLeast(1) ?: 1
    val availableStock: Int? = product?.stock
    val lineTotal: Double = (product?.price ?: 0.0) * quantity
    val hasMissingProduct: Boolean = product == null
    val isBelowMinimumOrder: Boolean = quantity < minimumOrderQuantity
    val isAboveStock: Boolean = availableStock?.let { quantity > it } ?: false
}

internal data class CartUiState(
    val items: List<CartProductItem> = emptyList(),
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val busyProductIds: Set<String> = emptySet(),
    val isClearing: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val itemCount: Int = items.sumOf { it.quantity }
    val estimatedSubtotal: Double = items.sumOf { it.lineTotal }
    val hasInvalidItems: Boolean = items.any { it.hasMissingProduct || it.isBelowMinimumOrder || it.isAboveStock }
}

internal data class CheckoutUiState(
    val isSubmitting: Boolean = false,
    val successGroup: OrderGroupDTO? = null,
    val errorMessage: String? = null,
)

internal data class CheckoutResult(
    val success: Boolean,
    val message: String? = null,
    val orderGroup: OrderGroupDTO? = null,
)

internal class CartStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: CartUiState by mutableStateOf(CartUiState())
        private set

    var checkoutState: CheckoutUiState by mutableStateOf(CheckoutUiState())
        private set

    val isEnabled: Boolean = session.user.role == UserRole.RETAILER

    suspend fun loadInitial() {
        if (state.hasLoaded || state.isLoading) {
            return
        }
        refresh()
    }

    suspend fun refresh() {
        if (!isEnabled) {
            state = CartUiState(
                hasLoaded = true,
                errorMessage = "Cart is available for retailer accounts.",
            )
            return
        }

        state = state.copy(
            isLoading = !state.hasLoaded,
            isRefreshing = state.hasLoaded,
            errorMessage = null,
            actionMessage = null,
        )

        runCatching {
            api.fetchCartItems(session.user.id).map { item ->
                CartProductItem(
                    cartItem = item,
                    product = runCatching { api.fetchProductById(item.productId) }.getOrNull(),
                )
            }
        }.fold(
            onSuccess = { items ->
                state = state.copy(
                    items = items,
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    items = if (state.hasLoaded) state.items else emptyList(),
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = userReadableMessage(error, "Unable to load your cart right now."),
                )
            },
        )
    }

    fun clearActionMessage() {
        state = state.copy(actionMessage = null)
    }

    fun clearCheckoutResult() {
        checkoutState = CheckoutUiState()
    }

    suspend fun fetchImageBytes(imageUrl: String): ByteArray {
        return api.fetchImageBytes(imageUrl)
    }

    suspend fun addProduct(product: ProductDTO, requestedQuantity: Int? = null): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "Cart is available for retailer accounts.")
        }

        val minimumOrderQuantity = product.minimumOrderQuantity.coerceAtLeast(1)
        val quantity = (requestedQuantity ?: minimumOrderQuantity).coerceAtLeast(minimumOrderQuantity)
        if (product.stock < minimumOrderQuantity) {
            return AuthActionResult(
                success = false,
                message = "This product does not have enough stock to meet the minimum order quantity.",
            )
        }
        if (quantity > product.stock) {
            return AuthActionResult(success = false, message = "Only ${product.stock} units are available.")
        }

        return performCartProductAction(
            productId = product.id,
            successMessage = "Added ${product.name} to cart.",
        ) {
            api.addCartItem(session.user.id, product.id, quantity)
        }
    }

    suspend fun updateQuantity(item: CartProductItem, requestedQuantity: Int): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "Cart is available for retailer accounts.")
        }

        val minimum = item.minimumOrderQuantity.coerceAtLeast(1)
        val max = item.availableStock
        val nextQuantity = max
            ?.takeIf { it >= minimum }
            ?.let { requestedQuantity.coerceIn(minimum, it) }
            ?: requestedQuantity.coerceAtLeast(minimum)

        return performCartProductAction(
            productId = item.productId,
            successMessage = "Quantity updated.",
        ) {
            api.updateCartItemQuantity(session.user.id, item.productId, nextQuantity)
        }
    }

    suspend fun removeItem(item: CartProductItem): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "Cart is available for retailer accounts.")
        }

        return performCartProductAction(
            productId = item.productId,
            successMessage = "Removed from cart.",
        ) {
            api.removeCartItem(session.user.id, item.productId)
        }
    }

    suspend fun clearCart(): AuthActionResult {
        if (!isEnabled) {
            return AuthActionResult(success = false, message = "Cart is available for retailer accounts.")
        }

        state = state.copy(isClearing = true, errorMessage = null, actionMessage = null)
        return runCatching {
            api.clearCart(session.user.id)
            refresh()
        }.fold(
            onSuccess = {
                state = state.copy(isClearing = false, actionMessage = "Cart cleared.")
                AuthActionResult(success = true, message = "Cart cleared.")
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to clear the cart.")
                state = state.copy(isClearing = false, errorMessage = message)
                AuthActionResult(success = false, message = message)
            },
        )
    }

    suspend fun checkout(): CheckoutResult {
        if (!isEnabled) {
            val message = "Checkout is available for retailer accounts."
            checkoutState = CheckoutUiState(errorMessage = message)
            return CheckoutResult(success = false, message = message)
        }
        if (state.items.isEmpty()) {
            val message = "Your cart is empty."
            checkoutState = CheckoutUiState(errorMessage = message)
            return CheckoutResult(success = false, message = message)
        }
        val invalidItem = state.items.firstOrNull { it.hasMissingProduct || it.isBelowMinimumOrder || it.isAboveStock }
        if (invalidItem != null) {
            val productName = invalidItem.product?.name ?: "An item"
            val message = when {
                invalidItem.hasMissingProduct -> "An item in your cart is no longer available. Remove it before checkout."
                invalidItem.isBelowMinimumOrder -> "$productName is below the minimum order quantity of ${invalidItem.minimumOrderQuantity}."
                else -> "$productName exceeds available stock."
            }
            checkoutState = CheckoutUiState(errorMessage = message)
            return CheckoutResult(success = false, message = message)
        }

        checkoutState = CheckoutUiState(isSubmitting = true)
        return runCatching { api.checkoutCart(session.user.id) }.fold(
            onSuccess = { group ->
                refresh()
                checkoutState = CheckoutUiState(successGroup = group)
                CheckoutResult(
                    success = true,
                    message = "Order placed. Payment is pending.",
                    orderGroup = group,
                )
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Checkout failed. Please review your cart and try again.")
                checkoutState = CheckoutUiState(errorMessage = message)
                CheckoutResult(success = false, message = message)
            },
        )
    }

    private suspend fun performCartProductAction(
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
        }.fold(
            onSuccess = {
                state = state.copy(
                    busyProductIds = state.busyProductIds - productId,
                    actionMessage = successMessage,
                )
                AuthActionResult(success = true, message = successMessage)
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Cart update failed. Please try again.")
                state = state.copy(
                    busyProductIds = state.busyProductIds - productId,
                    errorMessage = message,
                )
                AuthActionResult(success = false, message = message)
            },
        )
    }
}

internal enum class OrderStatusFilter(val label: String) {
    All("All"),
    Pending("Pending"),
    Confirmed("Confirmed"),
    Processing("Processing"),
    Shipped("Shipped"),
    Delivered("Delivered"),
    Cancelled("Cancelled"),
}

internal data class OrdersUiState(
    val orders: List<OrderDTO> = emptyList(),
    val searchInput: String = "",
    val statusFilter: OrderStatusFilter = OrderStatusFilter.All,
    val hasLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val busyOrderIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val filteredOrders: List<OrderDTO>
        get() {
            val search = searchInput.trim()
            return orders
                .filter { order ->
                    search.isEmpty() ||
                        order.id.contains(search, ignoreCase = true) ||
                        order.retailerName.contains(search, ignoreCase = true) ||
                        order.items.any { item -> item.productName.contains(search, ignoreCase = true) }
                }
                .filter { order ->
                    when (statusFilter) {
                        OrderStatusFilter.All -> true
                        OrderStatusFilter.Pending -> order.status == OrderStatus.PENDING
                        OrderStatusFilter.Confirmed -> order.status == OrderStatus.CONFIRMED
                        OrderStatusFilter.Processing -> order.status == OrderStatus.PROCESSING
                        OrderStatusFilter.Shipped -> order.status == OrderStatus.SHIPPED
                        OrderStatusFilter.Delivered -> order.status == OrderStatus.DELIVERED
                        OrderStatusFilter.Cancelled -> order.status == OrderStatus.CANCELLED
                    }
                }
                .sortedByDescending { it.placedAt }
        }
}

internal data class OrderDetailUiState(
    val order: OrderDTO? = null,
    val invoice: InvoiceDTO? = null,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null,
)

internal class OrdersStateHolder(
    private val api: MobileAuthApi,
    private val session: SessionState,
) {
    var state: OrdersUiState by mutableStateOf(OrdersUiState())
        private set

    var detailState: OrderDetailUiState by mutableStateOf(OrderDetailUiState())
        private set

    suspend fun loadInitial() {
        if (state.hasLoaded || state.isLoading) {
            return
        }
        refresh()
    }

    suspend fun refresh() {
        state = state.copy(
            isLoading = !state.hasLoaded,
            isRefreshing = state.hasLoaded,
            errorMessage = null,
            actionMessage = null,
        )
        runCatching {
            fetchOrdersForRole()
        }.fold(
            onSuccess = { orders ->
                state = state.copy(
                    orders = orders,
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            },
            onFailure = { error ->
                state = state.copy(
                    hasLoaded = true,
                    isLoading = false,
                    isRefreshing = false,
                    errorMessage = userReadableMessage(error, "Unable to load orders right now."),
                )
            },
        )
    }

    fun updateSearchInput(value: String) {
        state = state.copy(searchInput = value)
    }

    fun selectStatusFilter(filter: OrderStatusFilter) {
        state = state.copy(statusFilter = filter)
    }

    suspend fun loadOrder(orderId: String, force: Boolean = false) {
        if (!force && detailState.order?.id == orderId && detailState.errorMessage == null) {
            return
        }

        detailState = OrderDetailUiState(isLoading = true)
        runCatching { api.fetchOrderById(orderId) }.fold(
            onSuccess = { order ->
                detailState = OrderDetailUiState(order = order)
            },
            onFailure = { error ->
                detailState = OrderDetailUiState(
                    errorMessage = userReadableMessage(error, "Unable to load this order."),
                )
            },
        )
    }

    fun clearOrderDetail() {
        detailState = OrderDetailUiState()
    }

    suspend fun fetchImageBytes(imageUrl: String): ByteArray {
        return api.fetchImageBytes(imageUrl)
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): AuthActionResult {
        val label = when (status) {
            OrderStatus.CONFIRMED -> "Order confirmed."
            OrderStatus.PROCESSING -> "Order marked as processing."
            OrderStatus.SHIPPED -> "Order marked as shipped."
            else -> "Order updated."
        }
        return mutateOrder(orderId = orderId, successMessage = label) {
            api.updateOrderStatus(orderId, status)
        }
    }

    suspend fun confirmDelivery(orderId: String): AuthActionResult {
        return mutateOrder(orderId = orderId, successMessage = "Receipt confirmed.") {
            api.confirmOrderDelivery(orderId)
        }
    }

    suspend fun cancelOrder(orderId: String): AuthActionResult {
        if (session.user.role != UserRole.RETAILER) {
            return AuthActionResult(success = false, message = "Only retailer accounts can cancel orders.")
        }
        return mutateOrder(orderId = orderId, successMessage = "Order cancelled.") {
            api.cancelOrder(orderId)
        }
    }

    suspend fun loadInvoice(orderId: String): AuthActionResult {
        detailState = detailState.copy(isActionInProgress = true, errorMessage = null)
        return runCatching { api.fetchInvoiceByOrder(orderId) }.fold(
            onSuccess = { invoice ->
                detailState = detailState.copy(invoice = invoice, isActionInProgress = false)
                AuthActionResult(
                    success = true,
                    message = "Invoice ${invoice.invoiceNumber} is ready.",
                )
            },
            onFailure = { error ->
                val apiError = error as? MobileApiException
                val message = if (apiError?.statusCode == 404) {
                    "Invoice generated from order details."
                } else {
                    userReadableMessage(error, "Invoice is not available for this order yet.")
                }
                detailState = detailState.copy(isActionInProgress = false)
                AuthActionResult(success = apiError?.statusCode == 404, message = message)
            },
        )
    }

    private suspend fun mutateOrder(
        orderId: String,
        successMessage: String,
        mutation: suspend () -> OrderDTO,
    ): AuthActionResult {
        state = state.copy(busyOrderIds = state.busyOrderIds + orderId, actionMessage = null)
        if (detailState.order?.id == orderId) {
            detailState = detailState.copy(isActionInProgress = true, errorMessage = null)
        }

        return runCatching { mutation() }.fold(
            onSuccess = { updatedOrder ->
                val refreshedOrders = runCatching { fetchOrdersForRole() }.getOrNull()
                state = state.copy(
                    orders = refreshedOrders ?: state.orders.map { if (it.id == orderId) updatedOrder else it },
                    busyOrderIds = state.busyOrderIds - orderId,
                    errorMessage = null,
                    actionMessage = successMessage,
                )
                if (detailState.order?.id == orderId) {
                    detailState = detailState.copy(order = updatedOrder, isActionInProgress = false)
                }
                AuthActionResult(success = true, message = successMessage)
            },
            onFailure = { error ->
                val message = userReadableMessage(error, "Unable to update this order.")
                state = state.copy(
                    busyOrderIds = state.busyOrderIds - orderId,
                    errorMessage = message,
                    actionMessage = null,
                )
                if (detailState.order?.id == orderId) {
                    detailState = detailState.copy(isActionInProgress = false)
                }
                AuthActionResult(success = false, message = message)
            },
        )
    }

    private suspend fun fetchOrdersForRole(): List<OrderDTO> {
        return when (session.user.role) {
            UserRole.RETAILER -> api.fetchOrdersByRetailer(session.user.id)
            UserRole.WHOLESALER -> api.fetchOrdersByWholesaler(session.user.id)
        }
    }
}
