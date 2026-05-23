package uqu.drawbridge.platform.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uqu.drawbridge.platform.UserRole
import uqu.drawbridge.platform.ui.components.AppTextField
import uqu.drawbridge.platform.ui.components.ServerErrorCard
import uqu.drawbridge.platform.ui.common.ServerNotFoundMessage
import uqu.drawbridge.platform.ui.model.SignupFormState
import uqu.drawbridge.platform.ui.theme.Primary500

private val AuthNavy = Color(0xFF03111F)
private val AuthNavySoft = Color(0xFF0B1F33)
private val AuthBlue = Color(0xFF0D3B66)
private val AuthEmerald = Color(0xFF10B981)
private val AuthMint = Color(0xFF8EF3C5)
private val AuthText = Color(0xFFF8FAFC)
private val AuthMuted = Color(0xFFA8B7C7)
private val AuthLine = Color(0xFF2B4A5F)

@Composable
internal fun SplashAuthScreen(
    restoringSession: Boolean,
) {
    PreAuthSurface(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = 0.dp,
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "splash-progress")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0.32f,
            targetValue = 0.92f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "splash-progress-width",
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DrawbridgeMark(scale = 1.08f)
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Welcome to Drawbridge",
                style = MaterialTheme.typography.titleLarge,
                color = AuthText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (restoringSession) "Checking your secure workspace." else "Commerce operations, connected.",
                style = MaterialTheme.typography.bodyLarge,
                color = AuthMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(AuthLine.copy(alpha = 0.6f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AuthEmerald),
                )
            }
        }
    }
}

@Composable
internal fun WelcomeAuthScreen(
    onGoToLogin: () -> Unit,
    onGoToSignup: () -> Unit,
) {
    val slides = remember { onboardingSlides() }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastSlide = pagerState.currentPage == slides.lastIndex

    PreAuthSurface(
        modifier = Modifier.fillMaxSize(),
        drawBackground = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawbridgeMark(scale = 0.78f)
                TextButton(onClick = onGoToLogin) {
                    Text("Skip to sign in", color = AuthMint, fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    OnboardingSlideCard(slide = slides[page])
                }
            }

            OnboardingControls(
                pageCount = slides.size,
                selectedIndex = pagerState.currentPage,
                isLastSlide = isLastSlide,
                onBack = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNext = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(slides.lastIndex))
                    }
                },
                onGoToLogin = onGoToLogin,
                onGoToSignup = onGoToSignup,
            )
        }
    }
}

@Composable
internal fun LoginAuthScreen(
    onSubmit: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onGoToSignup: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    authMessage: String? = null,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    AuthFormScaffold(
        eyebrow = "Secure sign in",
        title = "Welcome back.",
        subtitle = "Access the mobile workspace for marketplace, orders, inventory, and support.",
        onBack = onBack,
    ) {
        AuthGlassCard {
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = "Business email",
                keyboardType = KeyboardType.Email,
            )
            AppTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                isPassword = true,
            )
            FilterChip(
                selected = rememberMe,
                onClick = { rememberMe = !rememberMe },
                label = { Text("Keep me signed in") },
                enabled = !isLoading,
            )
            AuthInlineMessage(message = authMessage)
            AuthSubmitButton(
                text = "Sign in",
                loadingText = "Signing in...",
                onClick = { onSubmit(email.trim(), password, rememberMe) },
                isLoading = isLoading,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New to Drawbridge?", style = MaterialTheme.typography.bodyMedium, color = AuthMuted)
                TextButton(onClick = onGoToSignup, enabled = !isLoading) {
                    Text("Create account", color = AuthMint, fontWeight = FontWeight.Bold)
                }
            }
        }

        DemoAccountsCard(
            enabled = !isLoading,
            onRetailer = {
                email = "retailer@test.com"
                password = "password"
                rememberMe = true
                onSubmit("retailer@test.com", "password", true)
            },
            onWholesaler = {
                email = "wholesaler@test.com"
                password = "password"
                rememberMe = true
                onSubmit("wholesaler@test.com", "password", true)
            },
        )
    }
}

