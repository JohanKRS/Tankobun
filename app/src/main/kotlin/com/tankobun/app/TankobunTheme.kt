package com.tankobun.app

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TankobunArtDirection {
    ORIGINAL,
    STORYBOOK,
    MOCHI_POP,
    PANEL_RIOT,
    NOIR_ATELIER,
    NEON_CURRENT,
}

enum class TankobunPaletteId {
    SUMI,
    SAKURA,
    MATCHA,
    AIZOME,
    YUZU,
    AMBAR,
    AMEIXA,
    NEON,
    RETICULA,
    DYNAMIC,

    // Retired palettes. They stay parseable so stored preferences and older backups migrate.
    MATCHA_MEADOW,
    PEACH_COUNTRYSIDE,
    YUZU_GARDEN,
    BUNNY_BERRY,
    SAKURA_MINT,
    CLOUDBERRY,
    REDLINE,
    ELECTRIC_BERRY,
    CITRUS_CLASH,
    CHARCOAL_GOLD,
    VELVET_PLUM,
    STARRY_INK,
    NEON_KOI,
    MOON_JELLY,
    ACID_AURORA,
}

enum class TankobunColorMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Immutable
data class TankobunThemePreference(
    val mode: TankobunColorMode = TankobunColorMode.SYSTEM,
    val direction: TankobunArtDirection = TankobunArtDirection.MOCHI_POP,
    val palette: TankobunPaletteId = TankobunPaletteId.SUMI,
    val pureBlack: Boolean = false,
) {
    fun normalized(): TankobunThemePreference = copy(
        direction = when (direction) {
            TankobunArtDirection.MOCHI_POP -> TankobunArtDirection.MOCHI_POP
            else -> TankobunArtDirection.ORIGINAL
        },
        palette = palette.retiredReplacement()?.first ?: palette,
    )
}

@Immutable
data class TankobunArtDirectionChoice(
    val id: TankobunArtDirection,
    val name: String,
    val description: String,
)

@Immutable
data class TankobunPaletteChoice(
    val id: TankobunPaletteId,
    val lightSwatches: List<Color>,
    val darkSwatches: List<Color>,
)

val TankobunVisiblePalettes = listOf(
    TankobunPaletteId.SUMI,
    TankobunPaletteId.SAKURA,
    TankobunPaletteId.MATCHA,
    TankobunPaletteId.AIZOME,
    TankobunPaletteId.YUZU,
    TankobunPaletteId.AMBAR,
    TankobunPaletteId.AMEIXA,
    TankobunPaletteId.NEON,
    TankobunPaletteId.RETICULA,
)

fun dynamicColorAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

fun tankobunArtDirectionChoices(): List<TankobunArtDirectionChoice> = listOf(
    TankobunArtDirectionChoice(TankobunArtDirection.ORIGINAL, "Defined", "Light rounding with defined corners"),
    TankobunArtDirectionChoice(TankobunArtDirection.MOCHI_POP, "Rounded", "Soft curves across controls and surfaces"),
)

fun TankobunArtDirection.themeNameRes(): Int = when (this) {
    TankobunArtDirection.MOCHI_POP -> R.string.theme_direction_soft
    else -> R.string.theme_direction_original
}

fun TankobunArtDirection.themeDescriptionRes(): Int = when (this) {
    TankobunArtDirection.MOCHI_POP -> R.string.theme_direction_soft_desc
    else -> R.string.theme_direction_original_desc
}

fun TankobunPaletteId.themeNameRes(): Int = when (normalizedPalette()) {
    TankobunPaletteId.SAKURA -> R.string.theme_palette_sakura
    TankobunPaletteId.MATCHA -> R.string.theme_palette_matcha
    TankobunPaletteId.AIZOME -> R.string.theme_palette_aizome
    TankobunPaletteId.YUZU -> R.string.theme_palette_yuzu
    TankobunPaletteId.AMBAR -> R.string.theme_palette_ambar
    TankobunPaletteId.AMEIXA -> R.string.theme_palette_ameixa
    TankobunPaletteId.NEON -> R.string.theme_palette_neon
    TankobunPaletteId.RETICULA -> R.string.theme_palette_reticula
    TankobunPaletteId.DYNAMIC -> R.string.theme_palette_dynamic
    else -> R.string.theme_palette_sumi
}

