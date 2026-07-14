package com.tankobun.app

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

@Immutable
data class TankobunThemePreference(
    val automatic: Boolean = true,
    val direction: TankobunArtDirection = TankobunArtDirection.MOCHI_POP,
    val palette: TankobunPaletteId = TankobunPaletteId.BUNNY_BERRY,
) {
    fun normalized(): TankobunThemePreference = copy(
        direction = when (direction) {
            TankobunArtDirection.MOCHI_POP -> TankobunArtDirection.MOCHI_POP
            else -> TankobunArtDirection.ORIGINAL
        },
        palette = when (palette) {
            TankobunPaletteId.CITRUS_CLASH -> TankobunPaletteId.YUZU_GARDEN
            else -> palette
        },
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
    val name: String,
    val dark: Boolean,
    val swatches: List<Color>,
)

fun tankobunArtDirectionChoices(): List<TankobunArtDirectionChoice> = listOf(
    TankobunArtDirectionChoice(TankobunArtDirection.ORIGINAL, "Defined", "Light rounding with defined corners"),
    TankobunArtDirectionChoice(TankobunArtDirection.MOCHI_POP, "Rounded", "Soft curves across controls and surfaces"),
)

fun TankobunArtDirection.themeNameRes(): Int = when (this) {
    TankobunArtDirection.ORIGINAL,
    TankobunArtDirection.STORYBOOK,
    TankobunArtDirection.PANEL_RIOT,
    TankobunArtDirection.NOIR_ATELIER,
    TankobunArtDirection.NEON_CURRENT -> R.string.theme_direction_original
    TankobunArtDirection.MOCHI_POP -> R.string.theme_direction_soft
}

fun TankobunArtDirection.themeDescriptionRes(): Int = when (this) {
    TankobunArtDirection.ORIGINAL,
    TankobunArtDirection.STORYBOOK,
    TankobunArtDirection.PANEL_RIOT,
    TankobunArtDirection.NOIR_ATELIER,
    TankobunArtDirection.NEON_CURRENT -> R.string.theme_direction_original_desc
    TankobunArtDirection.MOCHI_POP -> R.string.theme_direction_soft_desc
}

fun TankobunPaletteId.themeNameRes(): Int = when (this) {
    TankobunPaletteId.MATCHA_MEADOW -> R.string.theme_palette_matcha
    TankobunPaletteId.PEACH_COUNTRYSIDE -> R.string.theme_palette_peach
    TankobunPaletteId.YUZU_GARDEN -> R.string.theme_palette_yuzu
    TankobunPaletteId.BUNNY_BERRY -> R.string.theme_palette_berry
    TankobunPaletteId.SAKURA_MINT -> R.string.theme_palette_sakura
    TankobunPaletteId.CLOUDBERRY -> R.string.theme_palette_cobalt
    TankobunPaletteId.REDLINE -> R.string.theme_palette_redline
    TankobunPaletteId.ELECTRIC_BERRY -> R.string.theme_palette_ultraviolet
    TankobunPaletteId.CITRUS_CLASH -> R.string.theme_palette_citrus
    TankobunPaletteId.CHARCOAL_GOLD -> R.string.theme_palette_charcoal
    TankobunPaletteId.VELVET_PLUM -> R.string.theme_palette_plum
    TankobunPaletteId.STARRY_INK -> R.string.theme_palette_ink
    TankobunPaletteId.NEON_KOI -> R.string.theme_palette_koi
    TankobunPaletteId.MOON_JELLY -> R.string.theme_palette_moonlight
    TankobunPaletteId.ACID_AURORA -> R.string.theme_palette_aurora
}

fun tankobunPaletteChoices(): List<TankobunPaletteChoice> =
    PaletteCatalog.values
        .filterNot { it.id == TankobunPaletteId.CITRUS_CLASH }
        .map { it.choice() }

fun tankobunPaletteChoice(id: TankobunPaletteId): TankobunPaletteChoice =
    PaletteCatalog.value(id).choice()

internal fun tankobunColorScheme(id: TankobunPaletteId): ColorScheme =
    PaletteCatalog.value(id).colorScheme()

fun tankobunThemeShapeSet(direction: TankobunArtDirection): ThemeShapeSet =
    directionSpec(direction).shapes

fun legacyThemePreference(mode: TankobunThemeMode): TankobunThemePreference = when (mode) {
    TankobunThemeMode.SYSTEM -> TankobunThemePreference()
    TankobunThemeMode.LIGHT,
    TankobunThemeMode.BUNNY_MOCHI -> TankobunThemePreference(false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.BUNNY_BERRY)
    TankobunThemeMode.PEACH_SODA -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.PEACH_COUNTRYSIDE)
    TankobunThemeMode.MATCHA_MILK -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.MATCHA_MEADOW)
    TankobunThemeMode.SAKURA_MINT -> TankobunThemePreference(false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.SAKURA_MINT)
    TankobunThemeMode.CLOUDBERRY_POP -> TankobunThemePreference(false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.CLOUDBERRY)
    TankobunThemeMode.YUZU_GARDEN -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.YUZU_GARDEN)
    TankobunThemeMode.INKBERRY_FIZZ -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.ELECTRIC_BERRY)
    TankobunThemeMode.CHARCOAL_GOLD -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.CHARCOAL_GOLD)
    TankobunThemeMode.PLUM_NIGHT -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.VELVET_PLUM)
    TankobunThemeMode.STARRY_INK -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.STARRY_INK)
    TankobunThemeMode.DARK,
    TankobunThemeMode.MIDNIGHT_RAMEN,
    TankobunThemeMode.NEON_KOI -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.NEON_KOI)
    TankobunThemeMode.MOON_JELLY -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.MOON_JELLY)
}