@Composable
internal fun SignupAuthScreen(
    onSubmit: (SignupFormState) -> Unit,
    onGoToLogin: () -> Unit,
    onBack: () -> Unit,
    isLoading: Boolean = false,
    authMessage: String? = null,
) {
    var form by remember { mutableStateOf(SignupFormState()) }

    AuthFormScaffold(
        eyebrow = "Create account",
        title = "Create account.",
        subtitle = "Set up the business profile Drawbridge needs for secure trade.",
        onBack = onBack,
        compactHeader = true,
    ) {
        SignupSectionCard(
            title = "Business account",
            subtitle = "Choose how this workspace trades.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = form.role == UserRole.RETAILER,
                    onClick = { form = form.copy(role = UserRole.RETAILER) },
                    label = { Text("Retailer") },
                    enabled = !isLoading,
                )
                FilterChip(
                    selected = form.role == UserRole.WHOLESALER,
                    onClick = { form = form.copy(role = UserRole.WHOLESALER) },
                    label = { Text("Wholesaler") },
                    enabled = !isLoading,
                )
            }
            AppTextField(value = form.companyName, onValueChange = { form = form.copy(companyName = it) }, label = "Company")
            AppTextField(value = form.businessEmail, onValueChange = { form = form.copy(businessEmail = it) }, label = "Business email", keyboardType = KeyboardType.Email)
            AppTextField(value = form.phoneNumber, onValueChange = { form = form.copy(phoneNumber = it) }, label = "Phone", keyboardType = KeyboardType.Phone)
            AppTextField(value = form.commercialRegistration, onValueChange = { form = form.copy(commercialRegistration = it) }, label = "Commercial registration")
        }

        SignupSectionCard(
            title = "Representative",
            subtitle = "The person your partners can recognize.",
        ) {
            AppTextField(value = form.repName, onValueChange = { form = form.copy(repName = it) }, label = "Name")
            AppTextField(value = form.repJobTitle, onValueChange = { form = form.copy(repJobTitle = it) }, label = "Job title")
            AppTextField(value = form.repPhone, onValueChange = { form = form.copy(repPhone = it) }, label = "Phone", keyboardType = KeyboardType.Phone)
            AppTextField(value = form.repEmail, onValueChange = { form = form.copy(repEmail = it) }, label = "Email", keyboardType = KeyboardType.Email)
        }

        SignupSectionCard(
            title = "Business address",
            subtitle = "Used for account setup and order paperwork.",
        ) {
            AppTextField(value = form.street, onValueChange = { form = form.copy(street = it) }, label = "Street")
            AppTextField(value = form.city, onValueChange = { form = form.copy(city = it) }, label = "City")
            AppTextField(value = form.state, onValueChange = { form = form.copy(state = it) }, label = "State")
            AppTextField(value = form.zipCode, onValueChange = { form = form.copy(zipCode = it) }, label = "Zip")
            AppTextField(value = form.country, onValueChange = { form = form.copy(country = it) }, label = "Country")
        }

        SignupSectionCard(
            title = "Security",
            subtitle = "Protect access to this workspace.",
        ) {
            AppTextField(value = form.password, onValueChange = { form = form.copy(password = it) }, label = "Password", isPassword = true)
            AppTextField(value = form.confirmPassword, onValueChange = { form = form.copy(confirmPassword = it) }, label = "Confirm password", isPassword = true)
        }

        AuthGlassCard(compact = true) {
            AuthInlineMessage(message = authMessage)
            AuthSubmitButton(
                text = "Create account",
                loadingText = "Creating account...",
                onClick = { onSubmit(form) },
                isLoading = isLoading,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = AuthMuted)
                TextButton(onClick = onGoToLogin, enabled = !isLoading) {
                    Text("Sign in", color = AuthMint, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AuthFormScaffold(
    eyebrow: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    compactHeader: Boolean = false,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val bottomInset = with(density) { WindowInsets.safeDrawing.getBottom(this).toDp() }
    val scrollState = rememberScrollState()

    PreAuthSurface(
        modifier = Modifier
            .fillMaxSize(),
        drawBackground = true,
        contentPadding = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = contentPadding)
                .padding(top = 16.dp, bottom = bottomInset + 40.dp),
            verticalArrangement = Arrangement.spacedBy(if (compactHeader) 12.dp else 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DrawbridgeMark(scale = 0.7f)
                TextButton(onClick = onBack) {
                    Text("Back", color = AuthMint, fontWeight = FontWeight.Bold)
                }
            }
            AuthPill(eyebrow)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = if (compactHeader) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                    color = AuthText,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = subtitle,
                    style = if (compactHeader) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = AuthMuted,
                )
            }
            content()
        }
    }
}

