package uqu.drawbridge.platform.ui.platform

import androidx.compose.runtime.Composable

internal enum class HapticSignal {
    Selection,
    Success,
    Warning,
    Error,
}

internal enum class AppPermission {
    Camera,
    PhotoLibrary,
    Notifications,
}

internal enum class PermissionStatus {
    Granted,
    Denied,
    Unavailable,
}

internal data class PickedFile(
    val name: String,
    val mimeType: String?,
    val bytes: ByteArray,
)

internal interface SecureTokenStorage {
    suspend fun saveToken(token: String)
    suspend fun readToken(): String?
    suspend fun clearToken()
}

internal interface UrlOpener {
    fun openUrl(url: String): Boolean
}

internal interface Haptics {
    fun perform(signal: HapticSignal)
}

internal interface PermissionController {
    suspend fun request(permission: AppPermission): PermissionStatus
    suspend fun current(permission: AppPermission): PermissionStatus
}

internal interface FilePhotoPicker {
    suspend fun pickPhoto(): PickedFile?
    suspend fun pickFile(): PickedFile?
}

internal interface NativeOptionPicker {
    suspend fun pickOption(title: String, options: List<String>, selectedIndex: Int = -1): Int?
}

internal data class PlatformServices(
    val secureTokenStorage: SecureTokenStorage,
    val urlOpener: UrlOpener,
    val haptics: Haptics,
    val permissions: PermissionController,
    val filePhotoPicker: FilePhotoPicker,
    val optionPicker: NativeOptionPicker,
)

@Composable
internal expect fun rememberPlatformServices(): PlatformServices

internal class InMemorySecureTokenStorage : SecureTokenStorage {
    private var token: String? = null

    override suspend fun saveToken(token: String) {
        this.token = token
    }

    override suspend fun readToken(): String? = token

    override suspend fun clearToken() {
        token = null
    }
}

internal object NoopUrlOpener : UrlOpener {
    override fun openUrl(url: String): Boolean = false
}

internal object NoopHaptics : Haptics {
    override fun perform(signal: HapticSignal) = Unit
}

internal object NoopPermissionController : PermissionController {
    override suspend fun request(permission: AppPermission): PermissionStatus = PermissionStatus.Unavailable

    override suspend fun current(permission: AppPermission): PermissionStatus = PermissionStatus.Unavailable
}

internal object NoopFilePhotoPicker : FilePhotoPicker {
    override suspend fun pickPhoto(): PickedFile? = null

    override suspend fun pickFile(): PickedFile? = null
}

internal object NoopNativeOptionPicker : NativeOptionPicker {
    override suspend fun pickOption(title: String, options: List<String>, selectedIndex: Int): Int? = null
}
