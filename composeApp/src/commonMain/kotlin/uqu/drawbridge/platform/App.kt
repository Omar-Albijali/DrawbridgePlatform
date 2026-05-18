package uqu.drawbridge.platform

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.ui.auth.AuthSessionManager
import uqu.drawbridge.platform.ui.auth.AuthSessionState
import uqu.drawbridge.platform.ui.commerce.CartStateHolder
import uqu.drawbridge.platform.ui.commerce.OrdersStateHolder
import uqu.drawbridge.platform.ui.components.BackHandler
import uqu.drawbridge.platform.ui.components.ErrorStateCard
import uqu.drawbridge.platform.ui.components.LoadingStateCard
import uqu.drawbridge.platform.ui.components.MainBottomBar
import uqu.drawbridge.platform.ui.engagement.NotificationsStateHolder
import uqu.drawbridge.platform.ui.engagement.ReportsStateHolder
import uqu.drawbridge.platform.ui.engagement.SettingsStateHolder
import uqu.drawbridge.platform.ui.engagement.SupportStateHolder
import uqu.drawbridge.platform.ui.model.AppDestination
import uqu.drawbridge.platform.ui.model.AuthScreen
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.model.moreDestinationsFor
import uqu.drawbridge.platform.ui.model.primaryTabsFor
import uqu.drawbridge.platform.ui.marketplace.MarketplaceStateHolder
import uqu.drawbridge.platform.ui.marketplace.ProductDetailStateHolder
import uqu.drawbridge.platform.ui.marketplace.WishlistStateHolder
import uqu.drawbridge.platform.ui.operations.InventoryStateHolder
import uqu.drawbridge.platform.ui.operations.PosStateHolder
import uqu.drawbridge.platform.ui.operations.ProductManagementStateHolder
import uqu.drawbridge.platform.ui.platform.rememberPlatformServices
import uqu.drawbridge.platform.ui.screens.CartMainScreen
import uqu.drawbridge.platform.ui.screens.CheckoutMainScreen
import uqu.drawbridge.platform.ui.screens.DashboardMainScreen
import uqu.drawbridge.platform.ui.screens.FeaturePlaceholderMainScreen
import uqu.drawbridge.platform.ui.screens.InventoryDetailMainScreen
import uqu.drawbridge.platform.ui.screens.InventoryMainScreen
import uqu.drawbridge.platform.ui.screens.LoginAuthScreen
import uqu.drawbridge.platform.ui.screens.MarketplaceMainScreen
import uqu.drawbridge.platform.ui.screens.MoreDestinationScreen
import uqu.drawbridge.platform.ui.screens.MoreMainScreen
import uqu.drawbridge.platform.ui.screens.NotificationsMainScreen
import uqu.drawbridge.platform.ui.screens.OrderDetailMainScreen
import uqu.drawbridge.platform.ui.screens.OrdersMainScreen
import uqu.drawbridge.platform.ui.screens.PosMainScreen
import uqu.drawbridge.platform.ui.screens.ProductFormMainScreen
import uqu.drawbridge.platform.ui.screens.ProductManagementMainScreen
import uqu.drawbridge.platform.ui.screens.ProductDetailMainScreen
import uqu.drawbridge.platform.ui.screens.ReportsMainScreen
import uqu.drawbridge.platform.ui.screens.SettingsMainScreen
import uqu.drawbridge.platform.ui.screens.SignupAuthScreen
import uqu.drawbridge.platform.ui.screens.SplashAuthScreen
import uqu.drawbridge.platform.ui.screens.SupportMainScreen
import uqu.drawbridge.platform.ui.screens.WelcomeAuthScreen
import uqu.drawbridge.platform.ui.screens.WishlistMainScreen
import uqu.drawbridge.platform.ui.theme.AppBackground
import uqu.drawbridge.platform.ui.theme.DrawbridgeTheme

