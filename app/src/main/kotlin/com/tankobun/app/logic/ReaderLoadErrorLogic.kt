package com.tankobun.app.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.tankobun.app.R
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
    val sourceName = source?.name ?: context.getString(R.string.reader_error_this_source)
    return when (context.readerNetworkState()) {
        ReaderNetworkState.OFFLINE -> ReaderLoadError(
            title = context.getString(R.string.reader_error_no_internet_title),
            message = context.getString(R.string.reader_error_no_internet_message),
        )
        ReaderNetworkState.NO_INTERNET -> ReaderLoadError(
            title = context.getString(R.string.reader_error_no_access_title),
            message = context.getString(R.string.reader_error_no_access_message),
        )
        ReaderNetworkState.ONLINE -> readerLoadErrorForOnlineSource(context, error, sourceName)
    }
}

internal fun readerLoadErrorForOnlineSource(context: Context, error: Throwable, sourceName: String): ReaderLoadError {
    val statusCode = error.httpStatusCode()
    if (statusCode != null) {
        return when (statusCode) {
            403 -> ReaderLoadError(
                title = context.getString(R.string.reader_error_source_blocked_title),
                message = context.getString(R.string.reader_error_source_blocked_message, sourceName),
            )
            404, 410 -> ReaderLoadError(
                title = context.getString(R.string.reader_error_chapter_not_found_title),
                message = context.getString(R.string.reader_error_chapter_not_found_message, sourceName),
            )
            408, 429 -> ReaderLoadError(
                title = context.getString(R.string.reader_error_source_busy_title),
                message = context.getString(R.string.reader_error_source_busy_message, sourceName),
            )
            521, 522, 523, 524 -> ReaderLoadError(
                title = context.getString(R.string.reader_error_source_down_title),
                message = context.getString(R.string.reader_error_source_down_message, sourceName),
            )
            in 500..599 -> ReaderLoadError(
                title = context.getString(R.string.reader_error_source_trouble_title),
                message = context.getString(R.string.reader_error_source_trouble_message, sourceName),
            )
            else -> ReaderLoadError(
                title = context.getString(R.string.reader_error_source_problem_title),
                message = context.getString(R.string.reader_error_source_problem_message, sourceName),
            )
        }
    }

    val causes = error.causeChain()
    val messageText = causes.mapNotNull { it.message }.joinToString(" ")
    return when {
        causes.any { it is UnknownHostException } -> ReaderLoadError(
            title = context.getString(R.string.reader_error_source_missing_title),
            message = context.getString(R.string.reader_error_source_missing_message, sourceName),
        )
        causes.any { it is SocketTimeoutException || it is TimeoutCancellationException || it is TimeoutException } ||
            messageText.contains("timed out", ignoreCase = true) ||
            messageText.contains("too long", ignoreCase = true) -> ReaderLoadError(
            title = context.getString(R.string.reader_error_source_timeout_title),
            message = context.getString(R.string.reader_error_source_timeout_message, sourceName),
        )
        causes.any { it is ConnectException || it is NoRouteToHostException } -> ReaderLoadError(
            title = context.getString(R.string.reader_error_source_unreachable_title),
            message = context.getString(R.string.reader_error_source_unreachable_message, sourceName),
        )
        causes.any { it is SocketException } -> ReaderLoadError(
            title = context.getString(R.string.reader_error_connection_interrupted_title),
            message = context.getString(R.string.reader_error_connection_interrupted_message),
        )
        causes.any { it is SSLException } -> ReaderLoadError(
            title = context.getString(R.string.reader_error_ssl_title),
            message = context.getString(R.string.reader_error_ssl_message, sourceName),
        )
        messageText.contains("cloudflare", ignoreCase = true) ||
            messageText.contains("bypass", ignoreCase = true) -> ReaderLoadError(
            title = context.getString(R.string.reader_error_protection_title),
            message = context.getString(R.string.reader_error_protection_message, sourceName),
        )
        else -> ReaderLoadError(
            title = context.getString(R.string.reader_error_generic_title),
            message = context.getString(R.string.reader_error_generic_message, sourceName),
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
