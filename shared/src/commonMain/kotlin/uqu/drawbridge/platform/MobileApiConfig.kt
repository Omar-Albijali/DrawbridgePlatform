package uqu.drawbridge.platform

import uqu.drawbridge.platform.shared.BuildConfig

object MobileApiConfig {
    var baseUrl: String = BuildConfig.API_BASE_URL.ifBlank { platformDefaultApiBaseUrl() }
        set(value) {
            field = value.trim().trimEnd('/')
        }

    fun resolveResourceUrl(resourceUrl: String): String {
        val trimmed = resourceUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }

        val apiBase = baseUrl.trimEnd('/')
        val origin = apiBase.removeSuffix("/api")
        val path = when {
            trimmed.startsWith("/") -> trimmed
            else -> "/$trimmed"
        }
        return origin + path
    }
}

internal expect fun platformDefaultApiBaseUrl(): String
