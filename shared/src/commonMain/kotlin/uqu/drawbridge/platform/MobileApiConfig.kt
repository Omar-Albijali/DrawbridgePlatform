package uqu.drawbridge.platform

import uqu.drawbridge.platform.shared.BuildConfig

object MobileApiConfig {
    var baseUrl: String = BuildConfig.API_BASE_URL
        set(value) {
            field = value.trim().trimEnd('/')
        }
}
