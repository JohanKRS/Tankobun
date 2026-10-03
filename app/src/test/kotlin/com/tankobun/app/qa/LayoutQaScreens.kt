package com.tankobun.app.qa

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.platform.ComposeView
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tankobun.app.AppLanguage
import com.tankobun.app.TankobunColorMode
import com.tankobun.app.TankobunLocalizedContent
import com.tankobun.app.TankobunTheme
import com.tankobun.app.TankobunThemePreference
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders UI pieces in every app language so long translations can be checked for clipping
 * without a device. Images go to app/build/qa-screens; enable with -Ptankobun.qaScreens.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, sdk = [35], qualifiers = "w360dp-h1600dp-xxhdpi")
abstract class LayoutQaScreens(private val language: AppLanguage) {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    protected fun capture(
        name: String,
        width: Dp = 360.dp,
        dark: Boolean = false,
        topWindow: Boolean = false,
        content: @Composable () -> Unit,
    ) {
        assumeTrue(System.getProperty("tankobun.qaScreens") == "true")
        compose.setContent {
            TankobunLocalizedContent(language) {
                TankobunTheme(TankobunThemePreference(mode = if (dark) TankobunColorMode.DARK else TankobunColorMode.LIGHT)) {
                    Box(Modifier.width(width).background(MaterialTheme.colorScheme.background).padding(16.dp)) {
                        Box(Modifier.fillMaxWidth()) { content() }
                    }
                }
            }
        }
        compose.waitForIdle()
        // Sheets and dialogs live in their own windows; the newest root view is the one on top.
        val view = if (topWindow) topWindowRootView() else compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bitmap))
        val directory = File(System.getProperty("tankobun.qaScreensDir") ?: "build/qa-screens").apply { mkdirs() }
        File(directory, "$name-${language.storageValue}${if (dark) "-dark" else ""}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun topWindowRootView(): View {
        val global = Class.forName("android.view.WindowManagerGlobal").getMethod("getInstance").invoke(null)
        val field = global.javaClass.getDeclaredField("mViews").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        return (field.get(global) as List<View>).last()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun languages(): List<Array<Any>> = listOf(
            AppLanguage.ENGLISH,
            AppLanguage.PORTUGUESE_BRAZIL,
            AppLanguage.SPANISH,
            AppLanguage.CHINESE_SIMPLIFIED,
        ).map { arrayOf<Any>(it) }
    }
}
