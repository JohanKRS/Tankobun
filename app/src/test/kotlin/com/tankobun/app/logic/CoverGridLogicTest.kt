package com.tankobun.app.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverGridLogicTest {
    private val spacing = 16f

    @Test
    fun phonesKeepTheChosenCount() {
        // Upright phones from 360dp to 480dp wide, minus the 18dp page margins.
        listOf(324f, 375f, 394f, 444f).forEach { width ->
            (2..4).forEach { preferred ->
                assertEquals("$preferred at ${width}dp", preferred, adaptiveCoverColumns(preferred, width, spacing))
            }
        }
    }

    @Test
    fun unmeasuredGridKeepsTheChosenCount() {
        assertEquals(3, adaptiveCoverColumns(preferredColumns = 3, availableWidthDp = 0f, spacingDp = spacing))
        assertEquals(1, adaptiveCoverColumns(preferredColumns = 0, availableWidthDp = 360f, spacingDp = spacing))
    }

    @Test
    fun landscapeTabletShowsAboutSevenCovers() {
        // 1280dp beside the navigation rail, and the same window with the bottom dock.
        assertEquals(7, adaptiveCoverColumns(preferredColumns = 4, availableWidthDp = 1_152f, spacingDp = spacing))
        assertEquals(8, adaptiveCoverColumns(preferredColumns = 4, availableWidthDp = 1_244f, spacingDp = spacing))
    }

    @Test
    fun widerGridsAddColumnsInProportion() {
        assertEquals(3, adaptiveCoverColumns(preferredColumns = 2, availableWidthDp = 764f, spacingDp = spacing))
        assertEquals(5, adaptiveCoverColumns(preferredColumns = 2, availableWidthDp = 1_152f, spacingDp = spacing))
        assertEquals(4, adaptiveCoverColumns(preferredColumns = 2, availableWidthDp = 815f, spacingDp = spacing))
        assertEquals(5, adaptiveCoverColumns(preferredColumns = 3, availableWidthDp = 815f, spacingDp = spacing))
    }

    @Test
    fun extraColumnsNeverShrinkCoversBelowTheMinimum() {
        (2..4).forEach { preferred ->
            (401..2_560 step 7).forEach { width ->
                val columns = adaptiveCoverColumns(preferred, width.toFloat(), spacing)
                val coverWidth = (width - spacing * (columns - 1)) / columns
                assertTrue("$preferred at ${width}dp", columns >= preferred)
                if (columns > preferred) {
                    assertTrue("$preferred at ${width}dp gives ${coverWidth}dp covers", coverWidth >= MIN_ADAPTIVE_COVER_WIDTH_DP)
                }
            }
        }
    }

    @Test
    fun denseChoiceIsKeptEvenWhenCoversAreSmall() {
        assertEquals(8, adaptiveCoverColumns(preferredColumns = 8, availableWidthDp = 764f, spacingDp = spacing))
    }

    @Test
    fun columnsNeverDropAsTheGridWidens() {
        (2..8).forEach { preferred ->
            var previous = adaptiveCoverColumns(preferred, 300f, spacing)
            (301..2_560).forEach { width ->
                val columns = adaptiveCoverColumns(preferred, width.toFloat(), spacing)
                assertTrue("$preferred at ${width}dp", columns >= previous)
                previous = columns
            }
        }
    }
}
