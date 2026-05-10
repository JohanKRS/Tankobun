package com.tankobun.core.network

sealed interface HttpResult<out T> {
    data class Success<T>(val value: T, val cached: Boolean = false) : HttpResult<T>
    data class Failure(
        val message: String,
        val statusCode: Int? = null,
        val retryAfterMillis: Long? = null,
    ) : HttpResult<Nothing>
}
