package uqu.drawbridge.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MobileApiException(
    message: String,
    val statusCode: Int? = null,
    val isUnauthorized: Boolean = false,
) : Exception(message)

data class MobileSession(
    val token: String,
    val user: UserDTO,
)

data class DashboardSummary(
    val totalOrders: Int,
    val pendingOrders: Int,
    val processingOrders: Int,
    val totalAmount: Double,
)

class MobileAuthApi(
    private val client: HttpClient = createMobileHttpClient(),
    private val tokenProvider: suspend () -> String? = { null },
    private val onUnauthorized: suspend () -> Unit = {},
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun login(email: String, password: String, rememberMe: Boolean): MobileSession {
        val response = client.post(buildUrl("/auth/login")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password, rememberMe = rememberMe))
        }
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)

        val authResponse = json.decodeFromString(AuthResponsePayload.serializer(), response.bodyAsText()).toAuthResponse()
        val user = fetchUserById(authResponse.userId, tokenOverride = authResponse.token, notifyUnauthorized = false)
        return MobileSession(token = authResponse.token, user = user)
    }

    suspend fun register(request: RegisterRequest): MobileSession {
        val response = client.post(buildUrl("/auth/register")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(request.toPayload())
        }
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)

        val authResponse = json.decodeFromString(AuthResponsePayload.serializer(), response.bodyAsText()).toAuthResponse()
        val user = fetchUserById(authResponse.userId, tokenOverride = authResponse.token, notifyUnauthorized = false)
        return MobileSession(token = authResponse.token, user = user)
    }

    suspend fun logout() {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/auth/logout")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(LogoutRequest(token = token))
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun fetchDashboardSummary(userId: String, role: UserRole): DashboardSummary {
        val endpoint = if (role == UserRole.WHOLESALER) {
            "/orders/wholesaler/$userId"
        } else {
            "/orders/retailer/$userId"
        }

        val response = authorizedGet(endpoint)
        ensureSuccess(response.status, response.bodyAsText())

        val items = json.parseToJsonElement(response.bodyAsText()).jsonArray
        var pending = 0
        var processing = 0
        var total = 0.0

        items.forEach { item ->
            val obj = item.jsonObject
            val statusToken = obj.stringValue("status")
            val subtotal = obj.doubleValue("subtotal")
            if (statusToken == OrderStatus.PENDING.name) {
                pending += 1
            }
            if (statusToken == OrderStatus.PROCESSING.name) {
                processing += 1
            }
            total += subtotal
        }

        return DashboardSummary(
            totalOrders = items.size,
            pendingOrders = pending,
            processingOrders = processing,
            totalAmount = total,
        )
    }

    suspend fun fetchUserById(userId: String): UserDTO {
        return fetchUserById(userId, tokenOverride = null, notifyUnauthorized = true)
    }

    private suspend fun fetchUserById(
        userId: String,
        tokenOverride: String?,
        notifyUnauthorized: Boolean,
    ): UserDTO {
        val response = authorizedGet("/users/$userId", tokenOverride)
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = notifyUnauthorized)
        val payload = json.decodeFromString(UserPayload.serializer(), response.bodyAsText())
        return payload.toUserDto()
    }

    suspend fun scanBarcode(retailerId: String, gtin: String): PosScanResponse {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/inventory/scan")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(PosScanRequest(retailerId = retailerId, gtin = gtin))
        }

        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        val parsed = json.parseToJsonElement(body).jsonObject
        return PosScanResponse(
            productName = parsed.stringValue("productName"),
            newStock = parsed["newStock"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            message = parsed.stringValue("message"),
        )
    }

    suspend fun fetchInventoryByRetailer(retailerId: String): List<InventoryItemDTO> {
        val response = authorizedGet("/inventory/retailer/$retailerId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun fetchInventoryItem(inventoryItemId: String): InventoryItemDTO {
        val response = authorizedGet("/inventory/$inventoryItemId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(InventoryItemDTO.serializer(), body)
    }

    suspend fun createInventoryItem(request: CreateInventoryItemRequest): InventoryItemDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/inventory")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(InventoryItemDTO.serializer(), body)
    }

    suspend fun updateInventoryQuantity(inventoryItemId: String, quantity: Int): InventoryItemDTO {
        val token = requireBearerToken()
        val response = client.patch(buildUrl("/inventory/$inventoryItemId/quantity")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            parameter("quantity", quantity)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(InventoryItemDTO.serializer(), body)
    }

    suspend fun toggleAutoOrderConfig(inventoryItemId: String, enabled: Boolean): AutoOrderConfigDTO {
        val token = requireBearerToken()
        val response = client.patch(buildUrl("/inventory/auto-order/$inventoryItemId/toggle")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            parameter("enabled", enabled)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(AutoOrderConfigDTO.serializer(), body)
    }

    suspend fun updateAutoOrderConfig(
        inventoryItemId: String,
        request: UpdateAutoOrderConfigRequest,
    ): AutoOrderConfigDTO {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/inventory/auto-order/$inventoryItemId")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(AutoOrderConfigDTO.serializer(), body)
    }

    suspend fun fetchInventoryAuditLogs(
        productId: String? = null,
        inventoryItemId: String? = null,
        stockTargetType: String? = null,
        sourceType: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): InventoryAuditLogPageResponse {
        val token = requireBearerToken()
        val response = client.get(buildUrl("/inventory/logs")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            productId?.takeIf { it.isNotBlank() }?.let { parameter("productId", it) }
            inventoryItemId?.takeIf { it.isNotBlank() }?.let { parameter("inventoryItemId", it) }
            stockTargetType?.takeIf { it.isNotBlank() }?.let { parameter("stockTargetType", it) }
            sourceType?.takeIf { it.isNotBlank() }?.let { parameter("sourceType", it) }
            parameter("page", page)
            parameter("size", size)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(InventoryAuditLogPageResponse.serializer(), body)
    }

    suspend fun fetchProductsByWholesaler(wholesalerId: String): List<ProductDTO> {
        val response = authorizedGet("/products/wholesaler/$wholesalerId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun createProduct(request: CreateProductRequest): ProductDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/products")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(ProductDTO.serializer(), body)
    }

    suspend fun updateProduct(productId: String, request: CreateProductRequest): ProductDTO {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/products/$productId")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(ProductDTO.serializer(), body)
    }

    suspend fun uploadProductImage(
        productId: String,
        fileName: String,
        mimeType: String?,
        bytes: ByteArray,
        altText: String = "",
        sortIndex: Int? = null,
    ): ProductImageResponse {
        val token = requireBearerToken()
        val contentType = mimeType?.takeIf { it.isNotBlank() } ?: ContentType.Image.JPEG.toString()
        val safeName = fileName.ifBlank { "product-image.jpg" }.replace("\"", "")
        val response = client.post(buildUrl("/products/$productId/images")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentDisposition, "filename=\"$safeName\"")
                                append(HttpHeaders.ContentType, contentType)
                            },
                        )
                        append("altText", altText)
                        sortIndex?.let { append("sortIndex", it.toString()) }
                    },
                ),
            )
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(ProductImageResponse.serializer(), body)
    }

    suspend fun fetchProductImages(productId: String): List<ProductImageResponse> {
        val response = authorizedGet("/products/$productId/images")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun reorderProductImages(productId: String, orderedImageIds: List<String>) {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/products/$productId/images/reorder")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(orderedImageIds)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun deleteProductImage(imageId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/images/$imageId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun deleteProduct(productId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/products/$productId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun toggleProductPublished(productId: String): ProductDTO {
        val token = requireBearerToken()
        val response = client.patch(buildUrl("/products/$productId/toggle-published")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(ProductDTO.serializer(), body)
    }

    suspend fun fetchMarketplaceProducts(query: MarketplaceProductQuery = MarketplaceProductQuery()): PaginatedProductResponse {
        val response = client.get(buildUrl("/products")) {
            accept(ContentType.Application.Json)
            parameter("page", query.page.coerceAtLeast(0))
            parameter("size", query.size.coerceIn(1, 100))
            query.search?.trim()?.takeIf { it.isNotEmpty() }?.let { parameter("search", it) }
            query.categoryIds.filter { it.isNotBlank() }.forEach { parameter("category", it.trim()) }
            query.brands.filter { it.isNotBlank() }.forEach { parameter("brand", it.trim()) }
            query.minPrice?.takeIf { it >= 0.0 }?.let { parameter("minPrice", it) }
            query.maxPrice?.takeIf { it >= 0.0 }?.let { parameter("maxPrice", it) }
            query.sort.trim().takeIf { it.isNotEmpty() }?.let { parameter("sort", it) }
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body, notifyUnauthorized = false)
        return json.decodeFromString(PaginatedProductResponse.serializer(), body)
    }

    suspend fun fetchProductById(productId: String): ProductDTO {
        val response = client.get(buildUrl("/products/$productId")) {
            accept(ContentType.Application.Json)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body, notifyUnauthorized = false)
        return json.decodeFromString(ProductDTO.serializer(), body)
    }

    suspend fun fetchProductCategories(): List<CategoryDTO> {
        val response = client.get(buildUrl("/products/categories")) {
            accept(ContentType.Application.Json)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body, notifyUnauthorized = false)
        return json.decodeFromString(body)
    }

    suspend fun fetchProductBrands(): List<String> {
        val response = client.get(buildUrl("/products/brands")) {
            accept(ContentType.Application.Json)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body, notifyUnauthorized = false)
        return json.decodeFromString(body)
    }

    suspend fun fetchImageBytes(imageUrl: String): ByteArray {
        val resolvedUrl = MobileApiConfig.resolveResourceUrl(imageUrl)
        val response = client.get(resolvedUrl)
        if (!response.status.isSuccess()) {
            ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)
        }
        return response.body()
    }

    suspend fun fetchWishlist(userId: String): List<WishlistDTO> {
        val response = authorizedGet("/wishlist/$userId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun addToWishlist(userId: String, productId: String): WishlistDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/wishlist/$userId/$productId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(WishlistDTO.serializer(), body)
    }

    suspend fun removeFromWishlist(userId: String, productId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/wishlist/$userId/$productId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun fetchCartItems(retailerId: String): List<CartItemDTO> {
        val response = authorizedGet("/cart/$retailerId/items")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun addCartItem(retailerId: String, productId: String, quantity: Int): CartItemDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/cart/$retailerId/items")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(AddToCartRequest(productId = productId, quantity = quantity))
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(CartItemDTO.serializer(), body)
    }

    suspend fun updateCartItemQuantity(retailerId: String, productId: String, quantity: Int): CartItemDTO {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/cart/$retailerId/items/$productId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            parameter("quantity", quantity)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(CartItemDTO.serializer(), body)
    }

    suspend fun removeCartItem(retailerId: String, productId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/cart/$retailerId/items/$productId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun clearCart(retailerId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/cart/$retailerId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun checkoutCart(retailerId: String): OrderGroupDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/cart/$retailerId/checkout")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(OrderGroupDTO.serializer(), body)
    }

    suspend fun fetchOrdersByRetailer(retailerId: String): List<OrderDTO> {
        val response = authorizedGet("/orders/retailer/$retailerId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun fetchOrdersByWholesaler(wholesalerId: String): List<OrderDTO> {
        val response = authorizedGet("/orders/wholesaler/$wholesalerId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun fetchOrderById(orderId: String): OrderDTO {
        val response = authorizedGet("/orders/$orderId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(OrderDTO.serializer(), body)
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): OrderDTO {
        val token = requireBearerToken()
        val response = client.patch(buildUrl("/orders/$orderId/status")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            parameter("status", status.name)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(OrderDTO.serializer(), body)
    }

    suspend fun confirmOrderDelivery(orderId: String): OrderDTO {
        val token = requireBearerToken()
        val response = client.patch(buildUrl("/orders/$orderId/confirm-delivery")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(OrderDTO.serializer(), body)
    }

    suspend fun cancelOrder(orderId: String): OrderDTO {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/orders/$orderId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(OrderDTO.serializer(), body)
    }

    suspend fun fetchInvoiceByOrder(orderId: String): InvoiceDTO {
        val response = authorizedGet("/payments/invoices/order/$orderId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(InvoiceDTO.serializer(), body)
    }

    suspend fun forgotPassword(email: String) {
        val response = client.post(buildUrl("/auth/forgot-password")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email = email))
        }
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)
    }

    suspend fun resetPassword(token: String, newPassword: String) {
        val response = client.post(buildUrl("/auth/reset-password")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(ResetPasswordRequest(token = token, newPassword = newPassword))
        }
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)
    }

    suspend fun verifyEmail(token: String) {
        val response = client.post(buildUrl("/auth/verify-email")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(VerifyEmailRequest(token = token))
        }
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)
    }

    suspend fun resendVerification(email: String) {
        val response = client.post(buildUrl("/auth/resend-verification")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            setBody(ResendVerificationRequest(email = email))
        }
        ensureSuccess(response.status, response.bodyAsText(), notifyUnauthorized = false)
    }

    suspend fun updateUserProfile(userId: String, request: UpdateUserProfileRequest): UserDTO {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/users/$userId")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(UserPayload.serializer(), body).toUserDto()
    }

    suspend fun changePassword(userId: String, request: ChangePasswordRequest) {
        val token = requireBearerToken()
        val response = client.patch(buildUrl("/users/$userId/password")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun fetchAddresses(): List<AddressResponseDto> {
        val response = authorizedGet("/addresses")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun createAddress(request: CreateAddressRequest): AddressResponseDto {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/addresses")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(AddressResponseDto.serializer(), body)
    }

    suspend fun updateAddress(addressId: String, request: CreateAddressRequest): AddressResponseDto {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/addresses/$addressId")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(AddressResponseDto.serializer(), body)
    }

    suspend fun deleteAddress(addressId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/addresses/$addressId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun fetchPaymentMethods(ownerId: String): List<PaymentMethodDTO> {
        val response = authorizedGet("/payments/methods/owner/$ownerId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun addPaymentMethod(request: CreatePaymentMethodRequest): PaymentMethodDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/payments/methods")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(PaymentMethodDTO.serializer(), body)
    }

    suspend fun deletePaymentMethod(paymentMethodId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/payments/methods/$paymentMethodId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun setDefaultPaymentMethod(paymentMethodId: String): PaymentMethodDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/payments/methods/$paymentMethodId/default")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(PaymentMethodDTO.serializer(), body)
    }

    suspend fun fetchSupportTickets(): List<SupportTicketDTO> {
        val response = authorizedGet("/support/my")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun fetchSupportTicket(ticketId: String): SupportTicketDTO {
        val response = authorizedGet("/support/$ticketId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(SupportTicketDTO.serializer(), body)
    }

    suspend fun createSupportTicket(request: CreateTicketRequest): SupportTicketDTO {
        val token = requireBearerToken()
        val response = client.post(buildUrl("/support")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("subject", request.subject.trim())
                        append("category", request.category.name)
                        append("description", request.description.trim())
                    },
                ),
            )
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(SupportTicketDTO.serializer(), body)
    }

    suspend fun fetchNotifications(recipientId: String): List<NotificationDTO> {
        val response = authorizedGet("/notifications/recipient/$recipientId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun fetchUnreadNotificationCount(recipientId: String): Int {
        val response = authorizedGet("/notifications/recipient/$recipientId/unread-count")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(UnreadCountDTO.serializer(), body).count
    }

    suspend fun markNotificationRead(notificationId: String): NotificationDTO {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/notifications/$notificationId/read")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(NotificationDTO.serializer(), body)
    }

    suspend fun markAllNotificationsRead(recipientId: String): Int {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/notifications/recipient/$recipientId/read-all")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(UnreadCountDTO.serializer(), body).count
    }

    suspend fun deleteNotification(notificationId: String) {
        val token = requireBearerToken()
        val response = client.delete(buildUrl("/notifications/$notificationId")) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
        ensureSuccess(response.status, response.bodyAsText())
    }

    suspend fun fetchNotificationPreferences(userId: String): List<NotificationPreferenceDTO> {
        val response = authorizedGet("/notifications/preferences/$userId")
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(body)
    }

    suspend fun upsertNotificationPreference(
        userId: String,
        request: UpsertNotificationPreferenceRequest,
    ): NotificationPreferenceDTO {
        val token = requireBearerToken()
        val response = client.put(buildUrl("/notifications/preferences/$userId")) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        val body = response.bodyAsText()
        ensureSuccess(response.status, body)
        return json.decodeFromString(NotificationPreferenceDTO.serializer(), body)
    }

    private fun buildUrl(path: String): String = "${MobileApiConfig.baseUrl}${path}"

    private suspend fun authorizedGet(path: String, tokenOverride: String? = null): HttpResponse {
        val token = requireBearerToken(tokenOverride)
        return client.get(buildUrl(path)) {
            accept(ContentType.Application.Json)
            bearerAuth(token)
        }
    }

    private suspend fun requireBearerToken(tokenOverride: String? = null): String {
        val token = tokenOverride ?: tokenProvider()
        if (!token.isNullOrBlank()) {
            return token
        }

        onUnauthorized()
        throw MobileApiException(
            message = "Your session expired. Please sign in again.",
            statusCode = HttpStatusCode.Unauthorized.value,
            isUnauthorized = true,
        )
    }

    private suspend fun ensureSuccess(
        status: HttpStatusCode,
        body: String,
        notifyUnauthorized: Boolean = true,
    ) {
        if (status.isSuccess()) {
            return
        }

        val isUnauthorized = status == HttpStatusCode.Unauthorized
        if (isUnauthorized && notifyUnauthorized) {
            onUnauthorized()
        }

        val message = runCatching {
            json.parseToJsonElement(body)
                .jsonObject["message"]
                ?.jsonPrimitive
                ?.content
        }.getOrNull()

        throw MobileApiException(
            message = if (isUnauthorized && notifyUnauthorized) {
                "Your session expired. Please sign in again."
            } else {
                message ?: "Request failed with status ${status.value}"
            },
            statusCode = status.value,
            isUnauthorized = isUnauthorized,
        )
    }

    private fun RegisterRequest.toPayload(): RegisterRequestPayload {
        return RegisterRequestPayload(
            email = email,
            password = password,
            phoneNumber = phoneNumber,
            role = role.name,
            businessName = businessName,
            commercialRegistrationNumber = commercialRegistrationNumber,
            repName = repName,
            repJobTitle = repJobTitle,
            repPhoneNumber = repPhoneNumber,
            repEmail = repEmail,
            addresses = addresses.map { address ->
                AddressPayload(
                    id = address.id,
                    street = address.street,
                    city = address.city,
                    state = address.state,
                    zipCode = address.zipCode,
                    country = address.country,
                )
            },
        )
    }

}

@Serializable
private data class AddressPayload(
    val id: String? = null,
    val street: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String,
)

@Serializable
private data class RegisterRequestPayload(
    val email: String,
    val password: String,
    val phoneNumber: String,
    val role: String,
    val businessName: String? = null,
    val commercialRegistrationNumber: String,
    val repName: String,
    val repJobTitle: String,
    val repPhoneNumber: String,
    val repEmail: String,
    val addresses: List<AddressPayload>,
)

@Serializable
private data class AuthResponsePayload(
    val token: String,
    val userId: String,
    val email: String,
    val name: String,
    val role: String,
) {
    fun toAuthResponse(): AuthResponse {
        return AuthResponse(
            token = token,
            userId = userId,
            email = email,
            name = name,
            role = enumValueOfOrDefault(role, UserRole.RETAILER),
        )
    }
}

@Serializable
private data class UserPayload(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val company: String,
    val phone: String? = null,
    val addresses: List<AddressPayload>? = null,
    val representative: RepresentativePayload? = null,
    val commercialRegister: String? = null,
    val verificationStatus: String? = null,
    val avatar: String? = null,
) {
    fun toUserDto(): UserDTO {
        return UserDTO(
            id = id,
            name = name,
            email = email,
            role = enumValueOfOrDefault(role, UserRole.RETAILER),
            company = company,
            phone = phone,
            addresses = addresses?.map { addr ->
                AddressDto(
                    id = addr.id,
                    street = addr.street,
                    city = addr.city,
                    state = addr.state,
                    zipCode = addr.zipCode,
                    country = addr.country,
                )
            }?.toTypedArray(),
            representative = representative?.toDto(),
            commercialRegister = commercialRegister,
            verificationStatus = verificationStatus?.let { enumValueOfOrDefault(it, VerificationStatus.PENDING) },
            avatar = avatar,
        )
    }
}

@Serializable
private data class RepresentativePayload(
    val name: String,
    val jobTitle: String,
    val phoneNumber: String,
    val email: String,
) {
    fun toDto(): RepresentativeDto {
        return RepresentativeDto(
            name = name,
            jobTitle = jobTitle,
            phoneNumber = phoneNumber,
            email = email,
        )
    }
}

private inline fun <reified T : Enum<T>> enumValueOfOrDefault(token: String, default: T): T {
    return runCatching { enumValueOf<T>(token) }.getOrDefault(default)
}

private fun createMobileHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }
}

private fun JsonObject.stringValue(key: String): String {
    val element = this[key] ?: return ""
    return when (element) {
        is JsonPrimitive -> element.content
        else -> element.toString()
    }
}

private fun JsonObject.doubleValue(key: String): Double {
    val element = this[key] ?: return 0.0
    return when (element) {
        is JsonPrimitive -> element.doubleOrNull ?: 0.0
        else -> 0.0
    }
}
