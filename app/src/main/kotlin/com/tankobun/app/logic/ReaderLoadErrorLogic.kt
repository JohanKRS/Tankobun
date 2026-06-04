package com.tankobun.app.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.TimeoutCancellationException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

internal fun readerLoadErrorFor(context: Context, error: Throwable, source: SourceDescriptor?): ReaderLoadError {
    val sourceName = source?.name ?: "this source"
    return when (context.readerNetworkState()) {
        ReaderNetworkState.OFFLINE -> ReaderLoadError(
            title = "No internet connection",
            message = "Your device looks offline. Check Wi-Fi or mobile data, then try again.",
        )
        ReaderNetworkState.NO_INTERNET -> ReaderLoadError(
            title = "No internet access",
            message = "Your device is connected, but it cannot reach the internet right now. Check the connection and try again.",
        )
        ReaderNetworkState.ONLINE -> readerLoadErrorForOnlineSource(error, sourceName)
    }
}

internal fun readerLoadErrorForOnlineSource(error: Throwable, sourceName: String): ReaderLoadError {
    val statusCode = error.httpStatusCode()
    if (statusCode != null) {
        return when (statusCode) {
            403 -> ReaderLoadError(
                title = "Source blocked the request",
                message = "$sourceName refused to send this chapter. This can happen when a site changes its protection or region rules.",
            )
            404, 410 -> ReaderLoadError(
                title = "Chapter not found",
                message = "$sourceName says this chapter is no longer available. It may have been removed or moved.",
            )
            408, 429 -> ReaderLoadError(
                title = "Source is busy",
                message = "$sourceName is asking us to slow down. Wait a little, then try again.",
            )
            521, 522, 523, 524 -> ReaderLoadError(
                title = "Source server is down",
                message = "$sourceName is reachable through the internet, but its own server is not answering right now. Try again later or choose another source.",
            )
            in 500..599 -> ReaderLoadError(
                title = "Source is having trouble",
                message = "$sourceName is not responding properly right now. The site may be down or overloaded.",
            )
            else -> ReaderLoadError(
                title = "Source could not load the chapter",
                message = "$sourceName returned a problem while opening this chapter. Try again, or choose another source if it keeps happening.",
            )
        }
    }

    val causes = error.causeChain()
    val messageText = causes.mapNotNull { it.message }.joinToString(" ")
    return when {
        causes.any { it is UnknownHostException } -> ReaderLoadError(
            title = "Source cannot be found",
            message = "Your connection works, but $sourceName cannot be reached. The site may be offline or may have changed address.",
        )
        causes.any { it is SocketTimeoutException || it is TimeoutCancellationException || it is TimeoutException } ||
            messageText.contains("timed out", ignoreCase = true) ||
            messageText.contains("too long", ignoreCase = true) -> ReaderLoadError(
            title = "Source is not responding",
            message = "$sourceName did not answer in time. The site may be down, overloaded, or having server trouble right now.",
        )
        causes.any { it is ConnectException || it is NoRouteToHostException } -> ReaderLoadError(
            title = "Source cannot be reached",
            message = "$sourceName is not accepting connections right now. The site may be down or temporarily unavailable.",
        )
        causes.any { it is SocketException } -> ReaderLoadError(
            title = "Connection was interrupted",
            message = "The connection dropped while opening this chapter. Try again in a moment.",
        )
        causes.any { it is SSLException } -> ReaderLoadError(
            title = "Secure connection failed",
            message = "$sourceName could not make a safe connection. The site may have changed something on their side.",
        )
        messageText.contains("cloudflare", ignoreCase = true) ||
            messageText.contains("bypass", ignoreCase = true) -> ReaderLoadError(
            title = "Source protection stopped the request",
            message = "$sourceName is asking for a browser check that Tankobun could not pass right now. Try again later or use another source.",
        )
        else -> ReaderLoadError(
            title = "Chapter could not be loaded",
            message = "Tankobun could not open this chapter from $sourceName. The source may be temporarily down, or this chapter may no longer be available there.",
        )
    }
}

private fun Context.readerNetworkState(): ReaderNetworkState {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return ReaderNetworkState.ONLINE
    val network = connectivityManager.activeNetwork ?: return ReaderNetworkState.OFFLINE
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ReaderNetworkState.OFFLINE
    if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        return ReaderNetworkState.OFFLINE
    }
    return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
        ReaderNetworkState.ONLINE
    } else {
        ReaderNetworkState.NO_INTERNET
    }
}

private fun Throwable.httpStatusCode(): Int? =
    causeChain()
        .asSequence()
        .flatMap { cause -> listOfNotNull(cause.message, cause.toString()).asSequence() }
        .mapNotNull { text -> HTTP_STATUS_PATTERN.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() }
        .firstOrNull()

private fun Throwable.causeChain(): List<Throwable> = buildList {
    var current: Throwable? = this@causeChain
    while (current != null && current !in this) {
        add(current)
        current = current.cause
    }
}

private enum class ReaderNetworkState {
    ONLINE,
    OFFLINE,
    NO_INTERNET,
}

private val HTTP_STATUS_PATTERN = Regex("""HTTP(?: error)?\s+(\d{3})""", RegexOption.IGNORE_CASE)
