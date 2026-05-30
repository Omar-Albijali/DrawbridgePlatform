package uqu.drawbridge.platform.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFBooleanTrue
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.UIKit.UIApplication
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleActionSheet
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerImageURL
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import kotlin.coroutines.resume

@Composable
internal actual fun rememberPlatformServices(): PlatformServices {
    val filePhotoPicker = remember { IosFilePhotoPicker() }
    val optionPicker = remember { IosNativeOptionPicker() }
    return remember {
        PlatformServices(
            secureTokenStorage = KeychainSecureTokenStorage(),
            urlOpener = NoopUrlOpener,
            haptics = NoopHaptics,
            permissions = NoopPermissionController,
            filePhotoPicker = filePhotoPicker,
            optionPicker = optionPicker,
        )
    }
}

private class IosFilePhotoPicker : FilePhotoPicker {
    private var activeDelegate: ImagePickerDelegate? = null

    override suspend fun pickPhoto(): PickedFile? = suspendCancellableCoroutine { continuation ->
        val presenter = topViewController()
        if (presenter == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val picker = UIImagePickerController()
        val delegate = ImagePickerDelegate(
            picker = picker,
            continuation = continuation,
            onComplete = { activeDelegate = null },
        )
        activeDelegate = delegate
        picker.delegate = delegate
        continuation.invokeOnCancellation {
            picker.dismissViewControllerAnimated(true, completion = null)
            if (activeDelegate === delegate) activeDelegate = null
        }
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    override suspend fun pickFile(): PickedFile? = pickPhoto()
}

private class ImagePickerDelegate(
    private val picker: UIImagePickerController,
    private val continuation: CancellableContinuation<PickedFile?>,
    private val onComplete: () -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        val url = didFinishPickingMediaWithInfo[UIImagePickerControllerImageURL] as? NSURL
        val data = image?.let { UIImageJPEGRepresentation(it, 0.92) }
        finish(
            data?.let {
                PickedFile(
                    name = url?.lastPathComponent ?: "product-image.jpg",
                    mimeType = "image/jpeg",
                    bytes = it.toByteArray(),
                )
            },
        )
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        finish(null)
    }

    private fun finish(file: PickedFile?) {
        picker.dismissViewControllerAnimated(true, completion = null)
        if (continuation.isActive) continuation.resume(file)
        onComplete()
    }
}

private fun topViewController(): UIViewController? {
    val fallbackWindow = UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow
    val window = UIApplication.sharedApplication.keyWindow
        ?: fallbackWindow
        ?: return null
    var controller = window.rootViewController ?: return null
    while (controller.presentedViewController != null) {
        controller = controller.presentedViewController ?: break
    }
    return controller
}

private class IosNativeOptionPicker : NativeOptionPicker {
    override suspend fun pickOption(title: String, options: List<String>, selectedIndex: Int): Int? {
        if (options.isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            val presenter = topViewController()
            if (presenter == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            val alert = UIAlertController.alertControllerWithTitle(
                title = title,
                message = null,
                preferredStyle = UIAlertControllerStyleActionSheet,
            )
            options.forEachIndexed { index, option ->
                val label = if (index == selectedIndex) "✓ $option" else option
                alert.addAction(
                    UIAlertAction.actionWithTitle(
                        title = label,
                        style = UIAlertActionStyleDefault,
                        handler = {
                            if (continuation.isActive) continuation.resume(index)
                        },
                    ),
                )
            }
            alert.addAction(
                UIAlertAction.actionWithTitle(
                    title = "Cancel",
                    style = UIAlertActionStyleCancel,
                    handler = {
                        if (continuation.isActive) continuation.resume(null)
                    },
                ),
            )
            continuation.invokeOnCancellation { alert.dismissViewControllerAnimated(true, completion = null) }
            presenter.presentViewController(alert, animated = true, completion = null)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val length = length.toInt()
    if (length <= 0) return ByteArray(0)
    return bytes?.reinterpret<ByteVar>()?.readBytes(length) ?: ByteArray(0)
}

@OptIn(ExperimentalForeignApi::class)
private class KeychainSecureTokenStorage : SecureTokenStorage {
    override suspend fun saveToken(token: String) {
        if (token.isBlank()) {
            clearToken()
            return
        }

        clearToken()
        val status = SecItemAdd(tokenQuery(includeValue = token), null)
        if (status != errSecSuccess) {
            throw SecureTokenStorageException("Could not save secure session.")
        }
    }

    override suspend fun readToken(): String? = memScoped {
        val result = alloc<CPointerVarOf<CFTypeRef>>()
        val status = SecItemCopyMatching(tokenQuery(returnData = true), result.ptr)

        when (status) {
            errSecSuccess -> {
                val dataRef: CFDataRef = result.value?.reinterpret() ?: return@memScoped null
                dataRef.toUtf8String()
            }
            errSecItemNotFound -> null
            else -> null
        }
    }

    override suspend fun clearToken() {
        val status = SecItemDelete(tokenQuery())
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw SecureTokenStorageException("Could not clear secure session.")
        }
    }

    private fun tokenQuery(
        includeValue: String? = null,
        returnData: Boolean = false,
    ): CFDictionaryRef {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
            ?: throw SecureTokenStorageException("Could not prepare secure session.")

        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, KeychainService.toCFString())
        CFDictionaryAddValue(query, kSecAttrAccount, KeychainAccount.toCFString())

        if (includeValue != null) {
            CFDictionaryAddValue(query, kSecValueData, includeValue.toCFData())
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }

        if (returnData) {
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        }

        return query
    }

    private fun String.toCFString(): CFStringRef {
        return CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)
            ?: throw SecureTokenStorageException("Could not prepare secure session.")
    }

    private fun String.toCFData(): CFDataRef {
        val bytes = encodeToByteArray()
        return bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        } ?: throw SecureTokenStorageException("Could not prepare secure session.")
    }

    private fun CFDataRef.toUtf8String(): String? {
        val length = CFDataGetLength(this).toInt()
        if (length <= 0) {
            return null
        }

        val bytes = CFDataGetBytePtr(this)
            ?.reinterpret<ByteVar>()
            ?.readBytes(length)
            ?: return null
        return bytes.decodeToString()
    }

    private companion object {
        const val KeychainService = "uqu.drawbridge.platform.auth"
        const val KeychainAccount = "drawbridge.jwt"
    }
}

private class SecureTokenStorageException(message: String) : Exception(message)
