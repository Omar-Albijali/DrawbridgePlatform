package uqu.drawbridge.platform.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for iOS as it typically uses gestures
}
