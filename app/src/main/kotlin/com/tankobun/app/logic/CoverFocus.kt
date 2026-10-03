package com.tankobun.app.logic

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * The height (0 = top) of a cover that should sit [FOCUS_ANCHOR] of the way down a cropped view,
 * the way a portrait frames a face with room for the body below.
 *
 * Face detectors are trained on photos and miss most manga art, so this follows the smartcrop
 * idea instead: skin-toned pixels count most, but only where there is nearby line detail (eyes,
 * hair, mouths), which keeps flat beige backgrounds and paper textures from winning. Plain detail
 * counts a little, title lettering spanning the full width counts less than centered art, and a
 * soft prior favors the upper middle, where covers usually place faces.
 *
 * [argb] is a small downscaled image (about 48×72 is plenty) in row-major ARGB order.
 */
internal fun coverFocusY(
    argb: IntArray,
    width: Int,
    height: Int,
    windowFraction: Float = DEFAULT_FOCUS_WINDOW,
): Float {
    if (width < 3 || height < 3 || argb.size < width * height) return DEFAULT_COVER_FOCUS
    val luma = FloatArray(width * height)
    val skin = FloatArray(width * height)
    val saturation = FloatArray(width * height)
    for (index in 0 until width * height) {
        val color = argb[index]
        val r = (color shr 16 and 0xFF) / 255f
        val g = (color shr 8 and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        luma[index] = 0.299f * r + 0.587f * g + 0.114f * b
        skin[index] = skinLikelihood(r, g, b, luma[index])
        val maxChannel = max(r, max(g, b))
        val minChannel = minOf(r, g, b)
        saturation[index] = if (maxChannel <= 0f) 0f else (maxChannel - minChannel) / maxChannel
    }

    // Line detail: absolute Laplacian of luminance.
    val detail = FloatArray(width * height)
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val i = y * width + x
            val laplacian = 4 * luma[i] - luma[i - 1] - luma[i + 1] - luma[i - width] - luma[i + width]
            detail[i] = abs(laplacian).coerceAtMost(1f)
        }
    }
    val nearbyDetail = boxBlur(detail, width, height, radius = 2)
    val detailScale = nearbyDetail.maxOrNull()?.takeIf { it > 0f } ?: 1f

    val rows = FloatArray(height)
    val detailCap = width * 0.06f
    for (y in 0 until height) {
        var skinScore = 0f
        var detailScore = 0f
        for (x in 0 until width) {
            val i = y * width + x
            val localDetail = nearbyDetail[i] / detailScale
            val centerWeight = 1f - 0.6f * abs(x / (width - 1f) - 0.5f) * 2f
            skinScore += skin[i] * (0.15f + localDetail) * 2.2f * centerWeight
            detailScore += (detail[i] * 0.18f + saturation[i] * localDetail * 0.08f) * centerWeight
        }
        // Lettering fills whole rows with edges; capping plain detail keeps titles from outvoting faces.
        val rowScore = skinScore + detailScore.coerceAtMost(detailCap)
        val position = y / (height - 1f)
        val prior = exp(-((position - DEFAULT_COVER_FOCUS) * (position - DEFAULT_COVER_FOCUS)) / (2 * 0.32f * 0.32f))
        rows[y] = rowScore * (0.45f + 0.55f * prior)
    }

    val total = rows.sum()
    if (total < 0.5f) return DEFAULT_COVER_FOCUS
    // Score every anchor with an asymmetric window: rows near the anchor count most, and the
    // weight tapers more gently below it (the body) than above it (headroom).
    val window = windowFraction.coerceIn(0.1f, 1f) * height
    val above = (FOCUS_ANCHOR * window).coerceAtLeast(1f)
    val below = ((1f - FOCUS_ANCHOR) * window).coerceAtLeast(1f)
    var bestAnchor = (DEFAULT_COVER_FOCUS * (height - 1)).toInt()
    var bestScore = -1f
    for (anchor in 0 until height) {
        val first = (anchor - above).toInt().coerceAtLeast(0)
        val last = (anchor + below).toInt().coerceAtMost(height - 1)
        var score = 0f
        for (y in first..last) {
            val distance = (y - anchor).toFloat()
            val weight = if (distance < 0f) 1f + distance / above else 1f - 0.5f * distance / below
            score += rows[y] * weight.coerceAtLeast(0f)
        }
        if (score > bestScore + 1e-4f) {
            bestScore = score
            bestAnchor = anchor
        }
    }
    return ((bestAnchor + 0.5f) / height).coerceIn(0f, 1f)
}