@Composable
private fun PreAuthSurface(
    modifier: Modifier = Modifier,
    drawBackground: Boolean = true,
    contentPadding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val surfaceModifier = if (drawBackground) {
        modifier.background(
            Brush.verticalGradient(
                colors = listOf(AuthNavy, AuthNavySoft, AuthNavy),
            ),
        )
    } else {
        modifier
    }
    Box(
        modifier = surfaceModifier,
    ) {
        if (drawBackground) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(220.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AuthBlue.copy(alpha = 0.42f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(190.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AuthEmerald.copy(alpha = 0.2f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun OnboardingControls(
    pageCount: Int,
    selectedIndex: Int,
    isLastSlide: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onGoToLogin: () -> Unit,
    onGoToSignup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(154.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageDots(
            count = pageCount,
            selectedIndex = selectedIndex,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        AnimatedContent(
            targetState = isLastSlide,
            transitionSpec = {
                fadeIn().togetherWith(fadeOut()).using(SizeTransform(clip = false))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { finalStep ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (finalStep) {
                    AuthPrimaryButton(text = "Sign in", onClick = onGoToLogin)
                    SecondaryAuthButton(text = "Create account", onClick = onGoToSignup)
                } else if (selectedIndex == 0) {
                    AuthPrimaryButton(text = "Next", onClick = onNext)
                    Spacer(modifier = Modifier.height(58.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SecondaryAuthButton(
                            text = "Back",
                            onClick = onBack,
                            modifier = Modifier.weight(0.42f),
                        )
                        AuthPrimaryButton(
                            text = "Next",
                            onClick = onNext,
                            modifier = Modifier.weight(0.58f),
                        )
                    }
                    Spacer(modifier = Modifier.height(58.dp))
                }
            }
        }
    }
}

@Composable
private fun DrawbridgeMark(scale: Float = 1f) {
    val wordmarkStyle = if (scale < 0.8f) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((10 * scale).dp),
    ) {
        Box(
            modifier = Modifier
                .size((44 * scale).dp)
                .clip(RoundedCornerShape((8 * scale).dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AuthEmerald, Color(0xFF38BDF8)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((9 * scale).dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height((4 * scale).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF022C22).copy(alpha = 0.9f)),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.72f)
                        .height((3 * scale).dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.82f)),
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    listOf(12, 18, 12).forEach { height ->
                        Box(
                            modifier = Modifier
                                .width((5 * scale).dp)
                                .height((height * scale).dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF022C22).copy(alpha = 0.86f)),
                        )
                    }
                }
            }
        }
        Text(
            text = "Drawbridge",
            style = wordmarkStyle,
            color = AuthText,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun OnboardingSlideCard(slide: OnboardingSlide) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AuthGlassCard(compact = true) {
                OnboardingVisual(slide)
            }
            AuthPill(slide.kicker)
            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineMedium,
                color = AuthText,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = slide.body,
                style = MaterialTheme.typography.bodyMedium,
                color = AuthMuted,
            )
        }
    }
}

@Composable
private fun OnboardingVisual(slide: OnboardingSlide) {
    val cards = slide.cards
    when (slide.visualStyle) {
        OnboardingVisualStyle.Grid -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    MiniSignalCard(cards[0], modifier = Modifier.weight(1f))
                    MiniSignalCard(cards[1], modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    MiniSignalCard(cards[2], modifier = Modifier.weight(1f))
                    MiniSignalCard(cards[3], modifier = Modifier.weight(1f))
                }
            }
        }

        OnboardingVisualStyle.Hero -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniSignalCard(cards[0], modifier = Modifier.weight(1.05f), prominent = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(0.9f)) {
                    MiniSignalCard(cards[1], modifier = Modifier.weight(1f))
                    MiniSignalCard(cards[2], modifier = Modifier.weight(1f))
                }
            }
        }

        OnboardingVisualStyle.Control -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniSignalCard(cards[0], modifier = Modifier.weight(1.05f), prominent = true)
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiniSignalCard(cards[1], modifier = Modifier.weight(1f))
                    MiniSignalCard(cards[2], modifier = Modifier.weight(1f))
                    MiniSignalCard(cards[3], modifier = Modifier.weight(1f))
                }
            }
        }

        OnboardingVisualStyle.Start -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(154.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniSignalCard(cards[0], modifier = Modifier.weight(1f), prominent = true)
                MiniSignalCard(cards[1], modifier = Modifier.weight(1f), prominent = true)
            }
        }
    }
}

