package uqu.drawbridge.posdemo

import androidx.compose.runtime.Composable

@Composable
actual fun GlobalBarcodeScannerHook(enabled: Boolean, onBarcodeScanned: (String) -> Unit) {
    // Not supported on mobile
}
