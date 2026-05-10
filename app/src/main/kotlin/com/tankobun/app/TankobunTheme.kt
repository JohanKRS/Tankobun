package com.tankobun.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

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
    val dark = when (themeMode) {
        TankobunThemeMode.SYSTEM -> isSystemInDarkTheme()
        TankobunThemeMode.LIGHT -> false
        TankobunThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (dark) TankobunDarkColors else TankobunLightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
