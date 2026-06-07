package com.tankobun.app.ui.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

internal enum class ReaderPanAxis {
    BOTH,
    HORIZONTAL,
    WEBTOON,
}

internal data class WebtoonReaderPageItem(
    val chapter: SourceChapter,
    val page: ReaderPage,
    val pageIndex: Int,
)

internal class ReaderMotionState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    private var flingJob: Job? = null
    private var zoomAnimationJob: Job? = null

    fun stop() {
        flingJob?.cancel()
        flingJob = null
        zoomAnimationJob?.cancel()
        zoomAnimationJob = null
    }

    fun resetZoom(scope: CoroutineScope) {
        animateTransform(scope, targetScale = 1f, targetOffset = Offset.Zero)
    }

    fun animateTransform(scope: CoroutineScope, targetScale: Float, targetOffset: Offset) {
        stop()
        zoomAnimationJob = scope.launch {
            val startScale = scale
            val startOffset = offset
            val startNanos = withFrameNanos { it }
            val durationNanos = READER_TRANSFORM_ANIMATION_NANOS
            do {
                val frameNanos = withFrameNanos { it }
                val progress = ((frameNanos - startNanos).toFloat() / durationNanos).coerceIn(0f, 1f)
                val eased = 1f - (1f - progress).pow(3)
                scale = readerLerp(startScale, targetScale, eased)
                offset = Offset(
                    x = readerLerp(startOffset.x, targetOffset.x, eased),
                    y = readerLerp(startOffset.y, targetOffset.y, eased),
                )
            } while (progress < 1f)
            scale = targetScale
            offset = targetOffset
            zoomAnimationJob = null
        }
    }

    fun launchPagedFling(
        scope: CoroutineScope,
        velocity: Velocity,
        width: Float,
        height: Float,
        panAxis: ReaderPanAxis,
        contentWidth: Float = width,
        contentHeight: Float = height,
    ) {
        val boundedVelocity = velocity.boundedReaderFlingVelocity()
        val initialVelocity = when (panAxis) {
            ReaderPanAxis.BOTH -> Offset(boundedVelocity.x, boundedVelocity.y)
            ReaderPanAxis.HORIZONTAL,
            ReaderPanAxis.WEBTOON -> Offset(boundedVelocity.x, 0f)
        }
        val panBounds = readerPanBounds(scale, width, height, contentWidth, contentHeight)
        if (!panBounds.canPan || !initialVelocity.hasReaderStartVelocity()) return
        stop()
        flingJob = scope.launch {
            var velocityOffset = initialVelocity
            var lastFrameNanos = 0L
            while (velocityOffset.hasReaderFlingVelocity()) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    continue
                }

                val deltaSeconds = readerFrameDeltaSeconds(frameNanos, lastFrameNanos)
                lastFrameNanos = frameNanos
                val proposedOffset = offset + velocityOffset * deltaSeconds
                val clampedOffset = proposedOffset.clampedReaderOffset(
                    scale = scale,
                    width = width,
                    height = height,
                    contentWidth = contentWidth,
                    contentHeight = contentHeight,
                )
                offset = when (panAxis) {
                    ReaderPanAxis.BOTH -> clampedOffset
                    ReaderPanAxis.HORIZONTAL,
                    ReaderPanAxis.WEBTOON -> Offset(clampedOffset.x, 0f)
                }

                velocityOffset = Offset(
                    x = if (clampedOffset.x != proposedOffset.x) 0f else velocityOffset.x,
                    y = if (clampedOffset.y != proposedOffset.y || panAxis != ReaderPanAxis.BOTH) {
                        0f
                    } else {
                        velocityOffset.y
                    },
                ) * readerFlingDecay(deltaSeconds)
            }
        }
    }

    fun launchZoomedWebtoonFling(
        scope: CoroutineScope,
        velocity: Velocity,
        width: Float,
        height: Float,
        dispatchRawDelta: (Float) -> Float,
    ) {
        val boundedVelocity = velocity.boundedReaderFlingVelocity()
        var velocityX = boundedVelocity.x
        var scrollVelocityY = zoomedWebtoonScrollVelocity(boundedVelocity.y, scale)
        if (scale <= 1.01f || (abs(velocityX) < READER_MIN_FLING_START_VELOCITY && abs(scrollVelocityY) < READER_MIN_FLING_START_VELOCITY)) {
            return
        }
        stop()
        flingJob = scope.launch {
            var lastFrameNanos = 0L
            while (abs(velocityX) > READER_FLING_STOP_VELOCITY || abs(scrollVelocityY) > READER_FLING_STOP_VELOCITY) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    continue
                }

                val deltaSeconds = readerFrameDeltaSeconds(frameNanos, lastFrameNanos)
                lastFrameNanos = frameNanos

                if (abs(velocityX) > READER_FLING_STOP_VELOCITY) {
                    val proposedOffset = offset + Offset(velocityX * deltaSeconds, 0f)
                    val clampedOffset = proposedOffset.clampedReaderOffset(scale, width, height)
                    offset = Offset(clampedOffset.x, 0f)
                    if (clampedOffset.x != proposedOffset.x) velocityX = 0f
                }

                if (abs(scrollVelocityY) > READER_FLING_STOP_VELOCITY) {
                    dispatchRawDelta(scrollVelocityY * deltaSeconds)
                }

                val decay = readerFlingDecay(deltaSeconds)
                velocityX *= decay
                scrollVelocityY *= decay
            }
        }
    }
}