fun TankobunThemePreference.toLegacyThemeMode(): TankobunThemeMode {
    if (automatic) return TankobunThemeMode.SYSTEM
    return when (normalized().palette) {
        TankobunPaletteId.MATCHA_MEADOW -> TankobunThemeMode.MATCHA_MILK
        TankobunPaletteId.PEACH_COUNTRYSIDE -> TankobunThemeMode.PEACH_SODA
        TankobunPaletteId.YUZU_GARDEN -> TankobunThemeMode.YUZU_GARDEN
        TankobunPaletteId.BUNNY_BERRY -> TankobunThemeMode.BUNNY_MOCHI
        TankobunPaletteId.SAKURA_MINT -> TankobunThemeMode.SAKURA_MINT
        TankobunPaletteId.CLOUDBERRY -> TankobunThemeMode.CLOUDBERRY_POP
        TankobunPaletteId.REDLINE,
        TankobunPaletteId.CITRUS_CLASH -> TankobunThemeMode.BUNNY_MOCHI
        TankobunPaletteId.ELECTRIC_BERRY -> TankobunThemeMode.INKBERRY_FIZZ
        TankobunPaletteId.CHARCOAL_GOLD -> TankobunThemeMode.CHARCOAL_GOLD
        TankobunPaletteId.VELVET_PLUM -> TankobunThemeMode.PLUM_NIGHT
        TankobunPaletteId.STARRY_INK -> TankobunThemeMode.STARRY_INK
        TankobunPaletteId.NEON_KOI -> TankobunThemeMode.NEON_KOI
        TankobunPaletteId.MOON_JELLY -> TankobunThemeMode.MOON_JELLY
        TankobunPaletteId.ACID_AURORA -> TankobunThemeMode.NEON_KOI
    }
}

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
    val appBackdrop: Color,
    val elevatedSurface: Color,
    val softAccent: Color,
    val readerOverlay: Color,
    val drawerHandle: Color,
    val coverScrim: Color,
    val topBarSurface: Color,
    val topBarBleed: Color,
    val dockSurface: Color,
    val dockBleed: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val glow: Color,
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

@Immutable data class TankobunSizes(val iconAction: Dp = 42.dp)

