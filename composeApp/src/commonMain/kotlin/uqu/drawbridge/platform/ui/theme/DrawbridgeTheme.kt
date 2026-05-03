package uqu.drawbridge.platform.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Primary500 = Color(0xFF3B82F6) // Blue 500
internal val Primary600 = Color(0xFF2563EB)
internal val Primary100 = Color(0xFFDBEAFE)

internal val Secondary500 = Color(0xFF6B7280)

internal val SuccessColor = Color(0xFF10B981)
internal val WarningColor = Color(0xFFF59E0B)
internal val ErrorColor = Color(0xFFEF4444)

private val LightScheme = lightColorScheme(
    primary = Primary500,
    onPrimary = Color.White,
    primaryContainer = Primary100,
    onPrimaryContainer = Color(0xFF1E3A8A),
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
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Primary100,
    secondary = Color(0xFF9CA3AF),
    onSecondary = Color(0xFF111827),
    background = Color(0xFF020617),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
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
            displayLarge = baseTypography.displayLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp),
            displayMedium = baseTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
            headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
            headlineMedium = baseTypography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
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
    
    val gradientStart = if (isDark) Color(0xFF020617) else Color(0xFFF8FAFC)
    val gradientEnd = if (isDark) Color(0xFF0F172A) else Color(0xFFE2E8F0)
    
    val accent1 = if (isDark) Color(0x333B82F6) else Color(0x153B82F6)
    val accent2 = if (isDark) Color(0x2210B981) else Color(0x0F10B981)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(gradientStart, gradientEnd)
                )
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(Brush.radialGradient(listOf(accent1, Color.Transparent), radius = 1000f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 250.dp)
                .background(
                    Brush.radialGradient(
                        listOf(accent2, Color.Transparent), 
                        radius = 1200f, 
                        center = androidx.compose.ui.geometry.Offset(1200f, 800f)
                    )
                ),
        )
    }
}
