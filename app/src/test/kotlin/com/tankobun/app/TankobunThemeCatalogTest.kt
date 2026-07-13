package com.tankobun.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class TankobunThemeCatalogTest {
    @Test
    fun catalogHasTwoShapesAndFourteenIndependentPalettes() {
        assertEquals(2, tankobunArtDirectionChoices().size)
        assertEquals(14, tankobunPaletteChoices().size)
        assertEquals(15, TankobunPaletteId.entries.size)
    }

    @Test
    fun everyLegacyModeMigratesToAValidPreference() {
        TankobunThemeMode.entries.forEach { legacy ->
            val preference = legacyThemePreference(legacy).normalized()
            assertTrue(preference.direction in tankobunArtDirectionChoices().map { it.id })
            assertTrue(preference.palette in tankobunPaletteChoices().map { it.id })
        }
    }

    @Test
    fun everyVisibleShapeCanUseEveryVisiblePaletteWithoutNormalizationChangingIt() {
        tankobunArtDirectionChoices().forEach { direction ->
            tankobunPaletteChoices().forEach { palette ->
                val preference = TankobunThemePreference(false, direction.id, palette.id)
                assertEquals(preference, preference.normalized())
            }
        }
    }

    @Test
    fun retiredOptionsNormalizeWithoutLosingTheColorChoice() {
        val retiredShape = TankobunThemePreference(false, TankobunArtDirection.NEON_CURRENT, TankobunPaletteId.VELVET_PLUM)
        assertEquals(TankobunArtDirection.ORIGINAL, retiredShape.normalized().direction)
        assertEquals(TankobunPaletteId.VELVET_PLUM, retiredShape.normalized().palette)

        val retiredPalette = TankobunThemePreference(false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.CITRUS_CLASH)
        assertEquals(TankobunArtDirection.MOCHI_POP, retiredPalette.normalized().direction)
        assertEquals(TankobunPaletteId.YUZU_GARDEN, retiredPalette.normalized().palette)
    }

    @Test
    fun automaticModeResolvesToTheDocumentedDefaults() {
        val automatic = TankobunThemePreference()
        assertEquals(TankobunPaletteId.PEACH_COUNTRYSIDE, automatic.resolve(systemDark = false).palette)
        assertEquals(TankobunPaletteId.VELVET_PLUM, automatic.resolve(systemDark = true).palette)
        assertEquals(TankobunArtDirection.MOCHI_POP, automatic.resolve(systemDark = false).direction)
        assertEquals(TankobunArtDirection.ORIGINAL, automatic.resolve(systemDark = true).direction)
    }

    @Test
    fun textAndControlPairsMeetWcagContrast() {
        TankobunPaletteId.entries.forEach { palette ->
            val colors = tankobunColorScheme(palette)
            val pairs = listOf(
                "background" to (colors.onBackground to colors.background),
                "surface" to (colors.onSurface to colors.surface),
                "primary" to (colors.onPrimary to colors.primary),
                "secondary" to (colors.onSecondary to colors.secondary),
                "primaryContainer" to (colors.onPrimaryContainer to colors.primaryContainer),
                "secondaryContainer" to (colors.onSecondaryContainer to colors.secondaryContainer),
            )
            pairs.forEach { (role, pair) ->
                val contrast = contrastRatio(pair.first, pair.second)
                assertTrue("${palette.name} $role contrast was $contrast", contrast >= 4.5)
            }
        }
    }

    @Test
    fun highlightedActionsSupportWhiteTextInEveryPalette() {
        TankobunPaletteId.entries.forEach { palette ->
            val contrast = contrastRatio(Color.White, tankobunActionContainer(palette))
            assertTrue("${palette.name} action contrast was $contrast", contrast >= 4.5)
        }
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val first = luminance(a)
        val second = luminance(b)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    private fun luminance(color: Color): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    private fun linear(channel: Float): Double =
        if (channel <= 0.04045f) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}
