package uqu.drawbridge.posdemo

import androidx.compose.runtime.Composable

@Composable
expect fun GlobalBarcodeScannerHook(enabled: Boolean, onBarcodeScanned: (String) -> Unit)
