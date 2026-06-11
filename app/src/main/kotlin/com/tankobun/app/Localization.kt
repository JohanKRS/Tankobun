package com.tankobun.app

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import java.util.Locale

enum class AppLanguage(
    val storageValue: String,
    val languageTag: String?,
) {
    SYSTEM("system", null),
    ENGLISH("en", "en"),
    PORTUGUESE_BRAZIL("pt-BR", "pt-BR"),
    SPANISH("es", "es");

    companion object {
        fun fromStorageValue(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

fun Context.withAppLanguage(language: AppLanguage): Context {
    val tag = language.languageTag ?: return this
    val locale = Locale.forLanguageTag(tag)
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    return createConfigurationContext(configuration)
}

fun Context.getAppString(language: AppLanguage, @StringRes id: Int, vararg args: Any): String =
    withAppLanguage(language).resources.getString(id, *args)

fun Context.getAppQuantityString(
    language: AppLanguage,
    @PluralsRes id: Int,
    quantity: Int,
    vararg args: Any,
): String =
    withAppLanguage(language).resources.getQuantityString(id, quantity, *args)

@Composable
internal fun TankobunLocalizedContent(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val baseContext = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, language, systemConfiguration) {
        baseContext.withAppLanguage(language)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
    ) {
        content()
    }
}

@Composable
@ReadOnlyComposable
internal fun tankobunString(@StringRes id: Int): String =
    stringResource(id)

@Composable
@ReadOnlyComposable
internal fun tankobunString(@StringRes id: Int, vararg args: Any): String =
    stringResource(id, *args)

@Composable
@ReadOnlyComposable
internal fun tankobunQuantityString(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
    pluralStringResource(id, quantity, *args)