@Immutable
data class TankobunTypography(
    val displayFontFamily: FontFamily = TankobunDisplayFontFamily,
    val sectionLabel: TextStyle = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 20.sp, lineHeight = 20.sp),
    val statNumber: TextStyle = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 34.sp, lineHeight = 34.sp),
    val chapterTitle: TextStyle = TextStyle(fontFamily = TankobunDisplayFontFamily, fontSize = 28.sp, lineHeight = 28.sp),
    val compactStatus: TextStyle = TextStyle(fontSize = 10.sp, lineHeight = 10.sp, fontWeight = FontWeight.Bold),
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
    val shapes = when (direction) {
        TankobunArtDirection.ORIGINAL -> ThemeShapeSet(
            panel = RoundedCornerShape(8.dp), control = RoundedCornerShape(7.dp),
            chip = RoundedCornerShape(7.dp), dialog = RoundedCornerShape(8.dp),
            dock = RoundedCornerShape(999.dp), cover = RoundedCornerShape(8.dp), indicator = RoundedCornerShape(7.dp),
        )
        TankobunArtDirection.STORYBOOK -> ThemeShapeSet(
            panel = RoundedCornerShape(18.dp, 10.dp, 18.dp, 8.dp),
            control = RoundedCornerShape(14.dp, 8.dp, 14.dp, 8.dp),
            chip = RoundedCornerShape(16.dp, 10.dp, 16.dp, 10.dp),
            dialog = RoundedCornerShape(24.dp, 14.dp, 24.dp, 14.dp),
            dock = RoundedCornerShape(24.dp, 18.dp, 24.dp, 18.dp),
            cover = RoundedCornerShape(12.dp, 7.dp, 12.dp, 7.dp),
            indicator = RoundedCornerShape(14.dp, 8.dp, 14.dp, 8.dp),
        )
        TankobunArtDirection.MOCHI_POP -> ThemeShapeSet(
            panel = RoundedCornerShape(18.dp), control = RoundedCornerShape(14.dp),
            chip = RoundedCornerShape(999.dp), dialog = RoundedCornerShape(28.dp),
            dock = RoundedCornerShape(999.dp), cover = RoundedCornerShape(14.dp), indicator = RoundedCornerShape(999.dp),
        )
        TankobunArtDirection.PANEL_RIOT -> ThemeShapeSet(
            panel = CutCornerShape(10.dp), control = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
            chip = CutCornerShape(6.dp), dialog = CutCornerShape(16.dp), dock = CutCornerShape(14.dp),
            cover = CutCornerShape(5.dp), indicator = CutCornerShape(8.dp),
        )
        TankobunArtDirection.NOIR_ATELIER -> ThemeShapeSet(
            panel = CutCornerShape(4.dp), control = CutCornerShape(6.dp), chip = CutCornerShape(4.dp),
            dialog = CutCornerShape(10.dp), dock = RoundedCornerShape(14.dp), cover = CutCornerShape(3.dp), indicator = CutCornerShape(5.dp),
        )
        TankobunArtDirection.NEON_CURRENT -> ThemeShapeSet(
            panel = RoundedCornerShape(14.dp), control = RoundedCornerShape(11.dp), chip = RoundedCornerShape(999.dp),
            dialog = RoundedCornerShape(22.dp), dock = RoundedCornerShape(999.dp), cover = RoundedCornerShape(10.dp), indicator = RoundedCornerShape(999.dp),
        )
    }
    val stroke = when (direction) {
        TankobunArtDirection.PANEL_RIOT -> ThemeStrokeSet(2.dp, 3.dp, true)
        TankobunArtDirection.NOIR_ATELIER -> ThemeStrokeSet(1.dp, 1.5.dp, false)
        else -> ThemeStrokeSet(1.dp, 1.5.dp, false)
    }
    val motion = when (direction) {
        TankobunArtDirection.ORIGINAL -> ThemeMotionSet(0.96f, 190, true)
        TankobunArtDirection.STORYBOOK -> ThemeMotionSet(0.985f, 240, true)
        TankobunArtDirection.MOCHI_POP -> ThemeMotionSet(0.96f, 190, true)
        TankobunArtDirection.PANEL_RIOT -> ThemeMotionSet(0.975f, 110, false)
        TankobunArtDirection.NOIR_ATELIER -> ThemeMotionSet(0.99f, 180, false)
        TankobunArtDirection.NEON_CURRENT -> ThemeMotionSet(0.965f, 220, true)
    }
    val radii = when (direction) {
        TankobunArtDirection.ORIGINAL -> TankobunRadii(7.dp, 8.dp, 8.dp)
        TankobunArtDirection.STORYBOOK -> TankobunRadii(12.dp, 16.dp, 10.dp)
        TankobunArtDirection.MOCHI_POP -> TankobunRadii(20.dp, 18.dp, 14.dp)
        TankobunArtDirection.PANEL_RIOT -> TankobunRadii(4.dp, 4.dp, 4.dp)
        TankobunArtDirection.NOIR_ATELIER -> TankobunRadii(5.dp, 4.dp, 3.dp)
        TankobunArtDirection.NEON_CURRENT -> TankobunRadii(11.dp, 14.dp, 10.dp)
    }
    return DirectionSpec(
        shapes = shapes,
        strokes = stroke,
        motion = motion,
        materialShapes = Shapes(
            extraSmall = shapes.control,
            small = shapes.control,
            medium = shapes.panel,
            large = shapes.dialog,
            extraLarge = shapes.dialog,
        ),
        radii = radii,
    )
}

