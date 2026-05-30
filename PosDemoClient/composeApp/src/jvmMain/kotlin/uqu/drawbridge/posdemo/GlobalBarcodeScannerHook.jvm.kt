package uqu.drawbridge.posdemo

import androidx.compose.runtime.*
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import java.util.logging.Level
import java.util.logging.Logger

@Composable
actual fun GlobalBarcodeScannerHook(enabled: Boolean, onBarcodeScanned: (String) -> Unit) {
    val currentOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)
    
    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val logger = Logger.getLogger(GlobalScreen::class.java.getPackage().name)
        logger.level = Level.OFF
        logger.useParentHandlers = false

        if (!GlobalScreen.isNativeHookRegistered()) {
            try {
                GlobalScreen.registerNativeHook()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        var buffer = ""
        var lastKeyTime = 0L

        val listener = object : NativeKeyListener {
            override fun nativeKeyTyped(e: NativeKeyEvent?) {}

            override fun nativeKeyPressed(e: NativeKeyEvent?) {
                e ?: return
                val now = System.currentTimeMillis()
                // Barcode scanners typically "type" very fast.
                // 500ms timeout between keystrokes to reset the buffer.
                if (now - lastKeyTime > 500) {
                    buffer = ""
                }
                lastKeyTime = now
                
                if (e.keyCode == NativeKeyEvent.VC_ENTER) {
                    if (buffer.length == 13 && buffer.all { it.isDigit() }) {
                        val gtin = buffer
                        buffer = ""
                        currentOnBarcodeScanned(gtin)
                    } else {
                        buffer = ""
                    }
                } else {
                    val char = when (e.keyCode) {
                        NativeKeyEvent.VC_0 -> '0'
                        NativeKeyEvent.VC_1 -> '1'
                        NativeKeyEvent.VC_2 -> '2'
                        NativeKeyEvent.VC_3 -> '3'
                        NativeKeyEvent.VC_4 -> '4'
                        NativeKeyEvent.VC_5 -> '5'
                        NativeKeyEvent.VC_6 -> '6'
                        NativeKeyEvent.VC_7 -> '7'
                        NativeKeyEvent.VC_8 -> '8'
                        NativeKeyEvent.VC_9 -> '9'
                        else -> null
                    }
                    if (char != null) {
                        buffer += char
                    }
                }
            }

            override fun nativeKeyReleased(e: NativeKeyEvent?) {}
        }

        GlobalScreen.addNativeKeyListener(listener)

        onDispose {
            GlobalScreen.removeNativeKeyListener(listener)
        }
    }
}