fun TankobunPaletteId.themeDescriptionRes(): Int = when (normalizedPalette()) {
    TankobunPaletteId.SAKURA -> R.string.theme_palette_sakura_desc
    TankobunPaletteId.MATCHA -> R.string.theme_palette_matcha_desc
    TankobunPaletteId.AIZOME -> R.string.theme_palette_aizome_desc
    TankobunPaletteId.YUZU -> R.string.theme_palette_yuzu_desc
    TankobunPaletteId.AMBAR -> R.string.theme_palette_ambar_desc
    TankobunPaletteId.AMEIXA -> R.string.theme_palette_ameixa_desc
    TankobunPaletteId.NEON -> R.string.theme_palette_neon_desc
    TankobunPaletteId.RETICULA -> R.string.theme_palette_reticula_desc
    TankobunPaletteId.DYNAMIC -> R.string.theme_palette_dynamic_desc
    else -> R.string.theme_palette_sumi_desc
}

fun TankobunColorMode.themeNameRes(): Int = when (this) {
    TankobunColorMode.SYSTEM -> R.string.theme_mode_system
    TankobunColorMode.LIGHT -> R.string.common_light
    TankobunColorMode.DARK -> R.string.common_dark
}

fun tankobunPaletteChoices(): List<TankobunPaletteChoice> =
    TankobunVisiblePalettes.map { id ->
        val light = generatedColorScheme(id, dark = false) ?: SumiFallback(false)
        val dark = generatedColorScheme(id, dark = true) ?: SumiFallback(true)
        TankobunPaletteChoice(
            id = id,
            lightSwatches = listOf(light.background, light.primary, light.secondaryContainer, light.tertiary),
            darkSwatches = listOf(dark.background, dark.primary, dark.secondaryContainer, dark.tertiary),
        )
    }

fun tankobunThemeShapeSet(direction: TankobunArtDirection): ThemeShapeSet =
    directionSpec(direction).shapes

private fun TankobunPaletteId.normalizedPalette(): TankobunPaletteId = retiredReplacement()?.first ?: this

/** Retired palettes map to the closest new theme and the light or dark mode they always had. */
private fun TankobunPaletteId.retiredReplacement(): Pair<TankobunPaletteId, TankobunColorMode>? = when (this) {
    TankobunPaletteId.MATCHA_MEADOW -> TankobunPaletteId.MATCHA to TankobunColorMode.LIGHT
    TankobunPaletteId.PEACH_COUNTRYSIDE,
    TankobunPaletteId.BUNNY_BERRY,
    TankobunPaletteId.REDLINE -> TankobunPaletteId.SUMI to TankobunColorMode.LIGHT
    TankobunPaletteId.YUZU_GARDEN,
    TankobunPaletteId.CITRUS_CLASH -> TankobunPaletteId.YUZU to TankobunColorMode.LIGHT
    TankobunPaletteId.SAKURA_MINT -> TankobunPaletteId.SAKURA to TankobunColorMode.LIGHT
    TankobunPaletteId.CLOUDBERRY -> TankobunPaletteId.AIZOME to TankobunColorMode.LIGHT
    TankobunPaletteId.ELECTRIC_BERRY,
    TankobunPaletteId.NEON_KOI,
    TankobunPaletteId.ACID_AURORA -> TankobunPaletteId.NEON to TankobunColorMode.DARK
    TankobunPaletteId.CHARCOAL_GOLD -> TankobunPaletteId.AMBAR to TankobunColorMode.DARK
    TankobunPaletteId.VELVET_PLUM -> TankobunPaletteId.AMEIXA to TankobunColorMode.DARK
    TankobunPaletteId.STARRY_INK,
    TankobunPaletteId.MOON_JELLY -> TankobunPaletteId.AIZOME to TankobunColorMode.DARK
    else -> null
}

/**
 * Converts a preference saved before themes had separate light and dark modes. Automatic
 * preferences used to switch between two fixed palettes; they now follow the system with the
 * default theme. A manually chosen palette keeps the mode it always had.
 */
