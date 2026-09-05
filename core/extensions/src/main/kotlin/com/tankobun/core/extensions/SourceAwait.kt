package com.tankobun.core.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import rx.Observable
import rx.Subscriber
import rx.schedulers.Schedulers
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Legacy extensions may do blocking work while subscribing. Use the shared IO
// pool, and unsubscribe on cancellation so their network calls can be cancelled.
private val sourceScheduler = Schedulers.from(Dispatchers.IO.asExecutor())

internal suspend fun <T : Any> Observable<T>.awaitSourceValue(): T = suspendCancellableCoroutine { continuation ->
    val subscriber = object : Subscriber<T>() {
        override fun onNext(value: T) {
            continuation.resume(value)
        }

        override fun onError(error: Throwable) {
            if (continuation.isActive) continuation.resumeWithException(error)
        }

        override fun onCompleted() = Unit
    }
    continuation.invokeOnCancellation { subscriber.unsubscribe() }
    first().subscribeOn(sourceScheduler).subscribe(subscriber)
}