/**
 * Vertical [androidx.compose.ui.BiasAlignment] value that places [focusY] at [FOCUS_ANCHOR] of
 * the part of an image that stays visible when it is cropped to fill a box ([visibleFraction] of
 * its height).
 */
internal fun focusVerticalBias(focusY: Float, visibleFraction: Float): Float {
    if (visibleFraction >= 0.999f) return 0f
    val visible = visibleFraction.coerceIn(0.01f, 1f)
    val top = (focusY - FOCUS_ANCHOR * visible).coerceIn(0f, 1f - visible)
    return (2f * top / (1f - visible) - 1f).coerceIn(-1f, 1f)
}

/** Faces sit in the upper middle of most covers; used before analysis finishes or without signal. */
internal const val DEFAULT_COVER_FOCUS = 0.35f

/** How far down the visible crop the focus lands. */
internal const val FOCUS_ANCHOR = 0.4f

/** About how much of a 2:3 cover the tablet hero shows. */
private const val DEFAULT_FOCUS_WINDOW = 0.47f

private val skinReference = run {
    val r = 0.78f
    val g = 0.57f
    val b = 0.44f
    val magnitude = sqrt(r * r + g * g + b * b)
    floatArrayOf(r / magnitude, g / magnitude, b / magnitude)
}

/** Chromaticity close to drawn and real skin, from deep tan to the pale peach common in manga. */
private fun skinLikelihood(r: Float, g: Float, b: Float, lightness: Float): Float {
    if (lightness < 0.22f || lightness > 0.97f) return 0f
    val magnitude = sqrt(r * r + g * g + b * b)
    if (magnitude <= 0f) return 0f
    val dr = r / magnitude - skinReference[0]
    val dg = g / magnitude - skinReference[1]
    val db = b / magnitude - skinReference[2]
    val similarity = 1f - sqrt(dr * dr + dg * dg + db * db)
    // Skin has red above green above blue; this drops greys and most pastel backgrounds.
    if (!(r > g && g > b)) return 0f
    // Drawn and real skin sit around 10–32° of hue with modest saturation. Paper, sand and
    // parchment backgrounds are yellower, and logos are far more saturated.
    val hue = 60f * (g - b) / (r - b)
    val hueWeight = when {
        hue <= 32f -> 1f
        hue >= 40f -> 0f
        else -> (40f - hue) / 8f
    }
    val saturation = (r - b) / r
    val saturationWeight = when {
        saturation < 0.06f -> 0f
        saturation <= 0.5f -> 1f
        saturation >= 0.65f -> 0f
        else -> (0.65f - saturation) / 0.15f
    }
    return ((similarity - SKIN_THRESHOLD) / (1f - SKIN_THRESHOLD)).coerceIn(0f, 1f) * hueWeight * saturationWeight
}

private const val SKIN_THRESHOLD = 0.78f

private fun boxBlur(values: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
    val horizontal = FloatArray(values.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0f
            var count = 0
            for (dx in -radius..radius) {
                val nx = x + dx
                if (nx in 0 until width) {
                    sum += values[y * width + nx]
                    count++
                }
            }
            horizontal[y * width + x] = sum / count
        }
    }
    val result = FloatArray(values.size)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var sum = 0f
            var count = 0
            for (dy in -radius..radius) {
                val ny = y + dy
                if (ny in 0 until height) {
                    sum += horizontal[ny * width + x]
                    count++
                }
            }
            result[y * width + x] = sum / count
        }
    }
    return result
}
