package com.tankobun.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
data class TankobunStyle(
    val colors: TankobunStyleColors,
    val radii: TankobunRadii = TankobunRadii(),
    val spacing: TankobunSpacing = TankobunSpacing(),
    val sizes: TankobunSizes = TankobunSizes(),
    val typography: TankobunTypography = TankobunTypography(),
)

@Immutable
data class TankobunStyleColors(
    val backdrop: Color,
    val panel: Color,
    val panelContent: Color,
    val accent: Color,
    val action: Color,
    val actionContent: Color,
    val mutedContent: Color,
    val chip: Color,
    val chipContent: Color,
    val selectedChip: Color,
    val selectedChipContent: Color,
    val outline: Color,
)

@Immutable
data class TankobunRadii(
    val control: Dp = 7.dp,
    val panel: Dp = 8.dp,
    val cover: Dp = 8.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class TankobunSpacing(
    val compactScreenPadding: Dp = 16.dp,
    val expandedScreenPadding: Dp = 20.dp,
    val section: Dp = 18.dp,
    val item: Dp = 12.dp,
    val dense: Dp = 8.dp,
)

@Immutable
data class TankobunSizes(
    val iconAction: Dp = 42.dp,
)

@Immutable
data class TankobunTypography(
    val displayFontFamily: FontFamily = TankobunDisplayFontFamily,
    val sectionLabel: TextStyle = TextStyle(
        fontFamily = TankobunDisplayFontFamily,
        fontSize = 20.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    val statNumber: TextStyle = TextStyle(
        fontFamily = TankobunDisplayFontFamily,
        fontSize = 34.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    val chapterTitle: TextStyle = TextStyle(
        fontFamily = TankobunDisplayFontFamily,
        fontSize = 28.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    ),
    val compactStatus: TextStyle = TextStyle(
        fontSize = 10.sp,
        lineHeight = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
    ),
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

val TankobunDisplayFontFamily = FontFamily(
    Font(R.font.bebas_neue_regular, FontWeight.Normal),
)

fun tankobunThemeChoices(): List<TankobunThemeChoice> = listOf(
    TankobunThemeChoice(
        mode = TankobunThemeMode.SYSTEM,
        name = "Bunny's Pick",
        description = "Follows your tablet",
        dark = null,
        swatches = listOf(Color(0xFF071B1D), Color(0xFFFFFFFF), Color(0xFFB82235)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.BUNNY_MOCHI,
        name = "Bunny Mochi",
        description = "White pages, berry ink",
        dark = false,
        swatches = listOf(Color(0xFFFFFFFF), Color(0xFFB82235), Color(0xFFFFDDE3)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.PEACH_SODA,
        name = "Peach Soda",
        description = "Bright sunset shelf",
        dark = false,
        swatches = listOf(Color(0xFFFFF0E5), Color(0xFFC84A2D), Color(0xFF00A19A)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.MATCHA_MILK,
        name = "Matcha Milk",
        description = "Leafy, calm, warm",
        dark = false,
        swatches = listOf(Color(0xFFF0F8E8), Color(0xFF2F7D4B), Color(0xFFC07A1A)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.SAKURA_MINT,
        name = "Sakura Mint",
        description = "Pink bloom, cool mint",
        dark = false,
        swatches = listOf(Color(0xFFFFEFF6), Color(0xFFB43D76), Color(0xFF008F7A)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.CLOUDBERRY_POP,
        name = "Cloudberry Pop",
        description = "White sky, blue pop",
        dark = false,
        swatches = listOf(Color(0xFFFFFFFF), Color(0xFF2F63C3), Color(0xFFD92265)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.YUZU_GARDEN,
        name = "Yuzu Garden",
        description = "Crisp citrus, teal, and leafy calm",
        dark = false,
        swatches = listOf(Color(0xFFFFFBE0), Color(0xFF008577), Color(0xFFD79800)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.CHARCOAL_GOLD,
        name = "Charcoal Gold",
        description = "Matte charcoal with warm gold ink",
        dark = true,
        swatches = listOf(Color(0xFF11100E), Color(0xFFE8B44D), Color(0xFF3A3428)),
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
    TankobunThemeChoice(
        mode = TankobunThemeMode.NEON_KOI,
        name = "Neon Koi",
        description = "Deep water with coral and cyan glow",
        dark = true,
        swatches = listOf(Color(0xFF071B1D), Color(0xFFFF6F61), Color(0xFF5EF2D6)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.MOON_JELLY,
        name = "Moon Jelly",
        description = "Soft midnight teal and lavender light",
        dark = true,
        swatches = listOf(Color(0xFF071720), Color(0xFFB8A7FF), Color(0xFF72E6FF)),
    ),
    TankobunThemeChoice(
        mode = TankobunThemeMode.INKBERRY_FIZZ,
        name = "Inkberry Fizz",
        description = "Berry-dark shelves with fizzy blue sparks",
        dark = true,
        swatches = listOf(Color(0xFF180B24), Color(0xFFFF6FB1), Color(0xFF5BD9FF)),
    ),
)

private val TankobunLightTokens = TankobunThemeTokens(
    appBackdrop = Color(0xFFFFFFFF),
    elevatedSurface = Color(0xFFFFFFFF),
    softAccent = Color(0xFFFFEEF1),
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

val LocalTankobunStyle = staticCompositionLocalOf {
    TankobunStyle(
        colors = TankobunStyleColors(
            backdrop = TankobunLightTokens.appBackdrop,
            panel = TankobunLightTokens.elevatedSurface,
            panelContent = TankobunLightColors.onSurface,
            accent = TankobunLightColors.primary,
            action = TankobunLightColors.secondary,
            actionContent = TankobunLightColors.onSecondary,
            mutedContent = TankobunLightColors.onSurfaceVariant,
            chip = TankobunLightColors.surface,
            chipContent = TankobunLightColors.onSurface,
            selectedChip = TankobunLightColors.primaryContainer,
            selectedChipContent = TankobunLightColors.onPrimaryContainer,
            outline = TankobunLightColors.outline,
        ),
    )
}

private val TankobunShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(7.dp),
    large = RoundedCornerShape(7.dp),
    extraLarge = RoundedCornerShape(7.dp),
)

private val TankobunLightColors = lightColorScheme(
    primary = Color(0xFFB82235),
    onPrimary = Color.White,
    secondary = Color(0xFFFF8A58),
    onSecondary = Color(0xFF2A0B10),
    tertiary = Color(0xFFFFD84D),
    onTertiary = Color(0xFF2D2300),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF241316),
    surface = Color(0xFFFFFBFA),
    onSurface = Color(0xFF241316),
    surfaceVariant = Color(0xFFFFE0DE),
    onSurfaceVariant = Color(0xFF5D403B),
    primaryContainer = Color(0xFFFFDDE3),
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
        TankobunThemeMode.SYSTEM -> if (systemDark) TankobunThemeMode.NEON_KOI else TankobunThemeMode.BUNNY_MOCHI
        TankobunThemeMode.LIGHT -> TankobunThemeMode.BUNNY_MOCHI
        TankobunThemeMode.DARK -> TankobunThemeMode.NEON_KOI
        else -> themeMode
    }
    val spec = themeSpecFor(resolvedMode)

    MaterialTheme(
        colorScheme = spec.colors,
        shapes = TankobunShapes,
        typography = MaterialTheme.typography,
    ) {
        val style = tankobunStyleFor(spec.colors, spec.tokens)
        CompositionLocalProvider(
            LocalTankobunTokens provides spec.tokens,
            LocalTankobunStyle provides style,
            content = content,
        )
    }
}

@Composable
private fun tankobunStyleFor(colors: ColorScheme, tokens: TankobunThemeTokens): TankobunStyle =
    TankobunStyle(
        colors = TankobunStyleColors(
            backdrop = tokens.appBackdrop,
            panel = tokens.elevatedSurface,
            panelContent = colors.onSurface,
            accent = colors.primary,
            action = colors.secondary,
            actionContent = colors.onSecondary,
            mutedContent = colors.onSurfaceVariant,
            chip = colors.surface,
            chipContent = colors.onSurface,
            selectedChip = colors.primaryContainer,
            selectedChipContent = colors.onPrimaryContainer,
            outline = colors.outline,
        ),
    )

private fun themeSpecFor(mode: TankobunThemeMode): TankobunThemeSpec = when (mode) {
    TankobunThemeMode.PEACH_SODA -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFFC84A2D),
            onPrimary = Color.White,
            secondary = Color(0xFF00A19A),
            onSecondary = Color(0xFF002F2C),
            tertiary = Color(0xFFFFB02E),
            onTertiary = Color(0xFF332000),
            background = Color(0xFFFFF0E5),
            onBackground = Color(0xFF2B1710),
            surface = Color(0xFFFFFAF6),
            onSurface = Color(0xFF2B1710),
            surfaceVariant = Color(0xFFFFD9C7),
            onSurfaceVariant = Color(0xFF644239),
            primaryContainer = Color(0xFFFFD2C0),
            onPrimaryContainer = Color(0xFF4A1005),
            secondaryContainer = Color(0xFFC7F3EF),
            onSecondaryContainer = Color(0xFF002F2C),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFFFF0E5),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFFFDEC9),
            readerOverlay = Color(0xCC080609),
            drawerHandle = Color(0x33C84A2D),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.MATCHA_MILK -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFF2F7D4B),
            onPrimary = Color.White,
            secondary = Color(0xFFC07A1A),
            onSecondary = Color(0xFF2F1B00),
            tertiary = Color(0xFF6B8F1A),
            onTertiary = Color(0xFF3B1600),
            background = Color(0xFFF0F8E8),
            onBackground = Color(0xFF172016),
            surface = Color(0xFFFCFFF8),
            onSurface = Color(0xFF172016),
            surfaceVariant = Color(0xFFD3E8CB),
            onSurfaceVariant = Color(0xFF3F4D3B),
            primaryContainer = Color(0xFFC9EBCF),
            onPrimaryContainer = Color(0xFF102514),
            secondaryContainer = Color(0xFFFFE4B4),
            onSecondaryContainer = Color(0xFF2B2105),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFF0F8E8),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFDDF0D4),
            readerOverlay = Color(0xCC050806),
            drawerHandle = Color(0x332F7D4B),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.SAKURA_MINT -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFFB43D76),
            onPrimary = Color.White,
            secondary = Color(0xFF008F7A),
            onSecondary = Color.White,
            tertiary = Color(0xFF5A67C8),
            onTertiary = Color.White,
            background = Color(0xFFFFEFF6),
            onBackground = Color(0xFF26151C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF26151C),
            surfaceVariant = Color(0xFFFFD6E6),
            onSurfaceVariant = Color(0xFF5A3D48),
            primaryContainer = Color(0xFFFFD1E3),
            onPrimaryContainer = Color(0xFF43111F),
            secondaryContainer = Color(0xFFBDEFE4),
            onSecondaryContainer = Color(0xFF003B34),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFFFEFF6),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFFFDDEC),
            readerOverlay = Color(0xCC090608),
            drawerHandle = Color(0x33B43D76),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.CLOUDBERRY_POP -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFF2F63C3),
            onPrimary = Color.White,
            secondary = Color(0xFFD92265),
            onSecondary = Color.White,
            tertiary = Color(0xFF00A4A0),
            onTertiary = Color.White,
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF111B2B),
            surface = Color(0xFFF9FBFF),
            onSurface = Color(0xFF111B2B),
            surfaceVariant = Color(0xFFDCE8FF),
            onSurfaceVariant = Color(0xFF3D4961),
            primaryContainer = Color(0xFFD5E3FF),
            onPrimaryContainer = Color(0xFF0A1E46),
            secondaryContainer = Color(0xFFFFD5E4),
            onSecondaryContainer = Color(0xFF4B0D21),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFFFFFFF),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFE5EEFF),
            readerOverlay = Color(0xCC03070D),
            drawerHandle = Color(0x332F63C3),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.YUZU_GARDEN -> TankobunThemeSpec(
        colors = lightColorScheme(
            primary = Color(0xFF008577),
            onPrimary = Color.White,
            secondary = Color(0xFFD79800),
            onSecondary = Color(0xFF332500),
            tertiary = Color(0xFF5C8A1F),
            onTertiary = Color.White,
            background = Color(0xFFFFFBE0),
            onBackground = Color(0xFF171E12),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF171E12),
            surfaceVariant = Color(0xFFF0E9B8),
            onSurfaceVariant = Color(0xFF465038),
            primaryContainer = Color(0xFFC2F0E9),
            onPrimaryContainer = Color(0xFF003B34),
            secondaryContainer = Color(0xFFFFE68A),
            onSecondaryContainer = Color(0xFF332500),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFFFFFBE0),
            elevatedSurface = Color(0xFFFFFFFF),
            softAccent = Color(0xFFFFF0A8),
            readerOverlay = Color(0xCC030704),
            drawerHandle = Color(0x33008577),
            coverScrim = Color(0x22000000),
        ),
    )
    TankobunThemeMode.CHARCOAL_GOLD -> TankobunThemeSpec(
        colors = darkColorScheme(
            primary = Color(0xFFE8B44D),
            onPrimary = Color(0xFF2A1C00),
            secondary = Color(0xFFD8C49A),
            onSecondary = Color(0xFF2A2112),
            tertiary = Color(0xFFFFD98A),
            onTertiary = Color(0xFF302000),
            background = Color(0xFF11100E),
            onBackground = Color(0xFFF7EFE0),
            surface = Color(0xFF1B1915),
            onSurface = Color(0xFFF7EFE0),
            surfaceVariant = Color(0xFF3A3428),
            onSurfaceVariant = Color(0xFFE0D3B8),
            primaryContainer = Color(0xFF5A4113),
            onPrimaryContainer = Color(0xFFFFE2A4),
            secondaryContainer = Color(0xFF403727),
            onSecondaryContainer = Color(0xFFF4E3C2),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFF11100E),
            elevatedSurface = Color(0xFF1B1915),
            softAccent = Color(0xFF29251D),
            readerOverlay = Color(0xDD000000),
            drawerHandle = Color(0x66E8B44D),
            coverScrim = Color(0x4D000000),
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
    TankobunThemeMode.NEON_KOI -> TankobunThemeSpec(
        colors = darkColorScheme(
            primary = Color(0xFFFF8A7D),
            onPrimary = Color(0xFF4B0B05),
            secondary = Color(0xFF5EF2D6),
            onSecondary = Color(0xFF003C35),
            tertiary = Color(0xFFFFD166),
            onTertiary = Color(0xFF332300),
            background = Color(0xFF071B1D),
            onBackground = Color(0xFFE7FEFA),
            surface = Color(0xFF0E272A),
            onSurface = Color(0xFFE7FEFA),
            surfaceVariant = Color(0xFF234245),
            onSurfaceVariant = Color(0xFFB8D8D4),
            primaryContainer = Color(0xFF7C261F),
            onPrimaryContainer = Color(0xFFFFD7D1),
            secondaryContainer = Color(0xFF0D5B52),
            onSecondaryContainer = Color(0xFFC8FFF4),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFF071B1D),
            elevatedSurface = Color(0xFF0E272A),
            softAccent = Color(0xFF17373A),
            readerOverlay = Color(0xDD000000),
            drawerHandle = Color(0x66FF8A7D),
            coverScrim = Color(0x44000000),
        ),
    )
    TankobunThemeMode.MOON_JELLY -> TankobunThemeSpec(
        colors = darkColorScheme(
            primary = Color(0xFFB8A7FF),
            onPrimary = Color(0xFF261558),
            secondary = Color(0xFF72E6FF),
            onSecondary = Color(0xFF003743),
            tertiary = Color(0xFFFF9FCB),
            onTertiary = Color(0xFF4B1230),
            background = Color(0xFF071720),
            onBackground = Color(0xFFEAF7FF),
            surface = Color(0xFF0E202B),
            onSurface = Color(0xFFEAF7FF),
            surfaceVariant = Color(0xFF253847),
            onSurfaceVariant = Color(0xFFC3D5E2),
            primaryContainer = Color(0xFF46347F),
            onPrimaryContainer = Color(0xFFE6DFFF),
            secondaryContainer = Color(0xFF0D5464),
            onSecondaryContainer = Color(0xFFC7F6FF),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFF071720),
            elevatedSurface = Color(0xFF0E202B),
            softAccent = Color(0xFF172C39),
            readerOverlay = Color(0xDD000000),
            drawerHandle = Color(0x66B8A7FF),
            coverScrim = Color(0x44000000),
        ),
    )
    TankobunThemeMode.INKBERRY_FIZZ -> TankobunThemeSpec(
        colors = darkColorScheme(
            primary = Color(0xFFFF6FB1),
            onPrimary = Color(0xFF4C0A2C),
            secondary = Color(0xFF5BD9FF),
            onSecondary = Color(0xFF003646),
            tertiary = Color(0xFFA8F0A1),
            onTertiary = Color(0xFF123A13),
            background = Color(0xFF180B24),
            onBackground = Color(0xFFFFECFF),
            surface = Color(0xFF241332),
            onSurface = Color(0xFFFFECFF),
            surfaceVariant = Color(0xFF463255),
            onSurfaceVariant = Color(0xFFE3C9ED),
            primaryContainer = Color(0xFF74254E),
            onPrimaryContainer = Color(0xFFFFD6E9),
            secondaryContainer = Color(0xFF0C5268),
            onSecondaryContainer = Color(0xFFC6F1FF),
        ),
        tokens = TankobunThemeTokens(
            appBackdrop = Color(0xFF180B24),
            elevatedSurface = Color(0xFF241332),
            softAccent = Color(0xFF352044),
            readerOverlay = Color(0xDD000000),
            drawerHandle = Color(0x66FF6FB1),
            coverScrim = Color(0x44000000),
        ),
    )
    TankobunThemeMode.MIDNIGHT_RAMEN, TankobunThemeMode.DARK -> themeSpecFor(TankobunThemeMode.NEON_KOI)
    TankobunThemeMode.SYSTEM, TankobunThemeMode.BUNNY_MOCHI, TankobunThemeMode.LIGHT -> TankobunThemeSpec(
        colors = TankobunLightColors,
        tokens = TankobunLightTokens,
    )
}
