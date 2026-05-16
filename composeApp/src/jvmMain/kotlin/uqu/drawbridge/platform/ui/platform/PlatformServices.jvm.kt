package uqu.drawbridge.platform.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberPlatformServices(): PlatformServices {
    return remember {
        PlatformServices(
            secureTokenStorage = InMemorySecureTokenStorage(),
            urlOpener = NoopUrlOpener,
            haptics = NoopHaptics,
            permissions = NoopPermissionController,
            filePhotoPicker = NoopFilePhotoPicker,
        )
    }
}
