package uqu.drawbridge.platform.ui.common

import uqu.drawbridge.platform.MobileApiException

internal const val ServerNotFoundMessage = "Server not found"

internal fun userReadableMessage(error: Throwable, fallback: String): String {
    if (error.looksLikeServerNotFound()) {
        return ServerNotFoundMessage
    }

    val message = (error as? MobileApiException)?.message
        ?: error.message
        ?: fallback

    return message
        .takeIf { it.isNotBlank() && !it.looksTechnical() }
        ?: fallback
}

private fun Throwable.looksLikeServerNotFound(): Boolean {
    val text = buildString {
        var current: Throwable? = this@looksLikeServerNotFound
        var depth = 0
        while (current != null && depth < 4) {
            append(current.toString())
            append(' ')
            append(current.message.orEmpty())
            append(' ')
            current = current.cause
            depth += 1
        }
    }.lowercase()

    return listOf(
        "could not connect",
        "connection refused",
        "failed to connect",
        "connectexception",
        "unknownhost",
        "unresolvedaddress",
        "network is unreachable",
        "no route to host",
        "nsurlerrordomain code=-1004",
        "code=-1004",
        "errno 61",
        "timed out",
        "timeout",
    ).any { it in text }
}

private fun String.looksTechnical(): Boolean {
    if (length > 180) return true
    val text = lowercase()
    return listOf(
        "exception",
        "nsurlerror",
        "userinfo",
        "domain=",
        "code=",
        "localhost",
        "http://",
        "https://",
    ).any { it in text }
}
