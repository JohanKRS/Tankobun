package com.tankobun.app.logic

import android.content.Context
import com.tankobun.app.R
import com.tankobun.core.anilist.AnilistGraphQlException
import com.tankobun.core.network.InputLimitExceededException
import com.tankobun.app.backup.ImportReadTimeoutException
import com.tankobun.app.backup.ImportDocumentReadException

internal fun Throwable.userMessage(context: Context, fallback: String): String = when (this) {
    is com.tankobun.core.anilist.IncompleteAniListLibraryException -> context.getString(R.string.msg_library_sync_incomplete)
    is InputLimitExceededException -> context.getString(R.string.import_file_too_large, maxBytes / (1024 * 1024))
    is ImportReadTimeoutException -> context.getString(R.string.import_file_timeout)
    is ImportDocumentReadException -> context.getString(R.string.import_file_unreadable)
    is AnilistGraphQlException -> when (statusCode) {
        401 -> context.getString(R.string.anilist_error_session_expired)
        429 -> context.getString(R.string.anilist_error_rate_limited)
        500 -> context.getString(R.string.anilist_error_server)
        else -> context.getString(
            R.string.anilist_error_request_failed,
            statusCode?.let { context.getString(R.string.anilist_error_status_suffix, it) }.orEmpty(),
        )
    }
    else -> message ?: fallback
}

internal fun Throwable.userMessage(fallback: String): String = message ?: fallback

internal fun Throwable.importUserMessage(context: Context, fallback: String): String = when (this) {
    is InputLimitExceededException, is ImportReadTimeoutException, is ImportDocumentReadException -> userMessage(context, fallback)
    else -> fallback
}