private data class PaletteDefinition(
    val id: TankobunPaletteId,
    val name: String,
    val dark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
) {
    fun choice() = TankobunPaletteChoice(id, name, dark, listOf(background, primary, secondary))

    fun colorScheme(): ColorScheme {
        val builder: (
            Color, Color, Color, Color, Color, Color, Color, Color, Color, Color, Color,
            Color, Color, Color, Color, Color,
        ) -> ColorScheme = if (dark) {
            { p, op, s, os, t, ot, bg, obg, sf, sv, osv, pc, opc, sc, osc, outline ->
                darkColorScheme(primary = p, onPrimary = op, secondary = s, onSecondary = os, tertiary = t, onTertiary = ot,
                    background = bg, onBackground = obg, surface = sf, onSurface = obg, surfaceVariant = sv,
                    onSurfaceVariant = osv, primaryContainer = pc, onPrimaryContainer = opc,
                    secondaryContainer = sc, onSecondaryContainer = osc, outline = outline, outlineVariant = outline.copy(alpha = 0.62f))
            }
        } else {
            { p, op, s, os, t, ot, bg, obg, sf, sv, osv, pc, opc, sc, osc, outline ->
                lightColorScheme(primary = p, onPrimary = op, secondary = s, onSecondary = os, tertiary = t, onTertiary = ot,
                    background = bg, onBackground = obg, surface = sf, onSurface = obg, surfaceVariant = sv,
                    onSurfaceVariant = osv, primaryContainer = pc, onPrimaryContainer = opc,
                    secondaryContainer = sc, onSecondaryContainer = osc, outline = outline, outlineVariant = outline.copy(alpha = 0.62f))
            }
        }
        val outline = lerp(onSurfaceVariant, background, if (dark) 0.36f else 0.48f)
        return builder(primary, onPrimary, secondary, onSecondary, tertiary, onTertiary, background, onBackground,
            surface, surfaceVariant, onSurfaceVariant, primaryContainer, onPrimaryContainer,
            secondaryContainer, onSecondaryContainer, outline)
    }

    fun tokens(): TankobunThemeTokens {
        val topBar = lerp(surface, primaryContainer, if (dark) 0.34f else 0.46f)
        val dock = lerp(surface, secondaryContainer, if (dark) 0.40f else 0.48f)
        return TankobunThemeTokens(
            appBackdrop = background,
            elevatedSurface = surface,
            softAccent = primaryContainer,
            readerOverlay = if (dark) Color(0xDD000000) else Color(0xCC080609),
            drawerHandle = primary.copy(alpha = if (dark) 0.42f else 0.24f),
            coverScrim = Color.Black.copy(alpha = if (dark) 0.30f else 0.13f),
            topBarSurface = topBar,
            topBarBleed = primary,
            dockSurface = dock,
            dockBleed = secondary,
            gradientStart = lerp(background, primaryContainer, if (dark) 0.18f else 0.28f),
            gradientEnd = lerp(background, secondaryContainer, if (dark) 0.22f else 0.34f),
            glow = secondary,
        )
    }
}

