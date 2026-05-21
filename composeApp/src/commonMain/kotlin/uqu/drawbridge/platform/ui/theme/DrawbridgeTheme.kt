package uqu.drawbridge.platform.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val Primary500 = Color(0xFF10B981)
internal val Primary600 = Color(0xFF059669)
internal val Primary100 = Color(0xFFD1FAE5)

internal val Secondary500 = Color(0xFF0284C7)

internal val SuccessColor = Color(0xFF10B981)
internal val WarningColor = Color(0xFFF59E0B)
internal val ErrorColor = Color(0xFFEF4444)

internal val AppNavyBase = Color(0xFF03111F)
internal val AppNavySurface = Color(0xFF0B1F33)
internal val AppNavySurfaceHigh = Color(0xFF102A3D)
internal val AppDarkLine = Color(0xFF27465C)
internal val AppMutedText = Color(0xFFA8B7C7)

private val LightScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = Color(0xFF052E2B),
    primaryContainer = Primary100,
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Secondary500,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = ErrorColor,
)

private val DarkScheme = darkColorScheme(
    primary = Primary500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Primary100,
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    background = AppNavyBase,
    onBackground = Color(0xFFF8FAFC),
    surface = AppNavySurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AppNavySurfaceHigh,
    onSurfaceVariant = AppMutedText,
    outline = AppDarkLine,
    error = ErrorColor,
)

@Composable
internal fun DrawbridgeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val baseTypography = Typography()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = baseTypography.copy(
            displayLarge = baseTypography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 0.sp),
            displayMedium = baseTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.sp),
            headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            headlineMedium = baseTypography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
            titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
            bodyLarge = baseTypography.bodyLarge.copy(lineHeight = 24.sp, letterSpacing = 0.15.sp),
            labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp),
        ),
        content = content,
    )
}

@Composable
internal fun AppBackground() {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.2f
    
    val gradientColors = if (isDark) {
        listOf(
            Color(0xFF03111F),
            Color(0xFF0B1F33),
            Color(0xFF03111F),
        )
    } else {
        listOf(
            Color(0xFFF8FAFC),
            Color(0xFFEFF6FF),
            Color(0xFFF8FAFC),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = gradientColors,
                )
            ),
    )
}
