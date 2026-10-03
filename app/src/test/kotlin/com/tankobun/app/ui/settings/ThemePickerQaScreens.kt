package com.tankobun.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tankobun.app.AppLanguage
import com.tankobun.app.TankobunThemePreference
import com.tankobun.app.qa.LayoutQaScreens
import org.junit.Test

class ThemePickerQaScreens(language: AppLanguage) : LayoutQaScreens(language) {
    @Test
    fun themePicker() = capture("theme-picker") {
        var preference by remember { mutableStateOf(TankobunThemePreference()) }
        ThemePicker(selected = preference, onSelect = { preference = it })
    }
}