private object PaletteCatalog {
    val values = listOf(
        light(TankobunPaletteId.MATCHA_MEADOW, "Matcha", 0xFF3F7652, 0xFFA5653A, 0xFF778B3B, 0xFFF2F5E9, 0xFF172018, 0xFFFBFCF5, 0xFFDCE5D2, 0xFF3F4C3D, 0xFFD2E8D5, 0xFF102517, 0xFFFFDBC6, 0xFF3B190A),
        light(TankobunPaletteId.PEACH_COUNTRYSIDE, "Peach", 0xFFC84A2D, 0xFF008C86, 0xFFFFA82A, 0xFFFFF0E5, 0xFF2B1710, 0xFFFFFAF6, 0xFFFFD9C7, 0xFF644239, 0xFFFFD2C0, 0xFF4A1005, 0xFFC7F3EF, 0xFF002F2C),
        light(TankobunPaletteId.YUZU_GARDEN, "Yuzu", 0xFF007E71, 0xFFD19500, 0xFF5C8A1F, 0xFFFFF9D3, 0xFF191D10, 0xFFFFFFEF, 0xFFF2E49A, 0xFF4D4B2F, 0xFFC2F0E9, 0xFF003B34, 0xFFFFE477, 0xFF332500),
        light(TankobunPaletteId.BUNNY_BERRY, "Berry", 0xFFB82235, 0xFFEF7048, 0xFFE1A900, 0xFFFFF7F8, 0xFF241316, 0xFFFFFFFF, 0xFFFFE0DE, 0xFF5D403B, 0xFFFFDDE3, 0xFF3F0010, 0xFFFFE0C7, 0xFF3B1500),
        light(TankobunPaletteId.SAKURA_MINT, "Sakura", 0xFFB43D76, 0xFF008B77, 0xFF5A67C8, 0xFFFFEFF6, 0xFF26151C, 0xFFFFFAFC, 0xFFFFD6E6, 0xFF5A3D48, 0xFFFFD1E3, 0xFF43111F, 0xFFBDEFE4, 0xFF003B34),
        light(TankobunPaletteId.CLOUDBERRY, "Cobalt", 0xFF2F63C3, 0xFFD92265, 0xFF008F8C, 0xFFF5F8FF, 0xFF111B2B, 0xFFFFFFFF, 0xFFDCE8FF, 0xFF3D4961, 0xFFD5E3FF, 0xFF0A1E46, 0xFFFFD5E4, 0xFF4B0D21),
        light(TankobunPaletteId.REDLINE, "Redline", 0xFFC9261B, 0xFF151419, 0xFF008B91, 0xFFFFF4E8, 0xFF211515, 0xFFFFFBF5, 0xFFFFD4C8, 0xFF5F403D, 0xFFFFD5CF, 0xFF490601, 0xFFE4E0E3, 0xFF1C1B20),
        dark(TankobunPaletteId.ELECTRIC_BERRY, "Ultraviolet", 0xFFFF6FB1, 0xFF5BD9FF, 0xFFA8F0A1, 0xFF180B24, 0xFFFFECFF, 0xFF241332, 0xFF463255, 0xFFE3C9ED, 0xFF74254E, 0xFFFFD6E9, 0xFF0C5268, 0xFFC6F1FF),
        light(TankobunPaletteId.CITRUS_CLASH, "Citrus", 0xFF007E73, 0xFFE09C00, 0xFFD92D5B, 0xFFFFF9D9, 0xFF171C16, 0xFFFFFFF5, 0xFFECE5A7, 0xFF46503D, 0xFFC2EFE7, 0xFF003B34, 0xFFFFE17A, 0xFF332500),
        dark(TankobunPaletteId.CHARCOAL_GOLD, "Charcoal", 0xFFE8B44D, 0xFFD8C49A, 0xFFFFD98A, 0xFF11100E, 0xFFF7EFE0, 0xFF1B1915, 0xFF3A3428, 0xFFE0D3B8, 0xFF5A4113, 0xFFFFE2A4, 0xFF403727, 0xFFF4E3C2),
        dark(TankobunPaletteId.VELVET_PLUM, "Plum", 0xFFE58AD8, 0xFFA7E8BD, 0xFFFFC48D, 0xFF170F1E, 0xFFFFECFA, 0xFF21162A, 0xFF49344F, 0xFFE7C8E8, 0xFF6E2B66, 0xFFFFD6F8, 0xFF275038, 0xFFC8F9D8),
        dark(TankobunPaletteId.STARRY_INK, "Ink", 0xFF8FB6FF, 0xFFFFD166, 0xFF8CE6D2, 0xFF0A1020, 0xFFEAF0FF, 0xFF10182A, 0xFF23314A, 0xFFC7D3E8, 0xFF244A85, 0xFFD9E7FF, 0xFF5A4210, 0xFFFFE7A7),
        dark(TankobunPaletteId.NEON_KOI, "Koi", 0xFFFF8A7D, 0xFF5EF2D6, 0xFFFFD166, 0xFF071B1D, 0xFFE7FEFA, 0xFF0E272A, 0xFF234245, 0xFFB8D8D4, 0xFF7C261F, 0xFFFFD7D1, 0xFF0D5B52, 0xFFC8FFF4),
        dark(TankobunPaletteId.MOON_JELLY, "Moonlight", 0xFFB8A7FF, 0xFF72E6FF, 0xFFFF9FCB, 0xFF071720, 0xFFEAF7FF, 0xFF0E202B, 0xFF253847, 0xFFC3D5E2, 0xFF46347F, 0xFFE6DFFF, 0xFF0D5464, 0xFFC7F6FF),
        dark(TankobunPaletteId.ACID_AURORA, "Aurora", 0xFFC8FF43, 0xFFFF4FD8, 0xFF5FF3FF, 0xFF100D22, 0xFFF5F0FF, 0xFF1C1731, 0xFF3A3150, 0xFFD9CEEA, 0xFF405C08, 0xFFE9FFB9, 0xFF682252, 0xFFFFD8F0),
    )