@Composable
internal fun rememberReaderMotionState(transformKey: String): ReaderMotionState {
    val state = remember(transformKey) { ReaderMotionState() }
    DisposableEffect(state) {
        onDispose { state.stop() }
    }
    return state
}

internal fun webtoonReaderPageItems(
    previousSegment: ReaderChapterSegment?,
    chapter: SourceChapter,
    pages: List<ReaderPage>,
    nextSegment: ReaderChapterSegment?,
): List<WebtoonReaderPageItem> =
    buildList {
        previousSegment?.let { segment ->
            segment.pages.forEachIndexed { index, page ->
                add(WebtoonReaderPageItem(segment.chapter, page, index))
            }
        }
        pages.forEachIndexed { index, page ->
            add(WebtoonReaderPageItem(chapter, page, index))
        }
        nextSegment?.let { segment ->
            segment.pages.forEachIndexed { index, page ->
                add(WebtoonReaderPageItem(segment.chapter, page, index))
            }
        }
    }

internal fun Velocity.boundedReaderFlingVelocity(
    maxVelocity: Float = READER_MAX_FLING_VELOCITY_PX_PER_SECOND,
): Velocity =
    Velocity(
        x = x.boundedReaderVelocity(maxVelocity),
        y = y.boundedReaderVelocity(maxVelocity),
    )

internal fun zoomedWebtoonScrollVelocity(velocityY: Float, scale: Float): Float =
    -velocityY.boundedReaderVelocity() / scale.coerceAtLeast(1f)

internal fun readerFrameDeltaSeconds(frameNanos: Long, lastFrameNanos: Long): Float =
    ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, READER_MAX_FRAME_DELTA_SECONDS)

private fun Float.boundedReaderVelocity(maxVelocity: Float = READER_MAX_FLING_VELOCITY_PX_PER_SECOND): Float =
    takeIf { it.isFinite() }?.coerceIn(-maxVelocity, maxVelocity) ?: 0f

private fun Offset.hasReaderFlingVelocity(): Boolean =
    abs(x) > READER_FLING_STOP_VELOCITY || abs(y) > READER_FLING_STOP_VELOCITY

private fun Offset.hasReaderStartVelocity(): Boolean =
    abs(x) > READER_MIN_FLING_START_VELOCITY || abs(y) > READER_MIN_FLING_START_VELOCITY

private fun readerFlingDecay(deltaSeconds: Float): Float =
    READER_FLING_DECAY_PER_FRAME.pow(deltaSeconds * 60f)

internal const val READER_MAX_FLING_VELOCITY_PX_PER_SECOND = 9_000f
internal const val READER_FLING_STOP_VELOCITY = 20f
private const val READER_MIN_FLING_START_VELOCITY = 90f
private const val READER_MAX_FRAME_DELTA_SECONDS = 0.05f
private const val READER_FLING_DECAY_PER_FRAME = 0.88f
private const val READER_TRANSFORM_ANIMATION_NANOS = 180_000_000L
