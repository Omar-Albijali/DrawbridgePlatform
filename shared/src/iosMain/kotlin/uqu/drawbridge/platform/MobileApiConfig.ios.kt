package uqu.drawbridge.platform

import platform.Foundation.NSBundle

private const val defaultSimulatorApiBaseUrl = "http://localhost:8080/api"
private const val infoPlistApiBaseUrlKey = "API_BASE_URL"

internal actual fun platformConfiguredApiBaseUrl(): String? {
    val configuredValue = NSBundle.mainBundle
        .objectForInfoDictionaryKey(infoPlistApiBaseUrlKey) as? String

    return configuredValue
        ?.let(::normalizeConfiguredApiBaseUrl)
}

internal actual fun platformDefaultApiBaseUrl(): String = defaultSimulatorApiBaseUrl

private fun normalizeConfiguredApiBaseUrl(value: String): String? {
    val trimmed = value.trim().trimEnd('/')
    if (trimmed.isBlank() || trimmed.startsWith("$(")) return null

    return if (trimmed.endsWith("/api")) trimmed else "$trimmed/api"
}