    fun value(id: TankobunPaletteId) = values.first { it.id == id }

    private fun light(id: TankobunPaletteId, name: String, primary: Long, secondary: Long, tertiary: Long,
        background: Long, onBackground: Long, surface: Long, surfaceVariant: Long, onSurfaceVariant: Long,
        primaryContainer: Long, onPrimaryContainer: Long, secondaryContainer: Long, onSecondaryContainer: Long): PaletteDefinition {
        val primaryColor = Color(primary)
        val secondaryColor = Color(secondary)
        val tertiaryColor = Color(tertiary)
        return PaletteDefinition(id, name, false, primaryColor, contentFor(primaryColor), secondaryColor, contentFor(secondaryColor), tertiaryColor, contentFor(tertiaryColor),
            Color(background), Color(onBackground), Color(surface), Color(surfaceVariant), Color(onSurfaceVariant),
            Color(primaryContainer), Color(onPrimaryContainer), Color(secondaryContainer), Color(onSecondaryContainer))
    }

    private fun dark(id: TankobunPaletteId, name: String, primary: Long, secondary: Long, tertiary: Long,
        background: Long, onBackground: Long, surface: Long, surfaceVariant: Long, onSurfaceVariant: Long,
        primaryContainer: Long, onPrimaryContainer: Long, secondaryContainer: Long, onSecondaryContainer: Long): PaletteDefinition {
        val primaryColor = Color(primary)
        val secondaryColor = Color(secondary)
        val tertiaryColor = Color(tertiary)
        return PaletteDefinition(id, name, true, primaryColor, contentFor(primaryColor), secondaryColor, contentFor(secondaryColor), tertiaryColor, contentFor(tertiaryColor),
            Color(background), Color(onBackground), Color(surface), Color(surfaceVariant), Color(onSurfaceVariant),
            Color(primaryContainer), Color(onPrimaryContainer), Color(secondaryContainer), Color(onSecondaryContainer))
    }

    private fun contentFor(color: Color): Color =
        if (color.luminance() > 0.179f) Color(0xFF08090A) else Color.White
}

