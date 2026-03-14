package uqu.drawbridge.platform

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.ui.components.MainBottomBar
import uqu.drawbridge.platform.ui.components.BackHandler
import uqu.drawbridge.platform.ui.model.AuthScreen
import uqu.drawbridge.platform.ui.model.MainTab
import uqu.drawbridge.platform.ui.model.SessionState
import uqu.drawbridge.platform.ui.screens.*
import uqu.drawbridge.platform.ui.theme.AppBackground
import uqu.drawbridge.platform.ui.theme.DrawbridgeTheme

@Composable
@Preview
fun App() {
    val api = remember { MobileAuthApi() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var authScreen by remember { mutableStateOf(AuthScreen.Welcome) }
    var session by remember { mutableStateOf<SessionState?>(null) }
    var dashboardSummary by remember { mutableStateOf<DashboardSummary?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var darkTheme by remember { mutableStateOf(true) }

    val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })

    DrawbridgeTheme(darkTheme = darkTheme) {
        AppBackground()
        
        BackHandler(enabled = session == null && authScreen != AuthScreen.Welcome) {
            authScreen = AuthScreen.Welcome
        }

        BackHandler(enabled = session != null && pagerState.currentPage != 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(0)
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (session != null) {
                    val currentTab = MainTab.entries.getOrElse(pagerState.currentPage) { MainTab.Dashboard }
                    MainBottomBar(
                        currentTab = currentTab,
                        onSelectTab = { tab ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        },
                    )
                }
            },
            modifier = Modifier,
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (session == null) {
                    AnimatedContent(
                        targetState = authScreen,
                        transitionSpec = {
                            if (targetState.ordinal > initialState.ordinal) {
                                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                            } else {
                                (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { targetAuthScreen ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(top = 110.dp, bottom = 12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (targetAuthScreen) {
                                AuthScreen.Welcome -> {
                                    WelcomeAuthScreen(
                                        onGoToLogin = { authScreen = AuthScreen.Login },
                                        onGoToSignup = { authScreen = AuthScreen.Signup },
                                    )
                                }

                                AuthScreen.Login -> {
                                    LoginAuthScreen(
                                        onSubmit = { email, password, rememberMe ->
                                            coroutineScope.launch {
                                                isBusy = true
                                                runCatching { api.login(email, password, rememberMe) }
                                                    .onSuccess { newSession ->
                                                        session = SessionState(newSession.token, newSession.user)
                                                        dashboardSummary = runCatching {
                                                            api.fetchDashboardSummary(
                                                                userId = newSession.user.id,
                                                                role = newSession.user.role,
                                                                token = newSession.token,
                                                            )
                                                        }.getOrNull()
                                                        pagerState.scrollToPage(MainTab.Dashboard.ordinal)
                                                    }
                                                    .onFailure { err ->
                                                        snackbarHostState.showSnackbar(err.message ?: "Login failed")
                                                    }
                                                isBusy = false
                                            }
                                        },
                                        onGoToSignup = { authScreen = AuthScreen.Signup },
                                        onBack = { authScreen = AuthScreen.Welcome },
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
                                                isBusy = true
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
                                                    .onSuccess { newSession ->
                                                        session = SessionState(newSession.token, newSession.user)
                                                        dashboardSummary = runCatching {
                                                            api.fetchDashboardSummary(
                                                                userId = newSession.user.id,
                                                                role = newSession.user.role,
                                                                token = newSession.token,
                                                            )
                                                        }.getOrNull()
                                                        pagerState.scrollToPage(MainTab.Dashboard.ordinal)
                                                    }
                                                    .onFailure { err ->
                                                        snackbarHostState.showSnackbar(err.message ?: "Registration failed")
                                                    }
                                                isBusy = false
                                            }
                                        },
                                        onGoToLogin = { authScreen = AuthScreen.Login },
                                        onBack = { authScreen = AuthScreen.Welcome },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Top
                    ) { page ->
                        val currentTab = MainTab.entries.getOrElse(page) { MainTab.Dashboard }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(top = 110.dp, bottom = 12.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (currentTab) {
                                MainTab.Dashboard -> {
                                    DashboardMainScreen(
                                        dashboardSummary = dashboardSummary,
                                        onRefresh = {
                                            val currentSession = session ?: return@DashboardMainScreen
                                            coroutineScope.launch {
                                                isBusy = true
                                                dashboardSummary = runCatching {
                                                    api.fetchDashboardSummary(
                                                        userId = currentSession.user.id,
                                                        role = currentSession.user.role,
                                                        token = currentSession.token,
                                                    )
                                                }.getOrNull()
                                                isBusy = false
                                            }
                                        },
                                    )
                                }

                                MainTab.Account -> {
                                    AccountMainScreen(
                                        onLogout = {
                                            session = null
                                            dashboardSummary = null
                                            authScreen = AuthScreen.Welcome
                                        },
                                    )
                                }

                                MainTab.POS -> {
                                    PosMainScreen(
                                        onScan = { gtin, onResult ->
                                            val currentSession = session ?: return@PosMainScreen
                                            coroutineScope.launch {
                                                isBusy = true
                                                val result = runCatching {
                                                    api.scanBarcode(
                                                        retailerId = currentSession.user.id,
                                                        gtin = gtin,
                                                        token = currentSession.token,
                                                    )
                                                }.getOrElse { err ->
                                                    PosScanResponse(
                                                        productName = "",
                                                        newStock = 0,
                                                        message = err.message ?: "Scan failed",
                                                    )
                                                }
                                                onResult(result)
                                                isBusy = false
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isBusy,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
