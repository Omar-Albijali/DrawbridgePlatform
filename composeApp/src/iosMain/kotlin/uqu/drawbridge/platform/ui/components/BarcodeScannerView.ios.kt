package uqu.drawbridge.platform.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreDispatch.dispatch_get_main_queue
import platform.Foundation.NSNotificationCenter
import platform.UIKit.UIView
import platform.darwin.NSObject

@Composable
actual fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
) {
    class ScannerState { var isDebouncing = false }
    val state = remember { ScannerState() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IosCameraBarcodeScannerView(
            onBarcodeDetected = { value ->
                if (!state.isDebouncing) {
                    state.isDebouncing = true
                    onBarcodeScanned(value)
                    // Reset after delay
                    dispatch_get_main_queue().let {
                        platform.CoreDispatch.dispatch_after(
                            platform.CoreDispatch.dispatch_time(
                                platform.CoreDispatch.DISPATCH_TIME_NOW,
                                (1_500_000_000).toLong() // 1.5 seconds in nanoseconds
                            ),
                            it
                        ) {
                            state.isDebouncing = false
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        )

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Close Scanner", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun IosCameraBarcodeScannerView(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)
    val captureSession = remember { AVCaptureSession() }

    DisposableEffect(Unit) {
        onDispose {
            platform.CoreDispatch.dispatch_async(
                platform.CoreDispatch.dispatch_get_global_queue(0, 0u)
            ) {
                if (captureSession.isRunning()) {
                    captureSession.stopRunning()
                }
            }
        }
    }

    UIKitView(
        factory = {
            val containerView = UIView()
            captureSession.sessionPreset = AVCaptureSessionPresetMedium

            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null) {
                val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null)
                if (input != null && captureSession.canAddInput(input)) {
                    captureSession.addInput(input)
                }

                val metadataOutput = AVCaptureMetadataOutput()
                if (captureSession.canAddOutput(metadataOutput)) {
                    captureSession.addOutput(metadataOutput)

                    val delegate = BarcodeMetadataDelegate(currentOnBarcodeDetected)
                    metadataOutput.setMetadataObjectsDelegate(delegate, dispatch_get_main_queue())

                    // Set supported barcode types after adding to session
                    metadataOutput.metadataObjectTypes = listOf(
                        AVMetadataObjectTypeEAN13Code,
                        AVMetadataObjectTypeEAN8Code,
                        AVMetadataObjectTypeUPCECode,
                        AVMetadataObjectTypeCode128Code,
                        AVMetadataObjectTypeCode39Code,
                        AVMetadataObjectTypeQRCode,
                    )
                }

                val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
                previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
                previewLayer.frame = containerView.bounds
                containerView.layer.addSublayer(previewLayer)

                // Start session on background thread
                platform.CoreDispatch.dispatch_async(
                    platform.CoreDispatch.dispatch_get_global_queue(0, 0u)
                ) {
                    captureSession.startRunning()
                }
            }

            containerView
        },
        modifier = modifier,
    )
}

private class BarcodeMetadataDelegate(
    private val onBarcodeDetected: (String) -> Unit,
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection,
    ) {
        val metadataObjects = didOutputMetadataObjects
        for (obj in metadataObjects) {
            val readableObj = obj as? AVMetadataMachineReadableCodeObject ?: continue
            val value = readableObj.stringValue ?: continue
            if (value.isNotBlank()) {
                onBarcodeDetected(value)
                break
            }
        }
    }
}