@Composable
@Preview
fun App() {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val platformServices = rememberPlatformServices()
    val sessionManager = remember(platformServices.secureTokenStorage) {
        AuthSessionManager(platformServices.secureTokenStorage)
    }

    var authScreen by remember { mutableStateOf(AuthScreen.Welcome) }
    var activeMoreDestination by remember { mutableStateOf<AppDestination?>(null) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var selectedInventoryItemId by remember { mutableStateOf<String?>(null) }
    var isCheckoutOpen by remember { mutableStateOf(false) }
    var isProductFormOpen by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(true) }

    val authState = sessionManager.state
    val authenticatedState = authState as? AuthSessionState.Authenticated
    val currentSession = authenticatedState?.session
    val dashboardSummary = authenticatedState?.dashboardSummary
    val tabs = remember(currentSession?.user?.role) {
        currentSession?.user?.role?.let(::primaryTabsFor).orEmpty()
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size.coerceAtLeast(1) })

    LaunchedEffect(Unit) {
        sessionManager.restoreSession()
    }

    LaunchedEffect(currentSession?.user?.role) {
        activeMoreDestination = null
        selectedProductId = null
        selectedOrderId = null
        selectedInventoryItemId = null
        isCheckoutOpen = false
        isProductFormOpen = false
        if (currentSession != null && pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    LaunchedEffect(currentSession?.user?.id) {
        if (currentSession != null && dashboardSummary == null) {
            isBusy = true
            sessionManager.setDashboardSummary(fetchDashboardSummary(sessionManager.api, currentSession))
            isBusy = false
        }
    }

    DrawbridgeTheme(darkTheme = darkTheme) {
        AppBackground()

        BackHandler(enabled = currentSession == null && authScreen != AuthScreen.Welcome) {
            authScreen = AuthScreen.Welcome
        }

        BackHandler(enabled = currentSession != null && selectedProductId != null) {
            selectedProductId = null
        }

        BackHandler(enabled = currentSession != null && selectedProductId == null && selectedOrderId != null) {
            selectedOrderId = null
        }

        BackHandler(enabled = currentSession != null && selectedProductId == null && selectedOrderId == null && selectedInventoryItemId != null) {
            selectedInventoryItemId = null
        }

        BackHandler(enabled = currentSession != null && selectedProductId == null && selectedOrderId == null && selectedInventoryItemId == null && isProductFormOpen) {
            isProductFormOpen = false
        }

        BackHandler(enabled = currentSession != null && selectedProductId == null && selectedOrderId == null && selectedInventoryItemId == null && !isProductFormOpen && isCheckoutOpen) {
            isCheckoutOpen = false
        }

        BackHandler(enabled = currentSession != null && selectedProductId == null && selectedOrderId == null && selectedInventoryItemId == null && !isProductFormOpen && !isCheckoutOpen && activeMoreDestination != null) {
            activeMoreDestination = null
        }

        BackHandler(enabled = currentSession != null && selectedProductId == null && selectedOrderId == null && selectedInventoryItemId == null && !isProductFormOpen && !isCheckoutOpen && activeMoreDestination == null && pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding(),
                )
            },
            bottomBar = {
                if (currentSession != null && tabs.isNotEmpty()) {
                    val selectedTabIndex = if (pagerState.isScrollInProgress) {
                        pagerState.targetPage
                    } else {
                        pagerState.currentPage
                    }
                    val currentTab = tabs.getOrElse(selectedTabIndex) { tabs.first() }
                    MainBottomBar(
                        tabs = tabs,
                        currentTab = currentTab,
                        onSelectTab = { tab ->
                            activeMoreDestination = null
                            selectedProductId = null
                            selectedOrderId = null
                            selectedInventoryItemId = null
                            isCheckoutOpen = false
                            isProductFormOpen = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tabs.indexOf(tab))
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                if (currentSession == null) {
                    AuthHost(
                        authState = authState,
                        authScreen = authScreen,
                        onAuthScreenChange = {
                            sessionManager.clearAuthMessage()
                            authScreen = it
                        },
                        sessionManager = sessionManager,
                        snackbarHostState = snackbarHostState,
                    )
                } else {
                    MainHost(
                        session = currentSession,
                        tabs = tabs,
                        pagerState = pagerState,
                        dashboardSummary = dashboardSummary,
                        activeMoreDestination = activeMoreDestination,
                        selectedProductId = selectedProductId,
                        selectedOrderId = selectedOrderId,
                        selectedInventoryItemId = selectedInventoryItemId,
                        isCheckoutOpen = isCheckoutOpen,
                        isProductFormOpen = isProductFormOpen,
                        onActiveMoreDestinationChange = {
                            selectedProductId = null
                            selectedOrderId = null
                            selectedInventoryItemId = null
                            isCheckoutOpen = false
                            isProductFormOpen = false
                            activeMoreDestination = it
                        },
                        onOpenProduct = { productId ->
                            selectedOrderId = null
                            selectedInventoryItemId = null
                            isCheckoutOpen = false
                            isProductFormOpen = false
                            selectedProductId = productId
                        },
                        onCloseProduct = { selectedProductId = null },
                        onOpenOrder = { orderId ->
                            selectedProductId = null
                            selectedInventoryItemId = null
                            isCheckoutOpen = false
                            isProductFormOpen = false
                            selectedOrderId = orderId
                        },
                        onCloseOrder = { selectedOrderId = null },
                        onOpenInventoryItem = { itemId ->
                            selectedProductId = null
                            selectedOrderId = null
                            isCheckoutOpen = false
                            isProductFormOpen = false
                            selectedInventoryItemId = itemId
                        },
                        onCloseInventoryItem = { selectedInventoryItemId = null },
                        onCheckoutOpenChange = { isCheckoutOpen = it },
                        onProductFormOpenChange = { isProductFormOpen = it },
                        sessionManager = sessionManager,
                        snackbarHostState = snackbarHostState,
                        onBusyChange = { isBusy = it },
                        bottomContentPadding = innerPadding.calculateBottomPadding(),
                        onLogout = {
                            coroutineScope.launch {
                                sessionManager.logout()
                            }
                            activeMoreDestination = null
                            selectedProductId = null
                            selectedOrderId = null
                            selectedInventoryItemId = null
                            isCheckoutOpen = false
                            isProductFormOpen = false
                            authScreen = AuthScreen.Welcome
                        },
                    )
                }

                AnimatedVisibility(
                    visible = isBusy || authState is AuthSessionState.Loading || authState is AuthSessionState.RestoringSession,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AuthHost(
    authState: AuthSessionState,
    authScreen: AuthScreen,
    onAuthScreenChange: (AuthScreen) -> Unit,
    sessionManager: AuthSessionManager,
    snackbarHostState: SnackbarHostState,
) {
    val coroutineScope = rememberCoroutineScope()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    AnimatedContent(
        targetState = showSplash,
        transitionSpec = {
            fadeIn().togetherWith(fadeOut()).using(SizeTransform(clip = false))
        },
        modifier = Modifier.fillMaxSize(),
    ) { splashVisible ->
        if (splashVisible) {
            SplashAuthScreen(restoringSession = authState is AuthSessionState.RestoringSession)
        } else if (authState is AuthSessionState.RestoringSession) {
            PreAuthScrollColumn {
                LoadingStateCard(
                    title = "Restoring session",
                    message = "Checking your secure Drawbridge session.",
                )
            }
        } else {
            AnimatedContent(
                targetState = authScreen,
                transitionSpec = {
                    if (initialState != AuthScreen.Welcome && targetState != AuthScreen.Welcome) {
                        fadeIn().togetherWith(fadeOut()).using(SizeTransform(clip = false))
                    } else {
                        val enter = if (targetState.ordinal > initialState.ordinal) slideInHorizontally { it } else slideInHorizontally { -it }
                        val exit = if (targetState.ordinal > initialState.ordinal) slideOutHorizontally { -it } else slideOutHorizontally { it }
                        (enter + fadeIn()).togetherWith(exit + fadeOut()).using(SizeTransform(clip = false))
                    }
                },
                modifier = Modifier.fillMaxSize(),
            ) { targetAuthScreen ->
                val inlineAuthMessage = when (authState) {
                    is AuthSessionState.Error -> authState.message
                    is AuthSessionState.SessionExpired -> authState.message
                    else -> null
                }

                when (targetAuthScreen) {
                    AuthScreen.Welcome -> {
                        PreAuthScreenBox {
                            Box(modifier = Modifier.fillMaxSize()) {
                                WelcomeAuthScreen(
                                    onGoToLogin = { onAuthScreenChange(AuthScreen.Login) },
                                    onGoToSignup = { onAuthScreenChange(AuthScreen.Signup) },
                                )
                                AnimatedVisibility(
                                    visible = authState is AuthSessionState.SessionExpired ||
                                        authState is AuthSessionState.Error,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically(),
                                    modifier = Modifier.align(Alignment.TopCenter),
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        AuthStateMessage(
                                            authState = authState,
                                            onDismiss = { sessionManager.clearAuthMessage() },
                                            onRetryRestore = {
                                                coroutineScope.launch {
                                                    sessionManager.restoreSession()
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AuthScreen.Login -> {
                        LoginAuthScreen(
                            onSubmit = { email, password, rememberMe ->
                                coroutineScope.launch {
                                    val result = sessionManager.login(email, password, rememberMe)
                                    if (!result.success) {
                                        snackbarHostState.showSnackbar(result.message ?: "Login failed")
                                    }
                                }
                            },
                            onGoToSignup = { onAuthScreenChange(AuthScreen.Signup) },
                            onBack = { onAuthScreenChange(AuthScreen.Welcome) },
                            isLoading = authState is AuthSessionState.Loading,
                            authMessage = inlineAuthMessage,
                        )
                    }

                    AuthScreen.Signup -> {
                        SignupAuthScreen(
                            onSubmit = { form ->
                                if (form.password != form.confirmPassword) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Passwords do not match")
                                    }
                                    return@SignupAuthScreen
                                }

                                coroutineScope.launch {
                                    val request = RegisterRequest(
                                        email = form.businessEmail,
                                        password = form.password,
                                        phoneNumber = form.phoneNumber,
                                        role = form.role,
                                        businessName = form.companyName,
                                        commercialRegistrationNumber = form.commercialRegistration,
                                        repName = form.repName,
                                        repJobTitle = form.repJobTitle,
                                        repPhoneNumber = form.repPhone,
                                        repEmail = form.repEmail,
                                        addresses = arrayOf(
                                            AddressDto(
                                                id = null,
                                                street = form.street,
                                                city = form.city,
                                                state = form.state,
                                                zipCode = form.zipCode,
                                                country = form.country,
                                            ),
                                        ),
                                    )

                                    val result = sessionManager.register(request)
                                    if (!result.success) {
                                        snackbarHostState.showSnackbar(result.message ?: "Registration failed")
                                    }
                                }
                            },
                            onGoToLogin = { onAuthScreenChange(AuthScreen.Login) },
                            onBack = { onAuthScreenChange(AuthScreen.Welcome) },
                            isLoading = authState is AuthSessionState.Loading,
                            authMessage = inlineAuthMessage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthStateMessage(
    authState: AuthSessionState,
    onDismiss: () -> Unit,
    onRetryRestore: () -> Unit,
) {
    when (authState) {
        is AuthSessionState.Loading -> LoadingStateCard(
            title = authState.message,
            message = "This will only take a moment.",
        )

        is AuthSessionState.SessionExpired -> ErrorStateCard(
            title = "Session expired",
            message = authState.message,
            actionText = "Dismiss",
            onAction = onDismiss,
        )

        is AuthSessionState.Error -> ErrorStateCard(
            title = "We could not continue",
            message = authState.message,
            actionText = if (authState.canRetryRestore) "Try again" else "Dismiss",
            onAction = if (authState.canRetryRestore) onRetryRestore else onDismiss,
        )

        else -> Unit
    }
}

@Composable
private fun MainHost(
    session: SessionState,
    tabs: List<uqu.drawbridge.platform.ui.model.MainTab>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    dashboardSummary: DashboardSummary?,
    activeMoreDestination: AppDestination?,
    selectedProductId: String?,
    selectedOrderId: String?,
    selectedInventoryItemId: String?,
    isCheckoutOpen: Boolean,
    isProductFormOpen: Boolean,
    onActiveMoreDestinationChange: (AppDestination?) -> Unit,
    onOpenProduct: (String) -> Unit,
    onCloseProduct: () -> Unit,
    onOpenOrder: (String) -> Unit,
    onCloseOrder: () -> Unit,
    onOpenInventoryItem: (String) -> Unit,
    onCloseInventoryItem: () -> Unit,
    onCheckoutOpenChange: (Boolean) -> Unit,
    onProductFormOpenChange: (Boolean) -> Unit,
    sessionManager: AuthSessionManager,
    snackbarHostState: SnackbarHostState,
    onBusyChange: (Boolean) -> Unit,
    bottomContentPadding: Dp,
    onLogout: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val marketplaceStateHolder = remember(session.user.id) {
        MarketplaceStateHolder(sessionManager.api)
    }
    val wishlistStateHolder = remember(session.user.id, session.user.role) {
        WishlistStateHolder(sessionManager.api, session)
    }
    val cartStateHolder = remember(session.user.id, session.user.role) {
        CartStateHolder(sessionManager.api, session)
    }
    val ordersStateHolder = remember(session.user.id, session.user.role) {
        OrdersStateHolder(sessionManager.api, session)
    }
    val inventoryStateHolder = remember(session.user.id, session.user.role) {
        InventoryStateHolder(sessionManager.api, session)
    }
    val productManagementStateHolder = remember(session.user.id, session.user.role) {
        ProductManagementStateHolder(sessionManager.api, session)
    }
    val posStateHolder = remember(session.user.id, session.user.role) {
        PosStateHolder(sessionManager.api, session)
    }
    val reportsStateHolder = remember(session.user.id, session.user.role) {
        ReportsStateHolder(sessionManager.api, session)
    }
    val supportStateHolder = remember(session.user.id) {
        SupportStateHolder(sessionManager.api)
    }
    val notificationsStateHolder = remember(session.user.id) {
        NotificationsStateHolder(sessionManager.api, session)
    }
    val settingsStateHolder = remember(session.user.id) {
        SettingsStateHolder(sessionManager.api, session)
    }
    val productDetailStateHolder = remember(selectedProductId) {
        ProductDetailStateHolder(sessionManager.api)
    }
    val showMessage: (String) -> Unit = { message ->
        coroutineScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    val openTab: (AppDestination) -> Unit = { destination ->
        val index = tabs.indexOfFirst { it.destination == destination }
        if (index >= 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        userScrollEnabled = activeMoreDestination == null &&
            selectedProductId == null &&
            selectedOrderId == null &&
            selectedInventoryItemId == null &&
            !isProductFormOpen &&
            !isCheckoutOpen,
    ) { page ->
        val currentTab = tabs.getOrElse(page) { tabs.first() }
        val routeKey = listOf(
            currentTab.destination.name,
            activeMoreDestination?.name.orEmpty(),
            selectedProductId.orEmpty(),
            selectedOrderId.orEmpty(),
            selectedInventoryItemId.orEmpty(),
            isCheckoutOpen.toString(),
            isProductFormOpen.toString(),
        ).joinToString(":")
        val isRootDashboard = currentTab.destination == AppDestination.Home &&
            activeMoreDestination == null &&
            selectedProductId == null &&
            selectedOrderId == null &&
            selectedInventoryItemId == null &&
            !isProductFormOpen &&
            !isCheckoutOpen

        MobileScrollColumn(
            routeKey = routeKey,
            compactTop = isRootDashboard,
            bottomContentPadding = bottomContentPadding,
        ) {
            if (selectedProductId != null) {
                ProductDetailMainScreen(
                    productId = selectedProductId,
                    detailStateHolder = productDetailStateHolder,
                    wishlistStateHolder = wishlistStateHolder,
                    cartStateHolder = cartStateHolder,
                    onBack = onCloseProduct,
                    onShowMessage = showMessage,
                )
                return@MobileScrollColumn
            }

            if (selectedOrderId != null) {
                OrderDetailMainScreen(
                    orderId = selectedOrderId,
                    ordersStateHolder = ordersStateHolder,
                    session = session,
                    onBack = onCloseOrder,
                )
                return@MobileScrollColumn
            }

            if (selectedInventoryItemId != null) {
                InventoryDetailMainScreen(
                    itemId = selectedInventoryItemId,
                    inventoryStateHolder = inventoryStateHolder,
                    onBack = onCloseInventoryItem,
                    onShowMessage = showMessage,
                )
                return@MobileScrollColumn
            }

            if (isProductFormOpen) {
                ProductFormMainScreen(
                    productManagementStateHolder = productManagementStateHolder,
                    onBack = { onProductFormOpenChange(false) },
                    onShowMessage = showMessage,
                )
                return@MobileScrollColumn
            }

            when (currentTab.destination) {
                AppDestination.Home -> {
                    DashboardMainScreen(
                        dashboardSummary = dashboardSummary,
                        session = session,
                        ordersStateHolder = ordersStateHolder,
                        notificationsStateHolder = notificationsStateHolder,
                        onRefresh = {
                            coroutineScope.launch {
                                onBusyChange(true)
                                try {
                                    ordersStateHolder.refresh()
                                    notificationsStateHolder.load()
                                    sessionManager.setDashboardSummary(fetchDashboardSummary(sessionManager.api, session))
                                } finally {
                                    onBusyChange(false)
                                }
                            }
                        },
                        onOpenOrders = { openTab(AppDestination.Orders) },
                        onOpenOrder = onOpenOrder,
                    )
                }

                AppDestination.Marketplace -> {
                    MarketplaceMainScreen(
                        marketplaceStateHolder = marketplaceStateHolder,
                        wishlistStateHolder = wishlistStateHolder,
                        onOpenProduct = onOpenProduct,
                        onShowMessage = showMessage,
                    )
                }

                AppDestination.Cart -> {
                    if (isCheckoutOpen) {
                        CheckoutMainScreen(
                            cartStateHolder = cartStateHolder,
                            onBackToCart = { onCheckoutOpenChange(false) },
                            onViewOrders = {
                                onCheckoutOpenChange(false)
                                coroutineScope.launch {
                                    ordersStateHolder.refresh()
                                }
                                openTab(AppDestination.Orders)
                            },
                            onShowMessage = showMessage,
                        )
                    } else {
                        CartMainScreen(
                            cartStateHolder = cartStateHolder,
                            onOpenMarketplace = { openTab(AppDestination.Marketplace) },
                            onOpenCheckout = {
                                cartStateHolder.clearCheckoutResult()
                                onCheckoutOpenChange(true)
                            },
                            onShowMessage = showMessage,
                        )
                    }
                }

                AppDestination.Orders -> {
                    OrdersMainScreen(
                        ordersStateHolder = ordersStateHolder,
                        session = session,
                        onOpenOrder = onOpenOrder,
                    )
                }

                AppDestination.Inventory -> {
                    InventoryMainScreen(
                        inventoryStateHolder = inventoryStateHolder,
                        onOpenDetail = onOpenInventoryItem,
                    )
                }

                AppDestination.Products -> {
                    ProductManagementMainScreen(
                        productManagementStateHolder = productManagementStateHolder,
                        onCreateProduct = {
                            productManagementStateHolder.startCreate()
                            onProductFormOpenChange(true)
                        },
                        onEditProduct = { product ->
                            productManagementStateHolder.startEdit(product)
                            onProductFormOpenChange(true)
                        },
                        onShowMessage = showMessage,
                    )
                }

                AppDestination.More -> {
                    val selectedDestination = activeMoreDestination
                    if (selectedDestination == null) {
                        MoreMainScreen(
                            destinations = moreDestinationsFor(session.user.role),
                            onOpenDestination = onActiveMoreDestinationChange,
                            onLogout = onLogout,
                        )
                    } else {
                        MoreDestinationScreen(
                            destination = selectedDestination,
                            onBack = { onActiveMoreDestinationChange(null) },
                            onLogout = onLogout,
                            wishlistContent = {
                                WishlistMainScreen(
                                    wishlistStateHolder = wishlistStateHolder,
                                    onOpenProduct = onOpenProduct,
                                    onShowMessage = showMessage,
                                )
                            },
                            posContent = {
                                PosMainScreen(
                                    posStateHolder = posStateHolder,
                                    onShowMessage = showMessage,
                                )
                            },
                            reportsContent = {
                                ReportsMainScreen(
                                    reportsStateHolder = reportsStateHolder,
                                    role = session.user.role,
                                )
                            },
                            supportContent = {
                                SupportMainScreen(supportStateHolder = supportStateHolder)
                            },
                            notificationsContent = {
                                NotificationsMainScreen(notificationsStateHolder = notificationsStateHolder)
                            },
                            settingsContent = {
                                SettingsMainScreen(
                                    settingsStateHolder = settingsStateHolder,
                                    onLogout = onLogout,
                                )
                            },
                            inventoryContent = {
                                InventoryMainScreen(
                                    inventoryStateHolder = inventoryStateHolder,
                                    onOpenDetail = onOpenInventoryItem,
                                )
                            },
                        )
                    }
                }

                AppDestination.Settings -> {
                    SettingsMainScreen(
                        settingsStateHolder = settingsStateHolder,
                        onLogout = onLogout,
                    )
                }

                else -> {
                    FeaturePlaceholderMainScreen(destination = currentTab.destination)
                }
            }
        }
    }
}

@Composable
private fun MobileScrollColumn(
    routeKey: String,
    compactTop: Boolean = false,
    bottomContentPadding: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val topPadding: Dp = if (compactTop) 8.dp else 16.dp
    val bottomPadding = bottomContentPadding + 16.dp
    val scrollState = rememberScrollState()
    LaunchedEffect(routeKey) {
        scrollState.scrollTo(0)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun PreAuthScrollColumn(
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .preAuthBackground(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PreAuthScreenBox(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .preAuthBackground(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            content()
        }
    }
}

private fun Modifier.preAuthBackground(): Modifier {
    return background(
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF03111F),
                Color(0xFF0B1F33),
                Color(0xFF03111F),
            ),
        ),
    )
}

private suspend fun fetchDashboardSummary(
    api: MobileAuthApi,
    session: SessionState,
): DashboardSummary? {
    return runCatching {
        api.fetchDashboardSummary(
            userId = session.user.id,
            role = session.user.role,
        )
    }.getOrNull()
}
