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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.ui.components.BackHandler
import uqu.drawbridge.platform.ui.components.MainBottomBar
import uqu.drawbridge.platform.ui.model.AppDestination
import uqu.drawbridge.platform.ui.model.AuthScreen
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.model.moreDestinationsFor
import uqu.drawbridge.platform.ui.model.primaryTabsFor
import uqu.drawbridge.platform.ui.platform.rememberPlatformServices
import uqu.drawbridge.platform.ui.screens.AccountMainScreen
import uqu.drawbridge.platform.ui.screens.DashboardMainScreen
import uqu.drawbridge.platform.ui.screens.FeaturePlaceholderMainScreen
import uqu.drawbridge.platform.ui.screens.LoginAuthScreen
import uqu.drawbridge.platform.ui.screens.MoreDestinationScreen
import uqu.drawbridge.platform.ui.screens.MoreMainScreen
import uqu.drawbridge.platform.ui.screens.PosMainScreen
import uqu.drawbridge.platform.ui.screens.SignupAuthScreen
import uqu.drawbridge.platform.ui.screens.WelcomeAuthScreen
import uqu.drawbridge.platform.ui.theme.AppBackground
import uqu.drawbridge.platform.ui.theme.DrawbridgeTheme

@Composable
@Preview
fun App() {
    val api = remember { MobileAuthApi() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val platformServices = rememberPlatformServices()

    var authScreen by remember { mutableStateOf(AuthScreen.Welcome) }
    var session by remember { mutableStateOf<SessionState?>(null) }
    var dashboardSummary by remember { mutableStateOf<DashboardSummary?>(null) }
    var activeMoreDestination by remember { mutableStateOf<AppDestination?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(true) }

    val currentSession = session
    val tabs = remember(currentSession?.user?.role) {
        currentSession?.user?.role?.let(::primaryTabsFor).orEmpty()
    }
    val pagerState = rememberPagerState(pageCount = { tabs.size.coerceAtLeast(1) })

    LaunchedEffect(currentSession?.user?.role) {
        activeMoreDestination = null
        if (currentSession != null && pagerState.currentPage >= tabs.size) {
            pagerState.scrollToPage(0)
        }
    }

    DrawbridgeTheme(darkTheme = darkTheme) {
        AppBackground()

        BackHandler(enabled = currentSession == null && authScreen != AuthScreen.Welcome) {
            authScreen = AuthScreen.Welcome
        }

        BackHandler(enabled = currentSession != null && activeMoreDestination != null) {
            activeMoreDestination = null
        }

        BackHandler(enabled = currentSession != null && activeMoreDestination == null && pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (currentSession != null && tabs.isNotEmpty()) {
                    val currentTab = tabs.getOrElse(pagerState.currentPage) { tabs.first() }
                    MainBottomBar(
                        tabs = tabs,
                        currentTab = currentTab,
                        onSelectTab = { tab ->
                            activeMoreDestination = null
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
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (currentSession == null) {
                    AuthHost(
                        authScreen = authScreen,
                        onAuthScreenChange = { authScreen = it },
                        api = api,
                        snackbarHostState = snackbarHostState,
                        onBusyChange = { isBusy = it },
                        onAuthenticated = { newSession, summary ->
                            session = newSession
                            dashboardSummary = summary
                            activeMoreDestination = null
                            coroutineScope.launch {
                                pagerState.scrollToPage(0)
                            }
                        },
                    )
                } else {
                    MainHost(
                        session = currentSession,
                        tabs = tabs,
                        pagerState = pagerState,
                        dashboardSummary = dashboardSummary,
                        activeMoreDestination = activeMoreDestination,
                        onActiveMoreDestinationChange = { activeMoreDestination = it },
                        onDashboardSummaryChange = { dashboardSummary = it },
                        api = api,
                        onBusyChange = { isBusy = it },
                        onLogout = {
                            coroutineScope.launch {
                                platformServices.secureTokenStorage.clearToken()
                            }
                            session = null
                            dashboardSummary = null
                            activeMoreDestination = null
                            authScreen = AuthScreen.Welcome
                        },
                    )
                }

                AnimatedVisibility(
                    visible = isBusy,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AuthHost(
    authScreen: AuthScreen,
    onAuthScreenChange: (AuthScreen) -> Unit,
    api: MobileAuthApi,
    snackbarHostState: SnackbarHostState,
    onBusyChange: (Boolean) -> Unit,
    onAuthenticated: (SessionState, DashboardSummary?) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    AnimatedContent(
        targetState = authScreen,
        transitionSpec = {
            val enter = if (targetState.ordinal > initialState.ordinal) slideInHorizontally { it } else slideInHorizontally { -it }
            val exit = if (targetState.ordinal > initialState.ordinal) slideOutHorizontally { -it } else slideOutHorizontally { it }
            (enter + fadeIn()).togetherWith(exit + fadeOut()).using(SizeTransform(clip = false))
        },
        modifier = Modifier.fillMaxSize(),
    ) { targetAuthScreen ->
        MobileScrollColumn {
            when (targetAuthScreen) {
                AuthScreen.Welcome -> {
                    WelcomeAuthScreen(
                        onGoToLogin = { onAuthScreenChange(AuthScreen.Login) },
                        onGoToSignup = { onAuthScreenChange(AuthScreen.Signup) },
                    )
                }

                AuthScreen.Login -> {
                    LoginAuthScreen(
                        onSubmit = { email, password, rememberMe ->
                            coroutineScope.launch {
                                onBusyChange(true)
                                runCatching { api.login(email, password, rememberMe) }
                                    .onSuccess { mobileSession ->
                                        val newSession = SessionState(mobileSession.token, mobileSession.user)
                                        val summary = fetchDashboardSummary(api, newSession)
                                        onAuthenticated(newSession, summary)
                                    }
                                    .onFailure { err ->
                                        snackbarHostState.showSnackbar(err.message ?: "Login failed")
                                    }
                                onBusyChange(false)
                            }
                        },
                        onGoToSignup = { onAuthScreenChange(AuthScreen.Signup) },
                        onBack = { onAuthScreenChange(AuthScreen.Welcome) },
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
                                onBusyChange(true)
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

                                runCatching { api.register(request) }
                                    .onSuccess { mobileSession ->
                                        val newSession = SessionState(mobileSession.token, mobileSession.user)
                                        val summary = fetchDashboardSummary(api, newSession)
                                        onAuthenticated(newSession, summary)
                                    }
                                    .onFailure { err ->
                                        snackbarHostState.showSnackbar(err.message ?: "Registration failed")
                                    }
                                onBusyChange(false)
                            }
                        },
                        onGoToLogin = { onAuthScreenChange(AuthScreen.Login) },
                        onBack = { onAuthScreenChange(AuthScreen.Welcome) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainHost(
    session: SessionState,
    tabs: List<uqu.drawbridge.platform.ui.model.MainTab>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    dashboardSummary: DashboardSummary?,
    activeMoreDestination: AppDestination?,
    onActiveMoreDestinationChange: (AppDestination?) -> Unit,
    onDashboardSummaryChange: (DashboardSummary?) -> Unit,
    api: MobileAuthApi,
    onBusyChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Top,
        userScrollEnabled = activeMoreDestination == null,
    ) { page ->
        val currentTab = tabs.getOrElse(page) { tabs.first() }
        MobileScrollColumn {
            when (currentTab.destination) {
                AppDestination.Home -> {
                    DashboardMainScreen(
                        dashboardSummary = dashboardSummary,
                        onRefresh = {
                            coroutineScope.launch {
                                onBusyChange(true)
                                onDashboardSummaryChange(fetchDashboardSummary(api, session))
                                onBusyChange(false)
                            }
                        },
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
                            posContent = {
                                PosMainScreen(
                                    onScan = { gtin, onResult ->
                                        coroutineScope.launch {
                                            onBusyChange(true)
                                            val result = runCatching {
                                                api.scanBarcode(
                                                    retailerId = session.user.id,
                                                    gtin = gtin,
                                                    token = session.token,
                                                )
                                            }.getOrElse { err ->
                                                PosScanResponse(
                                                    productName = "",
                                                    newStock = 0,
                                                    message = err.message ?: "Scan failed",
                                                )
                                            }
                                            onResult(result)
                                            onBusyChange(false)
                                        }
                                    },
                                )
                            },
                        )
                    }
                }

                AppDestination.Settings -> {
                    AccountMainScreen(onLogout = onLogout)
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
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
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
            token = session.token,
        )
    }.getOrNull()
}
