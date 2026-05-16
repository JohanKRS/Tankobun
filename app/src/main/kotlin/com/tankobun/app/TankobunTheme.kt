package com.tankobun.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

@Immutable
data class TankobunThemeTokens(
    val appBackdrop: Color,
    val elevatedSurface: Color,
    val softAccent: Color,
    val readerOverlay: Color,
    val drawerHandle: Color,
    val coverScrim: Color,
)

@Immutable
data class TankobunThemeChoice(
    val mode: TankobunThemeMode,
    val name: String,
    val description: String,
    val dark: Boolean?,
    val swatches: List<Color>,
)

private data class TankobunThemeSpec(
    val colors: ColorScheme,
    val tokens: TankobunThemeTokens,
)

fun tankobunThemeChoices(): List<TankobunThemeChoice> = listOf(
    TankobunThemeChoice(
        mode = TankobunThemeMode.SYSTEM,
        name = "Bunny's Pick",
        description = "Follows your tablet",
        dark = null,
        swatches = listOf(Color(0xFFFFF7F0), Color(0xFFB82235), Color(0xFFFF8A58)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.BUNNY_MOCHI,
        name = "Bunny Mochi",
        description = "Warm, soft, default",
        dark = false,
        swatches = listOf(Color(0xFFFFF7F0), Color(0xFFB82235), Color(0xFFFFD9DD)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.PEACH_SODA,
        name = "Peach Soda",
        description = "Bright sunset shelf",
        dark = false,
        swatches = listOf(Color(0xFFFFF3E7), Color(0xFFD95F3F), Color(0xFFFFC857)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.MATCHA_MILK,
        name = "Matcha Milk",
        description = "Calm reading nook",
        dark = false,
        swatches = listOf(Color(0xFFF7FAEF), Color(0xFF4F7D5B), Color(0xFFD7E7BF)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.MIDNIGHT_RAMEN,
        name = "Midnight Ramen",
        description = "Cozy dark default",
        dark = true,
        swatches = listOf(Color(0xFF130D10), Color(0xFFFF7A88), Color(0xFFFFB078)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.STARRY_INK,
        name = "Starry Ink",
        description = "Blue-black pages",
        dark = true,
        swatches = listOf(Color(0xFF0A1020), Color(0xFF8FB6FF), Color(0xFFFFD166)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.PLUM_NIGHT,
        name = "Plum Night",
        description = "Velvety and gentle",
        dark = true,
        swatches = listOf(Color(0xFF170F1E), Color(0xFFE58AD8), Color(0xFFA7E8BD)),
    ),
)

private val TankobunLightTokens = TankobunThemeTokens(
    appBackdrop = Color(0xFFFFF7F0),
    elevatedSurface = Color(0xFFFFFFFF),
    softAccent = Color(0xFFFFECE0),
    readerOverlay = Color(0xCC080609),
    drawerHandle = Color(0x33B82235),
    coverScrim = Color(0x22000000),
)

private val TankobunDarkTokens = TankobunThemeTokens(
    appBackdrop = Color(0xFF130D10),
    elevatedSurface = Color(0xFF21171A),
    softAccent = Color(0xFF331E21),
    readerOverlay = Color(0xDD000000),
    drawerHandle = Color(0x55FF7A88),
    coverScrim = Color(0x44000000),
)

val LocalTankobunTokens = staticCompositionLocalOf { TankobunLightTokens }

private val TankobunLightColors = lightColorScheme(
    primary = Color(0xFFB82235),
    onPrimary = Color.White,
    secondary = Color(0xFFFF8A58),
    onSecondary = Color(0xFF2A0B10),
    tertiary = Color(0xFFFFD84D),
    onTertiary = Color(0xFF2D2300),
    background = Color(0xFFFFFAF4),
    onBackground = Color(0xFF241316),
    surface = Color(0xFFFFFCF8),
    onSurface = Color(0xFF241316),
    surfaceVariant = Color(0xFFFFDED5),
    onSurfaceVariant = Color(0xFF5D403B),
    primaryContainer = Color(0xFFFFD9DD),
    onPrimaryContainer = Color(0xFF3F0010),
    secondaryContainer = Color(0xFFFFE0C7),
    onSecondaryContainer = Color(0xFF3B1500),
)

private val TankobunDarkColors = darkColorScheme(
    primary = Color(0xFFFF7A88),
    onPrimary = Color(0xFF4C0012),
    secondary = Color(0xFFFFB078),
    onSecondary = Color(0xFF4A1D00),
    tertiary = Color(0xFFFFE66D),
    onTertiary = Color(0xFF3B3000),
    background = Color(0xFF171013),
    onBackground = Color(0xFFFFEDEA),
    surface = Color(0xFF21171A),
    onSurface = Color(0xFFFFEDEA),
    surfaceVariant = Color(0xFF4A3337),
    onSurfaceVariant = Color(0xFFF1C7C2),
    primaryContainer = Color(0xFF7C1023),
    onPrimaryContainer = Color(0xFFFFD9DD),
    secondaryContainer = Color(0xFF773A12),
    onSecondaryContainer = Color(0xFFFFE0C7),
)

@Composable
fun TankobunTheme(
    themeMode: TankobunThemeMode,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedMode = when (themeMode) {
        TankobunThemeMode.SYSTEM -> if (systemDark) TankobunThemeMode.MIDNIGHT_RAMEN else TankobunThemeMode.BUNNY_MOCHI
        TankobunThemeMode.LIGHT -> TankobunThemeMode.BUNNY_MOCHI
        TankobunThemeMode.DARK -> TankobunThemeMode.MIDNIGHT_RAMEN
        else -> themeMode
    }
    val spec = themeSpecFor(resolvedMode)

    MaterialTheme(
        colorScheme = spec.colors,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(
            LocalTankobunTokens provides spec.tokens,
            content = content,
        )
    }
}

private fun themeSpecFor(mode: TankobunThemeMode): TankobunThemeSpec = when (mode) {
    TankobunThemeMode.PEACH_SODA -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFFD95F3F),
            onPrimary = Color.White,
            secondary = Color(0xFFFF8F70),
            onSecondary = Color(0xFF421006),
            tertiary = Color(0xFFFFC857),
            onTertiary = Color(0xFF382800),
            background = Color(0xFFFFF3E7),
            onBackground = Color(0xFF2B1710),
            surface = Color(0xFFFFFBF7),
            onSurface = Color(0xFF2B1710),
            surfaceVariant = Color(0xFFFFDDCF),
            onSurfaceVariant = Color(0xFF644239),
            primaryContainer = Color(0xFFFFD5C8),
            onPrimaryContainer = Color(0xFF4A1005),
            secondaryContainer = Color(0xFFFFE2D3),
            onSecondaryContainer = Color(0xFF421006),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFFFF3E7),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFFFE3D2),
            readerOverlay = Color(0xCC080609),
            drawerHandle = Color(0x33D95F3F),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.MATCHA_MILK -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFF4F7D5B),
            onPrimary = Color.White,
            secondary = Color(0xFF8E7B42),
            onSecondary = Color.White,
            tertiary = Color(0xFFCE7F4B),
            onTertiary = Color(0xFF3B1600),
            background = Color(0xFFF7FAEF),
            onBackground = Color(0xFF172016),
            surface = Color(0xFFFFFDF8),
            onSurface = Color(0xFF172016),
            surfaceVariant = Color(0xFFDDE8D2),
            onSurfaceVariant = Color(0xFF3F4D3B),
            primaryContainer = Color(0xFFD7E7BF),
            onPrimaryContainer = Color(0xFF102514),
            secondaryContainer = Color(0xFFF0E3BA),
            onSecondaryContainer = Color(0xFF2B2105),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFF7FAEF),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFE5EFD6),
            readerOverlay = Color(0xCC050806),
            drawerHandle = Color(0x334F7D5B),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.STARRY_INK -> TankobunThemeSpec(
        colors = darkColorScheme(
            primary = Color(0xFF8FB6FF),
            onPrimary = Color(0xFF07152F),
            secondary = Color(0xFFFFD166),
            onSecondary = Color(0xFF322300),
            tertiary = Color(0xFF8CE6D2),
            onTertiary = Color(0xFF00382F),
            background = Color(0xFF0A1020),
            onBackground = Color(0xFFEAF0FF),
            surface = Color(0xFF10182A),
            onSurface = Color(0xFFEAF0FF),
            surfaceVariant = Color(0xFF23314A),
            onSurfaceVariant = Color(0xFFC7D3E8),
            primaryContainer = Color(0xFF244A85),
            onPrimaryContainer = Color(0xFFD9E7FF),
            secondaryContainer = Color(0xFF5A4210),
            onSecondaryContainer = Color(0xFFFFE7A7),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFF0A1020),
            elevatedSurface = Color(0xFF10182A),
            softAccent = Color(0xFF1A2944),
            readerOverlay = Color(0xDD000000),
            drawerHandle = Color(0x668FB6FF),
            coverScrim = Color(0x44000000),
        ),
    )
    TankobunThemeMode.PLUM_NIGHT -> TankobunThemeSpec(
        colors = darkColorScheme(
            primary = Color(0xFFE58AD8),
            onPrimary = Color(0xFF42113B),
            secondary = Color(0xFFA7E8BD),
            onSecondary = Color(0xFF0F351B),
            tertiary = Color(0xFFFFC48D),
            onTertiary = Color(0xFF3F2100),
            background = Color(0xFF170F1E),
            onBackground = Color(0xFFFFECFA),
            surface = Color(0xFF21162A),
            onSurface = Color(0xFFFFECFA),
            surfaceVariant = Color(0xFF49344F),
            onSurfaceVariant = Color(0xFFE7C8E8),
            primaryContainer = Color(0xFF6E2B66),
            onPrimaryContainer = Color(0xFFFFD6F8),
            secondaryContainer = Color(0xFF275038),
            onSecondaryContainer = Color(0xFFC8F9D8),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFF170F1E),
            elevatedSurface = Color(0xFF21162A),
            softAccent = Color(0xFF34223F),
            readerOverlay = Color(0xDD000000),
            drawerHandle = Color(0x66E58AD8),
            coverScrim = Color(0x44000000),
        ),
    )
    TankobunThemeMode.MIDNIGHT_RAMEN, TankobunThemeMode.DARK -> TankobunThemeSpec(
        colors = TankobunDarkColors,
        tokens = TankobunDarkTokens,
    )
    TankobunThemeMode.SYSTEM, TankobunThemeMode.BUNNY_MOCHI, TankobunThemeMode.LIGHT -> TankobunThemeSpec(
        colors = TankobunLightColors,
        tokens = TankobunLightTokens,
    )
}