@Composable
private fun MiniSignalCard(
    item: OnboardingMiniCard,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        color = if (prominent) item.tint.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, if (prominent) item.tint.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(item.tint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.tint,
                    modifier = Modifier.size(17.dp),
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = AuthText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AuthGlassCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.075f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 13.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 11.dp else 14.dp),
            content = content,
        )
    }
}

@Composable
private fun SignupSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    AuthGlassCard(compact = true) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AuthText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AuthMuted,
            )
        }
        content()
    }
}

@Composable
private fun AuthPill(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = AuthEmerald.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, AuthEmerald.copy(alpha = 0.28f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = AuthMint,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PageDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val width by animateFloatAsState(if (index == selectedIndex) 26f else 8f)
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(width.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (index == selectedIndex) AuthEmerald else Color.White.copy(alpha = 0.24f)),
                )
            }
        }
    }
}

@Composable
private fun SecondaryAuthButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, AuthMint.copy(alpha = if (enabled) 0.5f else 0.18f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = AuthMint,
            disabledContainerColor = Color.White.copy(alpha = 0.03f),
            disabledContentColor = AuthMuted.copy(alpha = 0.55f),
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DemoAccountsCard(
    enabled: Boolean,
    onRetailer: () -> Unit,
    onWholesaler: () -> Unit,
) {
    AuthGlassCard(compact = true) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Demo accounts",
                style = MaterialTheme.typography.titleMedium,
                color = AuthText,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Use a demo business account. It signs in through Drawbridge securely.",
                style = MaterialTheme.typography.bodySmall,
                color = AuthMuted,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoShortcut(
                title = "Retailer",
                subtitle = "retailer@test.com",
                icon = Icons.Default.Search,
                enabled = enabled,
                onClick = onRetailer,
            )
            DemoShortcut(
                title = "Wholesaler",
                subtitle = "wholesaler@test.com",
                icon = Icons.Default.CheckCircle,
                enabled = enabled,
                onClick = onWholesaler,
            )
        }
    }
}

