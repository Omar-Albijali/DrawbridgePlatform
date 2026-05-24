package uqu.drawbridge.posdemo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Palette ────────────────────────────────────────────────
// Teal / Slate dark palette — no purple anywhere.

private val Navy900      = Color(0xFF03111F)
private val Navy800      = Color(0xFF0D1F2D)
private val Navy700      = Color(0xFF162F43)
private val Navy600      = Color(0xFF1A3A52)
private val Slate500     = Color(0xFF475569)
private val Slate400     = Color(0xFF94A3B8)
private val Slate200     = Color(0xFFE2E8F0)
private val Blue500      = Color(0xFF3B82F6)
private val Blue600      = Color(0xFF2563EB)
private val Green500     = Color(0xFF10B981)
private val Green900     = Color(0xFF065F46)
private val Red500       = Color(0xFFEF4444)
private val SlateOutline = Color(0xFF334155)

// ── Color Scheme ───────────────────────────────────────────

val PosDarkColorScheme = darkColorScheme(
    primary              = Blue500,
    onPrimary            = Color.White,
    primaryContainer     = Blue600,
    onPrimaryContainer   = Color.White,

    secondary            = Green500,
    onSecondary          = Color.White,
    secondaryContainer   = Green900,
    onSecondaryContainer = Color.White,

    background           = Navy900,
    onBackground         = Slate200,

    surface              = Navy800,
    onSurface            = Slate200,
    surfaceVariant       = Navy700,
    onSurfaceVariant     = Slate400,

    surfaceContainerHigh = Navy600,

    outline              = SlateOutline,
    outlineVariant       = Slate500,

    error                = Red500,
    onError              = Color.White,

    inverseSurface       = Slate200,
    inverseOnSurface     = Navy900,
)

// ── Typography ─────────────────────────────────────────────

private val PosTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ── Theme Wrapper ──────────────────────────────────────────

@Composable
fun PosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PosDarkColorScheme,
        typography  = PosTypography,
        content     = content,
    )
}
