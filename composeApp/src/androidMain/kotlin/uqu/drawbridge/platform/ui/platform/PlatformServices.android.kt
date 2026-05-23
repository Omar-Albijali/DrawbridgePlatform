package uqu.drawbridge.platform.ui.platform

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
internal actual fun rememberPlatformServices(): PlatformServices {
    val context = LocalContext.current
    val filePhotoPicker = rememberAndroidFilePhotoPicker(context)
    val optionPicker = remember(context) { AndroidNativeOptionPicker(context) }
    return remember(filePhotoPicker, optionPicker) {
        PlatformServices(
            secureTokenStorage = InMemorySecureTokenStorage(),
            urlOpener = NoopUrlOpener,
            haptics = NoopHaptics,
            permissions = NoopPermissionController,
            filePhotoPicker = filePhotoPicker,
            optionPicker = optionPicker,
        )
    }
}

@Composable
private fun rememberAndroidFilePhotoPicker(context: Context): FilePhotoPicker {
    var pendingContinuation by remember { mutableStateOf<CancellableContinuation<PickedFile?>?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val continuation = pendingContinuation ?: return@rememberLauncherForActivityResult
        pendingContinuation = null
        continuation.resume(uri?.let { context.readPickedFile(it) })
    }

    return remember(context, imageLauncher) {
        object : FilePhotoPicker {
            override suspend fun pickPhoto(): PickedFile? = suspendCancellableCoroutine { continuation ->
                pendingContinuation = continuation
                continuation.invokeOnCancellation {
                    if (pendingContinuation === continuation) pendingContinuation = null
                }
                imageLauncher.launch("image/*")
            }

            override suspend fun pickFile(): PickedFile? = pickPhoto()
        }
    }
}

private fun Context.readPickedFile(uri: Uri): PickedFile? {
    val resolver = contentResolver
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
    } ?: "product-image.jpg"
    val bytes = resolver.openInputStream(uri)?.use { input -> input.readBytes() } ?: return null
    return PickedFile(
        name = name,
        mimeType = resolver.getType(uri),
        bytes = bytes,
    )
}

private class AndroidNativeOptionPicker(private val context: Context) : NativeOptionPicker {
    override suspend fun pickOption(title: String, options: List<String>, selectedIndex: Int): Int? {
        if (options.isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            val dialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialogInterface, which ->
                    if (continuation.isActive) continuation.resume(which)
                    dialogInterface.dismiss()
                }
                .setNegativeButton("Cancel") { dialogInterface, _ ->
                    if (continuation.isActive) continuation.resume(null)
                    dialogInterface.dismiss()
                }
                .create()
            dialog.setOnCancelListener {
                if (continuation.isActive) continuation.resume(null)
            }
            continuation.invokeOnCancellation { dialog.dismiss() }
            dialog.show()
        }
    }
}
