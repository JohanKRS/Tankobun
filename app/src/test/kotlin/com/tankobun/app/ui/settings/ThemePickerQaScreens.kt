package com.tankobun.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.TankobunPaletteId
import com.tankobun.app.TankobunThemePreference
import com.tankobun.app.qa.LayoutQaScreens
import org.junit.Test
import org.robolectric.annotation.Config

class ThemePickerQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    @Test
    fun themePicker() = capture("theme-picker") {
        var preference by remember { mutableStateOf(TankobunThemePreference()) }
        ThemePicker(selected = preference, onSelect = { preference = it })
    }

    // The phone Appearance page at 115% font scale, inside the capture's 16dp frame: 384dp and 360dp phones.
    @Test
    @Config(qualifiers = "w416dp-h1600dp-xxhdpi")
    fun appearancePhone() = capture("appearance-phone", width = 416.dp) {
        AppearancePage(TankobunPaletteId.RETICULA)
    }

    @Test
    @Config(qualifiers = "w416dp-h1600dp-xxhdpi")
    fun appearancePhoneDynamic() = capture("appearance-phone-dynamic", width = 416.dp) {
        AppearancePage(TankobunPaletteId.DYNAMIC)
    }

    @Test
    @Config(qualifiers = "w392dp-h1600dp-xxhdpi")
    fun appearanceNarrowPhone() = capture("appearance-phone-narrow", width = 392.dp) {
        AppearancePage(TankobunPaletteId.RETICULA)
    }

    @Composable
    private fun AppearancePage(palette: TankobunPaletteId) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1.15f)) {
            var preference by remember { mutableStateOf(TankobunThemePreference(palette = palette)) }
            SettingsDetailPanel(title = "", subtitle = "", showHeader = false) {
                ThemePicker(selected = preference, onSelect = { preference = it })
            }
        }
    }
}
