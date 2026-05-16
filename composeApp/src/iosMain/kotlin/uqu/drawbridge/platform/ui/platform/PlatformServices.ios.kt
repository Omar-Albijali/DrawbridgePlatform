package uqu.drawbridge.platform.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
internal actual fun rememberPlatformServices(): PlatformServices {
    return remember {
        PlatformServices(
            secureTokenStorage = KeychainSecureTokenStorage(),
            urlOpener = NoopUrlOpener,
            haptics = NoopHaptics,
            permissions = NoopPermissionController,
            filePhotoPicker = NoopFilePhotoPicker,
        )
    }
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
