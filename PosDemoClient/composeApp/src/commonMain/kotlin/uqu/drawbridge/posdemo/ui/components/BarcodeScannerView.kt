package uqu.drawbridge.posdemo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ncgroup.kscan.ScannerView
import org.ncgroup.kscan.ScannerController
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.BarcodeFormat

@Composable
fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
) {
    val scannerController = remember { ScannerController() }
    val colors = MaterialTheme.colorScheme

    // Animated scan line
    val infiniteTransition = rememberInfiniteTransition()
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // ── Camera preview (no default UI) ────────────────
        ScannerView(
            modifier = Modifier.fillMaxSize(),
            codeTypes = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
            scannerUiOptions = null,
            scannerController = scannerController,
            result = { result ->
                if (result is BarcodeResult.OnSuccess) {
                    onBarcodeScanned(result.barcode.data)
                }
            }
        )

        // ── Custom overlay ────────────────────────────────
        // Dark semi-transparent overlay with a transparent cutout for the scan area
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Scan area — centered square, 65% of narrowest dimension
            val scanSize = minOf(canvasWidth, canvasHeight) * 0.65f
            val scanLeft = (canvasWidth - scanSize) / 2f
            val scanTop = (canvasHeight - scanSize) / 2f
            val scanRect = Rect(scanLeft, scanTop, scanLeft + scanSize, scanTop + scanSize)
            val cornerRadius = 16.dp.toPx()
            val cornerLength = scanSize * 0.15f
            val strokeWidth = 3.dp.toPx()

            // Draw dark overlay over the whole canvas
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Cut out the scan area (transparent hole)
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(scanRect.left, scanRect.top),
                size = Size(scanRect.width, scanRect.height),
                cornerRadius = CornerRadius(cornerRadius),
                blendMode = BlendMode.Clear
            )

            // Draw corner brackets
            val bracketColor = Color(0xFF3B82F6) // primary blue
            val bracketStroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            // Top-left corner
            drawLine(bracketColor, Offset(scanRect.left, scanRect.top + cornerLength), Offset(scanRect.left, scanRect.top + cornerRadius), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawArc(bracketColor, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.left, scanRect.top), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)
            drawLine(bracketColor, Offset(scanRect.left + cornerRadius, scanRect.top), Offset(scanRect.left + cornerLength, scanRect.top), strokeWidth = strokeWidth, cap = StrokeCap.Round)

            // Top-right corner
            drawLine(bracketColor, Offset(scanRect.right, scanRect.top + cornerLength), Offset(scanRect.right, scanRect.top + cornerRadius), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawArc(bracketColor, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.right - cornerRadius * 2, scanRect.top), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)
            drawLine(bracketColor, Offset(scanRect.right - cornerRadius, scanRect.top), Offset(scanRect.right - cornerLength, scanRect.top), strokeWidth = strokeWidth, cap = StrokeCap.Round)

            // Bottom-left corner
            drawLine(bracketColor, Offset(scanRect.left, scanRect.bottom - cornerLength), Offset(scanRect.left, scanRect.bottom - cornerRadius), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawArc(bracketColor, startAngle = 90f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.left, scanRect.bottom - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)
            drawLine(bracketColor, Offset(scanRect.left + cornerRadius, scanRect.bottom), Offset(scanRect.left + cornerLength, scanRect.bottom), strokeWidth = strokeWidth, cap = StrokeCap.Round)

            // Bottom-right corner
            drawLine(bracketColor, Offset(scanRect.right, scanRect.bottom - cornerLength), Offset(scanRect.right, scanRect.bottom - cornerRadius), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            drawArc(bracketColor, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(scanRect.right - cornerRadius * 2, scanRect.bottom - cornerRadius * 2), size = Size(cornerRadius * 2, cornerRadius * 2), style = bracketStroke)
            drawLine(bracketColor, Offset(scanRect.right - cornerRadius, scanRect.bottom), Offset(scanRect.right - cornerLength, scanRect.bottom), strokeWidth = strokeWidth, cap = StrokeCap.Round)

            // Animated scan line
            val lineY = scanRect.top + cornerRadius + (scanRect.height - cornerRadius * 2) * scanLineProgress
            drawLine(
                color = bracketColor.copy(alpha = 0.6f),
                start = Offset(scanRect.left + cornerRadius, lineY),
                end = Offset(scanRect.right - cornerRadius, lineY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // ── Top bar controls ──────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Title
            Text(
                text = "Scan Barcode",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )

            // Torch toggle
            IconButton(
                onClick = { scannerController.setTorch(!scannerController.torchEnabled) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (scannerController.torchEnabled) colors.primary.copy(alpha = 0.8f)
                        else Color.Black.copy(alpha = 0.45f)
                    ),
            ) {
                Text(
                    text = if (scannerController.torchEnabled) "🔦" else "💡",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }

        // ── Bottom hint ───────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Text(
                text = "Point camera at a barcode",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}
