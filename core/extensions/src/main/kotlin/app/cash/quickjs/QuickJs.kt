package app.cash.quickjs

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.json.JSONTokener
import uy.kohesive.injekt.TankobunInjektRegistry

class QuickJs private constructor(
    private val webView: WebView,
) : Closeable {
    @JvmOverloads
    fun evaluate(script: String, fileName: String = "TankobunQuickJs"): Any? {
        check(!closed) { "QuickJs is closed" }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw QuickJsException("Synchronous JavaScript evaluation cannot run on the main thread", fileName)
        }

        val latch = CountDownLatch(1)
        val result = AtomicReference<Result<Any?>>()
        mainHandler.post {
            if (closed) {
                result.set(Result.failure(IllegalStateException("QuickJs is closed")))
                latch.countDown()
                return@post
            }
            webView.evaluateJavascript(script) { value ->
                result.set(runCatching { value.toQuickJsValue() })
                latch.countDown()
            }
        }
        if (!latch.await(EVALUATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw QuickJsException("JavaScript evaluation timed out", fileName)
        }
        return result.get().getOrThrow()
    }

    fun <T> set(name: String, clazz: Class<T>, value: T) {
        throw UnsupportedOperationException("JavaScript interface binding is not supported")
    }

    fun <T> get(name: String, clazz: Class<T>): T {
        throw UnsupportedOperationException("JavaScript interface binding is not supported")
    }

    @JvmOverloads
    fun compile(script: String, fileName: String = "TankobunQuickJs"): ByteArray =
        script.toByteArray(StandardCharsets.UTF_8)

    fun execute(bytecode: ByteArray): Any? =
        evaluate(bytecode.toString(StandardCharsets.UTF_8))

    override fun close() {
        if (closed) return
        closed = true
        mainHandler.post {
            webView.stopLoading()
            webView.destroy()
        }
    }

    private var closed = false

    companion object {
        private const val EVALUATION_TIMEOUT_SECONDS = 10L
        private val mainHandler = Handler(Looper.getMainLooper())

        @JvmStatic
        fun create(): QuickJs {
            val app = TankobunInjektRegistry.applicationOrNull()
                ?: throw IllegalStateException("Tankobun application has not been registered for QuickJs")
            return QuickJs(
                onMain {
                    WebView(app).also { webView ->
                        webView.settings.javaScriptEnabled = true
                    }
                },
            )
        }

        private fun <T> onMain(block: () -> T): T {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return block()
            }
            val latch = CountDownLatch(1)
            val result = AtomicReference<Result<T>>()
            mainHandler.post {
                result.set(runCatching(block))
                latch.countDown()
            }
            latch.await()
            return result.get().getOrThrow()
        }
    }
}

class QuickJsException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, fileName: String) : super("$message ($fileName)")
}

private fun String?.toQuickJsValue(): Any? {
    if (this == null || this == "null") return null
    return runCatching {
        when (val parsed = JSONTokener(this).nextValue()) {
            org.json.JSONObject.NULL -> null
            else -> parsed
        }
    }.getOrElse {
        this
    }
}