private data class ResolvedTheme(
    val preference: TankobunThemePreference,
    val palette: PaletteDefinition,
    val direction: DirectionSpec,
)

fun TankobunThemePreference.resolve(systemDark: Boolean): TankobunThemePreference = when {
    !automatic -> normalized()
    systemDark -> TankobunThemePreference(false, TankobunArtDirection.ORIGINAL, TankobunPaletteId.VELVET_PLUM)
    else -> TankobunThemePreference(false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.PEACH_COUNTRYSIDE)
}

fun TankobunThemePreference.isDark(systemDark: Boolean): Boolean =
    PaletteCatalog.value(resolve(systemDark).palette).dark

private fun resolvedTheme(preference: TankobunThemePreference, systemDark: Boolean): ResolvedTheme {
    val resolved = preference.resolve(systemDark)
    return ResolvedTheme(resolved, PaletteCatalog.value(resolved.palette), directionSpec(resolved.direction))
}

private val DefaultPreference = TankobunThemePreference(false, TankobunArtDirection.MOCHI_POP, TankobunPaletteId.BUNNY_BERRY)
private val DefaultPalette = PaletteCatalog.value(TankobunPaletteId.BUNNY_BERRY)
private val DefaultDirection = directionSpec(TankobunArtDirection.MOCHI_POP)
private val DefaultColors = DefaultPalette.colorScheme()
private val DefaultTokens = DefaultPalette.tokens()

private fun whiteTextContainer(color: Color): Color {
    if (color.luminance() <= 0.183f) return color
    var lighterBound = 0f
    var darkerBound = 1f
    repeat(10) {
        val amount = (lighterBound + darkerBound) / 2f
        if (lerp(color, Color.Black, amount).luminance() <= 0.183f) {
            darkerBound = amount
        } else {
            lighterBound = amount
        }
    }
    return lerp(color, Color.Black, darkerBound)
}

internal fun tankobunActionContainer(id: TankobunPaletteId): Color =
    whiteTextContainer(tankobunColorScheme(id).secondary)

val LocalTankobunTokens = staticCompositionLocalOf { DefaultTokens }
val LocalTankobunStyle = staticCompositionLocalOf {
    TankobunStyle(
        direction = DefaultPreference.direction,
        colors = TankobunStyleColors(
            backdrop = DefaultTokens.appBackdrop,
            panel = DefaultTokens.elevatedSurface,
            panelContent = DefaultColors.onSurface,
            accent = DefaultColors.primary,
            action = whiteTextContainer(DefaultColors.secondary),
            actionContent = Color.White,
            mutedContent = DefaultColors.onSurfaceVariant,
            chip = DefaultColors.surfaceVariant,
            chipContent = DefaultColors.onSurface,
            selectedChip = whiteTextContainer(DefaultColors.primary),
            selectedChipContent = Color.White,
            outline = DefaultColors.outline,
        ),
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
    val resolved = resolvedTheme(preference, isSystemInDarkTheme())
    val colors = resolved.palette.colorScheme()
    val tokens = resolved.palette.tokens()
    val style = TankobunStyle(
        direction = resolved.preference.direction,
        colors = TankobunStyleColors(
            backdrop = tokens.appBackdrop,
            panel = tokens.elevatedSurface,
            panelContent = colors.onSurface,
            accent = colors.primary,
            action = whiteTextContainer(colors.secondary),
            actionContent = Color.White,
            mutedContent = colors.onSurfaceVariant,
            chip = colors.surfaceVariant,
            chipContent = colors.onSurface,
            selectedChip = whiteTextContainer(colors.primary),
            selectedChipContent = Color.White,
            outline = colors.outline,
        ),
        themeShapes = resolved.direction.shapes,
        strokes = resolved.direction.strokes,
        motion = resolved.direction.motion,
        radii = resolved.direction.radii,
    )
    MaterialTheme(colorScheme = colors, shapes = resolved.direction.materialShapes) {
        CompositionLocalProvider(LocalTankobunTokens provides tokens, LocalTankobunStyle provides style, content = content)
    }
}
