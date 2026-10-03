package com.tankobun.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverFocusTest {
    private val width = 48
    private val height = 72

    @Test
    fun followsAFaceNearTheTopOverATitleBelow() {
        val image = canvas(rgb(200, 200, 206))
        face(image, centerY = 0.2f)
        lettering(image, fromY = 0.72f, toY = 0.86f)
        val focus = coverFocusY(image, width, height)
        assertTrue("focus=$focus", focus < 0.32f)
    }

    @Test
    fun followsAFaceNearTheBottom() {
        val image = canvas(rgb(40, 52, 90))
        face(image, centerY = 0.74f)
        val focus = coverFocusY(image, width, height)
        assertTrue("focus=$focus", focus > 0.55f)
    }

    @Test
    fun beigePaperBesideLetteringDoesNotLookLikeSkin() {
        val image = canvas(rgb(236, 222, 182))
        lettering(image, fromY = 0.08f, toY = 0.22f)
        face(image, centerY = 0.62f)
        val focus = coverFocusY(image, width, height)
        assertTrue("focus=$focus", focus > 0.45f)
    }

    @Test
    fun flatImagesKeepTheDefault() {
        assertEquals(DEFAULT_COVER_FOCUS, coverFocusY(canvas(rgb(120, 120, 120)), width, height))
        assertEquals(DEFAULT_COVER_FOCUS, coverFocusY(IntArray(4), 2, 2))
    }

    @Test
    fun biasPlacesTheFocusFortyPercentDownTheVisibleCrop() {
        assertEquals(0f, focusVerticalBias(0.2f, 1f))
        assertEquals(-1f, focusVerticalBias(0.05f, 0.5f))
        assertEquals(1f, focusVerticalBias(0.98f, 0.5f))
        // Visible top = 0.5 - 0.4 × 0.5 = 0.3 of a 0.5 range.
        assertEquals(0.2f, focusVerticalBias(0.5f, 0.5f), 1e-4f)
    }

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    private fun canvas(color: Int) = IntArray(width * height) { color }

    /** A peach oval with dark eyes and a mouth, roughly how a drawn face reads at 48 px. */
    private fun face(image: IntArray, centerY: Float) {
        val cx = width / 2f
        val cy = centerY * height
        val rx = width * 0.2f
        val ry = height * 0.09f
        for (y in 0 until height) for (x in 0 until width) {
            val dx = (x - cx) / rx
            val dy = (y - cy) / ry
            if (dx * dx + dy * dy <= 1f) image[y * width + x] = rgb(250, 214, 188)
        }
        val eyeY = (cy - ry * 0.2f).toInt()
        for (eyeX in listOf((cx - rx * 0.45f).toInt(), (cx + rx * 0.45f).toInt())) {
            for (dy in -1..1) for (dx in -1..1) image[(eyeY + dy) * width + eyeX + dx] = rgb(40, 30, 60)
        }
        val mouthY = (cy + ry * 0.5f).toInt()
        for (x in (cx - 2).toInt()..(cx + 2).toInt()) image[mouthY * width + x] = rgb(170, 70, 80)
    }

    /** Black strokes on white across the full width, like a title block. */
    private fun lettering(image: IntArray, fromY: Float, toY: Float) {
        for (y in (fromY * height).toInt() until (toY * height).toInt()) for (x in 0 until width) {
            image[y * width + x] = if ((x / 2 + y / 3) % 2 == 0) rgb(15, 15, 15) else rgb(250, 250, 250)
        }
    }
}
