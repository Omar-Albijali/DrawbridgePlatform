package uqu.drawbridge.posdemo

import androidx.compose.runtime.Composable

@Composable
actual fun RequestCameraPermissionOnStartup() {
    // Left empty as JVM desktop apps generally do not require runtime permissions 
    // for standard webcam access in the same way mobile platforms do.
}
