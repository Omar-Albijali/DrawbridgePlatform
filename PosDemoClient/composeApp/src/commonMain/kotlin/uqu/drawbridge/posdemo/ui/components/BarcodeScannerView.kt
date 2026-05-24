package uqu.drawbridge.posdemo.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
)
