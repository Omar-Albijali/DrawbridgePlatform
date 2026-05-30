package uqu.drawbridge.posdemo

import androidx.compose.runtime.Composable

@Composable
actual fun RequestCameraPermissionOnStartup() {
    // Left empty as iOS handles camera permission implicitly on first access
    // or through native Swift implementations.
}
