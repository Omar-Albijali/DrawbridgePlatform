package uqu.drawbridge.posdemo

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

object PosIosEntryPoint {
    fun mainViewController(): UIViewController = ComposeUIViewController {
        App()
    }
}