fun migratedThemePreference(
    automatic: Boolean,
    direction: TankobunArtDirection,
    palette: TankobunPaletteId,
): TankobunThemePreference {
    if (automatic) return TankobunThemePreference(direction = direction).normalized()
    val replacement = palette.retiredReplacement()
    return TankobunThemePreference(
        mode = replacement?.second ?: TankobunColorMode.SYSTEM,
        direction = direction,
        palette = replacement?.first ?: palette,
    ).normalized()
}

fun legacyThemePreference(mode: TankobunThemeMode): TankobunThemePreference {
    fun light(palette: TankobunPaletteId, direction: TankobunArtDirection = TankobunArtDirection.ORIGINAL) =
        TankobunThemePreference(TankobunColorMode.LIGHT, direction, palette)
    fun dark(palette: TankobunPaletteId) =
        TankobunThemePreference(TankobunColorMode.DARK, TankobunArtDirection.ORIGINAL, palette)
    return when (mode) {
        TankobunThemeMode.SYSTEM -> TankobunThemePreference()
        TankobunThemeMode.LIGHT,
        TankobunThemeMode.BUNNY_MOCHI -> light(TankobunPaletteId.SUMI, TankobunArtDirection.MOCHI_POP)
        TankobunThemeMode.PEACH_SODA -> light(TankobunPaletteId.SUMI)
        TankobunThemeMode.MATCHA_MILK -> light(TankobunPaletteId.MATCHA)
        TankobunThemeMode.SAKURA_MINT -> light(TankobunPaletteId.SAKURA, TankobunArtDirection.MOCHI_POP)
        TankobunThemeMode.CLOUDBERRY_POP -> light(TankobunPaletteId.AIZOME, TankobunArtDirection.MOCHI_POP)
        TankobunThemeMode.YUZU_GARDEN -> light(TankobunPaletteId.YUZU)
        TankobunThemeMode.INKBERRY_FIZZ -> dark(TankobunPaletteId.NEON)
        TankobunThemeMode.CHARCOAL_GOLD -> dark(TankobunPaletteId.AMBAR)
        TankobunThemeMode.PLUM_NIGHT -> dark(TankobunPaletteId.AMEIXA)
        TankobunThemeMode.STARRY_INK,
        TankobunThemeMode.MOON_JELLY -> dark(TankobunPaletteId.AIZOME)
        TankobunThemeMode.DARK,
        TankobunThemeMode.MIDNIGHT_RAMEN,
        TankobunThemeMode.NEON_KOI -> dark(TankobunPaletteId.NEON)
    }
}

/** The closest pre-redesign mode, written to backups so older app versions can still restore something sensible. */
fun TankobunThemePreference.toLegacyThemeMode(): TankobunThemeMode {
    val normalized = normalized()
    return when (normalized.mode) {
        TankobunColorMode.SYSTEM -> TankobunThemeMode.SYSTEM
        TankobunColorMode.LIGHT -> when (normalized.palette) {
            TankobunPaletteId.SUMI -> TankobunThemeMode.PEACH_SODA
            TankobunPaletteId.SAKURA -> TankobunThemeMode.SAKURA_MINT
            TankobunPaletteId.MATCHA -> TankobunThemeMode.MATCHA_MILK
            TankobunPaletteId.AIZOME -> TankobunThemeMode.CLOUDBERRY_POP
            TankobunPaletteId.YUZU -> TankobunThemeMode.YUZU_GARDEN
            else -> TankobunThemeMode.LIGHT
        }
        TankobunColorMode.DARK -> when (normalized.palette) {
            TankobunPaletteId.AMBAR -> TankobunThemeMode.CHARCOAL_GOLD
            TankobunPaletteId.AMEIXA -> TankobunThemeMode.PLUM_NIGHT
            TankobunPaletteId.AIZOME -> TankobunThemeMode.STARRY_INK
            TankobunPaletteId.NEON -> TankobunThemeMode.NEON_KOI
            else -> TankobunThemeMode.DARK
        }
    }
}

fun TankobunThemePreference.isDark(systemDark: Boolean): Boolean = when (normalized().mode) {
    TankobunColorMode.SYSTEM -> systemDark
    TankobunColorMode.LIGHT -> false
    TankobunColorMode.DARK -> true
}

/**
 * The full Material color scheme for a preference. Material You needs a [context] and Android 12;
 * without either it falls back to the default theme.
 */
