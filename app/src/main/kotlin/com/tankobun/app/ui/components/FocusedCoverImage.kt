package com.tankobun.app.ui.components

import android.content.Context
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Scale
import coil3.toBitmap
import com.tankobun.app.logic.DEFAULT_COVER_FOCUS
import com.tankobun.app.logic.coverFocusY
import com.tankobun.app.logic.focusVerticalBias
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A cover's interesting height (0 = top) and its width/height ratio. */
internal data class CoverFocus(val focusY: Float, val aspectRatio: Float)

private object CoverFocusCache {
    private val entries = LruCache<String, CoverFocus>(96)
    fun get(url: String): CoverFocus? = entries.get(url)
    fun put(url: String, focus: CoverFocus) {
        entries.put(url, focus)
    }
}

/** Analyses a small software copy of the image; the full image keeps loading as usual. */
@Composable
internal fun rememberCoverFocus(url: String?): CoverFocus? {
    val context = LocalContext.current
    var focus by remember(url) { mutableStateOf(url?.let(CoverFocusCache::get)) }
    LaunchedEffect(url) {
        if (url == null || focus != null) return@LaunchedEffect
        analyzeCoverFocus(context, url)?.let { result ->
            CoverFocusCache.put(url, result)
            focus = result
        }
    }
    return focus
}

private suspend fun analyzeCoverFocus(context: Context, url: String): CoverFocus? = try {
    val request = ImageRequest.Builder(context)
        .data(url)
        .size(FOCUS_SAMPLE_WIDTH, FOCUS_SAMPLE_HEIGHT)
        .scale(Scale.FIT)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request) as? SuccessResult
    result?.let {
        withContext(Dispatchers.Default) {
            val bitmap = it.image.toBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            CoverFocus(
                focusY = coverFocusY(pixels, bitmap.width, bitmap.height),
                aspectRatio = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1),
            )
        }
    }
} catch (error: Exception) {
    if (error is CancellationException) throw error
    null
}

/**
 * Crops an image to fill its box. With [focus] on, the vertical crop follows the cover's
 * interesting area (usually faces) instead of its middle, easing into place once analysed.
 */
@Composable
internal fun FocusedCoverImage(
    url: String?,
    contentDescription: String?,
    focus: Boolean,
    modifier: Modifier = Modifier,
    onError: (() -> Unit)? = null,
) {
    val coverFocus = if (focus) rememberCoverFocus(url) else null
    BoxWithConstraints(modifier) {
        val boxAspect = if (maxHeight.value > 0f) maxWidth.value / maxHeight.value else 1f
        val imageAspect = coverFocus?.aspectRatio ?: DEFAULT_COVER_ASPECT
        val visibleFraction = (imageAspect / boxAspect).coerceIn(0f, 1f)
        val targetBias = if (focus) {
            focusVerticalBias(coverFocus?.focusY ?: DEFAULT_COVER_FOCUS, visibleFraction)
        } else {
            0f
        }
        val bias by animateFloatAsState(
            targetValue = targetBias,
            animationSpec = tween(durationMillis = 420),
            label = "Cover focus",
        )
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, bias),
            modifier = Modifier.fillMaxSize(),
            onError = onError?.let { callback -> { callback() } },
        )
    }
}

private const val FOCUS_SAMPLE_WIDTH = 48
private const val FOCUS_SAMPLE_HEIGHT = 72
private const val DEFAULT_COVER_ASPECT = 2f / 3f
