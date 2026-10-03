package com.tankobun.app

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class TankobunThemeCatalogTest {
    @Test
    fun catalogHasTwoShapesAndNineThemesWithLightAndDarkSchemes() {
        assertEquals(2, tankobunArtDirectionChoices().size)
        assertEquals(9, tankobunPaletteChoices().size)
        TankobunVisiblePalettes.forEach { palette ->
            assertNotNull(generatedColorScheme(palette, dark = false))
            assertNotNull(generatedColorScheme(palette, dark = true))
        }
    }

    @Test
    fun everyLegacyModeMigratesToAVisibleTheme() {
        TankobunThemeMode.entries.forEach { legacy ->
            val preference = legacyThemePreference(legacy).normalized()
            assertTrue(preference.direction in tankobunArtDirectionChoices().map { it.id })
            assertTrue("$legacy -> ${preference.palette}", preference.palette in TankobunVisiblePalettes)
        }
    }

    @Test
    fun retiredPalettesKeepTheirLightOrDarkLook() {
        val plum = migratedThemePreference(automatic = false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.VELVET_PLUM)
        assertEquals(TankobunPaletteId.AMEIXA, plum.palette)
        assertEquals(TankobunColorMode.DARK, plum.mode)

        val peach = migratedThemePreference(automatic = false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.PEACH_COUNTRYSIDE)
        assertEquals(TankobunPaletteId.SUMI, peach.palette)
        assertEquals(TankobunColorMode.LIGHT, peach.mode)
        assertEquals(TankobunArtDirection.MOCHI_POP, peach.direction)

        val citrus = TankobunThemePreference(palette = TankobunPaletteId.CITRUS_CLASH).normalized()
        assertEquals(TankobunPaletteId.YUZU, citrus.palette)
    }

    @Test
    fun automaticPreferencesFollowTheSystemWithTheDefaultTheme() {
        val migrated = migratedThemePreference(automatic = true, TankobunArtDirection.NEON_CURRENT, TankobunPaletteId.NEON_KOI)
        assertEquals(TankobunColorMode.SYSTEM, migrated.mode)
        assertEquals(TankobunPaletteId.SUMI, migrated.palette)
        assertEquals(TankobunArtDirection.ORIGINAL, migrated.direction)
        assertTrue(migrated.isDark(systemDark = true))
        assertTrue(!migrated.isDark(systemDark = false))
    }

    @Test
    fun visibleOptionsSurviveNormalization() {
        tankobunArtDirectionChoices().forEach { direction ->
            (TankobunVisiblePalettes + TankobunPaletteId.DYNAMIC).forEach { palette ->
                TankobunColorMode.entries.forEach { mode ->
                    val preference = TankobunThemePreference(mode, direction.id, palette, pureBlack = true)
                    assertEquals(preference, preference.normalized())
                }
            }
        }
    }

    @Test
    fun pureBlackOnlyChangesDarkSurfaces() {
        val preference = TankobunThemePreference(palette = TankobunPaletteId.NEON, pureBlack = true)
        assertEquals(Color.Black, tankobunColorScheme(preference, dark = true).background)
        assertEquals(
            tankobunColorScheme(preference.copy(pureBlack = false), dark = false).background,
            tankobunColorScheme(preference, dark = false).background,
        )
    }

    @Test
    fun textAndControlPairsMeetWcagContrast() {
        TankobunVisiblePalettes.forEach { palette ->
            listOf(false, true).forEach { dark ->
                listOf(false, true).forEach { pureBlack ->
                    val colors = tankobunColorScheme(TankobunThemePreference(palette = palette, pureBlack = pureBlack), dark)
                    val pairs = listOf(
                        "background" to (colors.onBackground to colors.background),
                        "surfaceContainer" to (colors.onSurface to colors.surfaceContainer),
                        "surfaceContainerHigh" to (colors.onSurface to colors.surfaceContainerHigh),
                        "mutedText" to (colors.onSurfaceVariant to colors.surfaceContainerLow),
                        "accentText" to (colors.primary to colors.background),
                        "primary" to (colors.onPrimary to colors.primary),
                        "primaryContainer" to (colors.onPrimaryContainer to colors.primaryContainer),
                        "secondaryContainer" to (colors.onSecondaryContainer to colors.secondaryContainer),
                    )
                    pairs.forEach { (role, pair) ->
                        val contrast = contrastRatio(pair.first, pair.second)
                        assertTrue("$palette dark=$dark black=$pureBlack $role contrast was $contrast", contrast >= 4.5)
                    }
                }
            }
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