fun tankobunColorScheme(
    preference: TankobunThemePreference,
    dark: Boolean,
    context: Context? = null,
): ColorScheme {
    val normalized = preference.normalized()
    val base = if (normalized.palette == TankobunPaletteId.DYNAMIC && context != null && dynamicColorAvailable()) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        generatedColorScheme(normalized.palette, dark) ?: SumiFallback(dark)
    }
    return if (dark && normalized.pureBlack) base.withPureBlack() else base
}

@Suppress("FunctionName")
private fun SumiFallback(dark: Boolean): ColorScheme =
    checkNotNull(generatedColorScheme(TankobunPaletteId.SUMI, dark))

/** Moves every background surface toward true black while keeping the theme's hue in raised layers. */
private fun ColorScheme.withPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = lerp(surfaceContainerLow, Color.Black, 0.55f),
    surfaceContainer = lerp(surfaceContainer, Color.Black, 0.5f),
    surfaceContainerHigh = lerp(surfaceContainerHigh, Color.Black, 0.45f),
    surfaceContainerHighest = lerp(surfaceContainerHighest, Color.Black, 0.4f),
    surfaceVariant = lerp(surfaceVariant, Color.Black, 0.3f),
)

@Immutable
data class ThemeShapeSet(
    val panel: CornerBasedShape,
    val control: CornerBasedShape,
    val chip: CornerBasedShape,
    val dialog: CornerBasedShape,
    val dock: CornerBasedShape,
    val cover: CornerBasedShape,
    val indicator: CornerBasedShape,
)

@Immutable
data class ThemeStrokeSet(
    val defaultWidth: Dp,
    val emphasizedWidth: Dp,
    val hardShadow: Boolean,
)

@Immutable
data class ThemeMotionSet(
    val pressScale: Float,
    val durationMillis: Int,
    val springy: Boolean,
)

@Immutable
data class TankobunThemeTokens(
    val dark: Boolean,
    val appBackdrop: Color,
    val elevatedSurface: Color,
    val softAccent: Color,
    val readerOverlay: Color,
    val coverScrim: Color,
    val topBarSurface: Color,
    val topBarBleed: Color,
    val dockSurface: Color,
    val dockBleed: Color,
    val glassOutline: Color,
    val glassHighlight: Color,
    val glassShadow: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

@Immutable
data class TankobunStyle(
    val direction: TankobunArtDirection,
    val colors: TankobunStyleColors,
    val themeShapes: ThemeShapeSet,
    val strokes: ThemeStrokeSet,
    val motion: ThemeMotionSet,
    val radii: TankobunRadii,
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
    val control: Dp,
    val panel: Dp,
    val cover: Dp,
    val pill: Dp = 999.dp,
)

@Immutable data class TankobunSpacing(
    val compactScreenPadding: Dp = 16.dp,
    val expandedScreenPadding: Dp = 20.dp,
    val section: Dp = 18.dp,
    val item: Dp = 12.dp,
    val dense: Dp = 8.dp,
)

@Immutable data class TankobunSizes(val iconAction: Dp = 44.dp)

@Immutable
data class TankobunTypography(
    val displayFontFamily: FontFamily = TankobunDisplayFontFamily,
    val sectionLabel: TextStyle = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 20.sp, lineHeight = 22.sp),
    val statNumber: TextStyle = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 34.sp, lineHeight = 36.sp),
    val chapterTitle: TextStyle = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 28.sp, lineHeight = 30.sp),
    val compactStatus: TextStyle = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
)

val TankobunDisplayFontFamily = FontFamily(Font(R.font.bebas_neue_regular, FontWeight.Normal))

private data class DirectionSpec(
    val shapes: ThemeShapeSet,
    val strokes: ThemeStrokeSet,
    val motion: ThemeMotionSet,
    val materialShapes: Shapes,
    val radii: TankobunRadii,
)