@Composable
private fun DemoShortcut(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f)
    Surface(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .graphicsLayer(
                alpha = if (enabled) 1f else 0.55f,
                scaleX = scale,
                scaleY = scale,
            ),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuthEmerald.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AuthMint)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = AuthText, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AuthMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(58.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthEmerald,
            contentColor = Color(0xFF022C22),
            disabledContainerColor = AuthEmerald.copy(alpha = 0.46f),
            disabledContentColor = Color.White.copy(alpha = 0.82f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AuthSubmitButton(
    text: String,
    loadingText: String,
    onClick: () -> Unit,
    isLoading: Boolean,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .height(58.dp)
            .fillMaxWidth(),
        enabled = !isLoading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthEmerald,
            contentColor = Color(0xFF022C22),
            disabledContainerColor = AuthEmerald.copy(alpha = 0.46f),
            disabledContentColor = Color.White.copy(alpha = 0.82f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 0.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Text(
                text = if (isLoading) loadingText else text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AuthInlineMessage(message: String?) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(160)) + expandVertically(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(140)),
    ) {
        if (message == ServerNotFoundMessage) {
            ServerErrorCard(actionText = null, onAction = null)
            return@AnimatedVisibility
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.28f)),
        ) {
            Text(
                text = message.orEmpty(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD5D5),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private data class OnboardingSlide(
    val kicker: String,
    val title: String,
    val body: String,
    val visualStyle: OnboardingVisualStyle,
    val cards: List<OnboardingMiniCard>,
)

private enum class OnboardingVisualStyle {
    Grid,
    Hero,
    Control,
    Start,
}

private data class OnboardingMiniCard(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
)

private fun onboardingSlides(): List<OnboardingSlide> {
    val blue = Color(0xFF7DD3FC)
    val emerald = Primary500
    val amber = Color(0xFFFBBF24)
    val rose = Color(0xFFFB7185)
    return listOf(
        OnboardingSlide(
            kicker = "The problem",
            title = "Operations are scattered.",
            body = "Orders, stock, support, and payments live in too many places.",
            visualStyle = OnboardingVisualStyle.Grid,
            cards = listOf(
                OnboardingMiniCard("Stock risk", Icons.Default.CheckCircle, emerald),
                OnboardingMiniCard("Order blocked", Icons.Default.ShoppingCart, amber),
                OnboardingMiniCard("Ticket open", Icons.Default.Notifications, rose),
                OnboardingMiniCard("Catalog split", Icons.Default.Search, blue),
            ),
        ),
        OnboardingSlide(
            kicker = "The workflow",
            title = "Drawbridge connects the workflow.",
            body = "One mobile workflow for marketplace, cart, orders, inventory, and support.",
            visualStyle = OnboardingVisualStyle.Hero,
            cards = listOf(
                OnboardingMiniCard("Unified flow", Icons.Default.CheckCircle, emerald),
                OnboardingMiniCard("Build cart", Icons.Default.ShoppingCart, blue),
                OnboardingMiniCard("Open ticket", Icons.Default.Notifications, rose),
                OnboardingMiniCard("Order status", Icons.Default.CheckCircle, amber),
            ),
        ),
        OnboardingSlide(
            kicker = "The control room",
            title = "Real-time visibility.",
            body = "See stock, orders, alerts, and customer action in one place.",
            visualStyle = OnboardingVisualStyle.Control,
            cards = listOf(
                OnboardingMiniCard("Live catalog", Icons.Default.ShoppingCart, blue),
                OnboardingMiniCard("Low stock", Icons.Default.CheckCircle, emerald),
                OnboardingMiniCard("Unread alert", Icons.Default.Notifications, amber),
                OnboardingMiniCard("Saved item", Icons.Default.Favorite, rose),
            ),
        ),
        OnboardingSlide(
            kicker = "Start strong",
            title = "Run commerce with confidence.",
            body = "Sign in or create your business account to begin.",
            visualStyle = OnboardingVisualStyle.Start,
            cards = listOf(
                OnboardingMiniCard("Retailer", Icons.Default.Search, blue),
                OnboardingMiniCard("Wholesaler", Icons.Default.CheckCircle, emerald),
                OnboardingMiniCard("Orders", Icons.Default.CheckCircle, amber),
                OnboardingMiniCard("Support", Icons.Default.Notifications, rose),
            ),
        ),
    )
}
