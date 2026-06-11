package com.tankobun.app.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import com.tankobun.app.R
import com.tankobun.app.state.ReaderLoadError
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.TimeoutCancellationException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale
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
    return readerLoadErrorForOnlineSource(
        strings = AndroidReaderLoadErrorStrings(context),
        error = error,
        sourceName = sourceName,
    )
}

internal fun readerLoadErrorForOnlineSource(error: Throwable, sourceName: String): ReaderLoadError =
    readerLoadErrorForOnlineSource(
        strings = EnglishReaderLoadErrorStrings,
        error = error,
        sourceName = sourceName,
    )

private fun readerLoadErrorForOnlineSource(
    strings: ReaderLoadErrorStrings,
    error: Throwable,
    sourceName: String,
): ReaderLoadError {
    val statusCode = error.httpStatusCode()
    if (statusCode != null) {
        return when (statusCode) {
            403 -> ReaderLoadError(
                title = strings.get(R.string.reader_error_source_blocked_title),
                message = strings.get(R.string.reader_error_source_blocked_message, sourceName),
            )
            404, 410 -> ReaderLoadError(
                title = strings.get(R.string.reader_error_chapter_not_found_title),
                message = strings.get(R.string.reader_error_chapter_not_found_message, sourceName),
            )
            408, 429 -> ReaderLoadError(
                title = strings.get(R.string.reader_error_source_busy_title),
                message = strings.get(R.string.reader_error_source_busy_message, sourceName),
            )
            521, 522, 523, 524 -> ReaderLoadError(
                title = strings.get(R.string.reader_error_source_down_title),
                message = strings.get(R.string.reader_error_source_down_message, sourceName),
            )
            in 500..599 -> ReaderLoadError(
                title = strings.get(R.string.reader_error_source_trouble_title),
                message = strings.get(R.string.reader_error_source_trouble_message, sourceName),
            )
            else -> ReaderLoadError(
                title = strings.get(R.string.reader_error_source_problem_title),
                message = strings.get(R.string.reader_error_source_problem_message, sourceName),
            )
        }
    }

    val causes = error.causeChain()
    val messageText = causes.mapNotNull { it.message }.joinToString(" ")
    return when {
        causes.any { it is UnknownHostException } -> ReaderLoadError(
            title = strings.get(R.string.reader_error_source_missing_title),
            message = strings.get(R.string.reader_error_source_missing_message, sourceName),
        )
        causes.any { it is SocketTimeoutException || it is TimeoutCancellationException || it is TimeoutException } ||
            messageText.contains("timed out", ignoreCase = true) ||
            messageText.contains("too long", ignoreCase = true) -> ReaderLoadError(
            title = strings.get(R.string.reader_error_source_timeout_title),
            message = strings.get(R.string.reader_error_source_timeout_message, sourceName),
        )
        causes.any { it is ConnectException || it is NoRouteToHostException } -> ReaderLoadError(
            title = strings.get(R.string.reader_error_source_unreachable_title),
            message = strings.get(R.string.reader_error_source_unreachable_message, sourceName),
        )
        causes.any { it is SocketException } -> ReaderLoadError(
            title = strings.get(R.string.reader_error_connection_interrupted_title),
            message = strings.get(R.string.reader_error_connection_interrupted_message),
        )
        causes.any { it is SSLException } -> ReaderLoadError(
            title = strings.get(R.string.reader_error_ssl_title),
            message = strings.get(R.string.reader_error_ssl_message, sourceName),
        )
        messageText.contains("cloudflare", ignoreCase = true) ||
            messageText.contains("bypass", ignoreCase = true) -> ReaderLoadError(
            title = strings.get(R.string.reader_error_protection_title),
            message = strings.get(R.string.reader_error_protection_message, sourceName),
        )
        else -> ReaderLoadError(
            title = strings.get(R.string.reader_error_generic_title),
            message = strings.get(R.string.reader_error_generic_message, sourceName),
        )
    }
}

private interface ReaderLoadErrorStrings {
    fun get(@StringRes id: Int, vararg args: Any): String
}

private class AndroidReaderLoadErrorStrings(private val context: Context) : ReaderLoadErrorStrings {
    override fun get(id: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)
}

private object EnglishReaderLoadErrorStrings : ReaderLoadErrorStrings {
    override fun get(id: Int, vararg args: Any): String {
        val template = when (id) {
            R.string.reader_error_source_blocked_title -> "Source blocked the request"
            R.string.reader_error_source_blocked_message ->
                "%1\$s refused to send this chapter. This can happen when a site changes its protection or region rules."
            R.string.reader_error_chapter_not_found_title -> "Chapter not found"
            R.string.reader_error_chapter_not_found_message ->
                "%1\$s says this chapter is no longer available. It may have been removed or moved."
            R.string.reader_error_source_busy_title -> "Source is busy"
            R.string.reader_error_source_busy_message ->
                "%1\$s is asking us to slow down. Wait a little, then try again."
            R.string.reader_error_source_down_title -> "Source server is down"
            R.string.reader_error_source_down_message ->
                "%1\$s is reachable through the internet, but its own server is not answering right now. Try again later or choose another source."
            R.string.reader_error_source_trouble_title -> "Source is having trouble"
            R.string.reader_error_source_trouble_message ->
                "%1\$s is not responding properly right now. The site may be down or overloaded."
            R.string.reader_error_source_problem_title -> "Source could not load the chapter"
            R.string.reader_error_source_problem_message ->
                "%1\$s returned a problem while opening this chapter. Try again, or choose another source if it keeps happening."
            R.string.reader_error_source_missing_title -> "Source cannot be found"
            R.string.reader_error_source_missing_message ->
                "Your connection works, but %1\$s cannot be reached. The site may be offline or may have changed address."
            R.string.reader_error_source_timeout_title -> "Source is not responding"
            R.string.reader_error_source_timeout_message ->
                "%1\$s did not answer in time. The site may be down, overloaded, or having server trouble right now."
            R.string.reader_error_source_unreachable_title -> "Source cannot be reached"
            R.string.reader_error_source_unreachable_message ->
                "%1\$s is not accepting connections right now. The site may be down or temporarily unavailable."
            R.string.reader_error_connection_interrupted_title -> "Connection was interrupted"
            R.string.reader_error_connection_interrupted_message ->
                "The connection dropped while opening this chapter. Try again in a moment."
            R.string.reader_error_ssl_title -> "Secure connection failed"
            R.string.reader_error_ssl_message ->
                "%1\$s could not make a safe connection. The site may have changed something on their side."
            R.string.reader_error_protection_title -> "Source protection stopped the request"
            R.string.reader_error_protection_message ->
                "%1\$s is asking for a browser check that Tankobun could not pass right now. Try again later or use another source."
            R.string.reader_error_generic_title -> "Chapter could not be loaded"
            R.string.reader_error_generic_message ->
                "Tankobun could not open this chapter from %1\$s. The source may be temporarily down, or this chapter may no longer be available there."
            else -> "string-$id"
        }
        return if (args.isEmpty()) template else String.format(Locale.US, template, *args)
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