private fun directionSpec(direction: TankobunArtDirection): DirectionSpec {
    val rounded = direction == TankobunArtDirection.MOCHI_POP
    val shapes = if (rounded) {
        ThemeShapeSet(
            panel = RoundedCornerShape(18.dp), control = RoundedCornerShape(14.dp),
            chip = RoundedCornerShape(999.dp), dialog = RoundedCornerShape(28.dp),
            dock = RoundedCornerShape(999.dp), cover = RoundedCornerShape(14.dp), indicator = RoundedCornerShape(999.dp),
        )
    } else {
        ThemeShapeSet(
            panel = RoundedCornerShape(8.dp), control = RoundedCornerShape(7.dp),
            chip = RoundedCornerShape(7.dp), dialog = RoundedCornerShape(8.dp),
            dock = RoundedCornerShape(999.dp), cover = RoundedCornerShape(8.dp), indicator = RoundedCornerShape(7.dp),
        )
    }
    return DirectionSpec(
        shapes = shapes,
        strokes = ThemeStrokeSet(1.dp, 1.5.dp, false),
        motion = ThemeMotionSet(0.96f, 190, true),
        materialShapes = Shapes(
            extraSmall = shapes.control,
            small = shapes.control,
            medium = shapes.panel,
            large = shapes.dialog,
            extraLarge = shapes.dialog,
        ),
        radii = if (rounded) TankobunRadii(14.dp, 18.dp, 14.dp) else TankobunRadii(7.dp, 8.dp, 8.dp),
    )
}

private fun ColorScheme.tokens(dark: Boolean): TankobunThemeTokens = TankobunThemeTokens(
    dark = dark,
    appBackdrop = background,
    elevatedSurface = surfaceContainerLow,
    softAccent = primaryContainer,
    readerOverlay = Color(0xE6000000),
    coverScrim = Color.Black.copy(alpha = if (dark) 0.30f else 0.13f),
    topBarSurface = surfaceContainer,
    topBarBleed = primaryContainer,
    dockSurface = surfaceContainerHigh,
    dockBleed = primaryContainer,
    glassOutline = if (dark) Color.White.copy(alpha = 0.10f) else outlineVariant.copy(alpha = 0.6f),
    glassHighlight = Color.White.copy(alpha = if (dark) 0.10f else 0.65f),
    glassShadow = if (dark) Color.Black.copy(alpha = 0.7f) else lerp(primary, Color.Black, 0.5f).copy(alpha = 0.38f),
    gradientStart = lerp(background, primaryContainer, if (dark) 0.18f else 0.28f),
    gradientEnd = lerp(background, secondaryContainer, if (dark) 0.22f else 0.34f),
)

private fun ColorScheme.styleColors(): TankobunStyleColors = TankobunStyleColors(
    backdrop = background,
    panel = surfaceContainerLow,
    panelContent = onSurface,
    accent = primary,
    action = primary,
    actionContent = onPrimary,
    mutedContent = onSurfaceVariant,
    chip = surfaceContainerHigh,
    chipContent = onSurface,
    selectedChip = secondaryContainer,
    selectedChipContent = onSecondaryContainer,
    outline = outline,
)

private val DefaultPreference = TankobunThemePreference()
private val DefaultColors = tankobunColorScheme(DefaultPreference, dark = false)
private val DefaultDirection = directionSpec(DefaultPreference.direction)

val LocalTankobunTokens = staticCompositionLocalOf { DefaultColors.tokens(dark = false) }
val LocalTankobunStyle = staticCompositionLocalOf {
    TankobunStyle(
        direction = DefaultPreference.direction,
        colors = DefaultColors.styleColors(),
        themeShapes = DefaultDirection.shapes,
        strokes = DefaultDirection.strokes,
        motion = DefaultDirection.motion,
        radii = DefaultDirection.radii,
    )
}

@Composable
fun TankobunTheme(
    preference: TankobunThemePreference,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val normalized = preference.normalized()
    val dark = normalized.isDark(isSystemInDarkTheme())
    val colors = remember(normalized, dark, context) { tankobunColorScheme(normalized, dark, context) }
    val direction = remember(normalized.direction) { directionSpec(normalized.direction) }
    val style = TankobunStyle(
        direction = normalized.direction,
        colors = colors.styleColors(),
        themeShapes = direction.shapes,
        strokes = direction.strokes,
        motion = direction.motion,
        radii = direction.radii,
    )
    MaterialTheme(colorScheme = colors, shapes = direction.materialShapes) {
        CompositionLocalProvider(
            LocalTankobunTokens provides colors.tokens(dark),
            LocalTankobunStyle provides style,
            content = content,
        )
    }
}
