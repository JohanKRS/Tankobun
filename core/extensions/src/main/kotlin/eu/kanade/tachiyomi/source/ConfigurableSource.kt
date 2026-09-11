package eu.kanade.tachiyomi.source

import androidx.preference.PreferenceScreen
import android.content.Context
import android.content.SharedPreferences
import uy.kohesive.injekt.TankobunInjektRegistry

interface ConfigurableSource : Source {
    fun getSourcePreferences(): SharedPreferences = sourcePreferences()
    fun setupPreferenceScreen(screen: PreferenceScreen) = Unit
}

fun ConfigurableSource.preferenceKey(): String = "source_$id"
fun ConfigurableSource.sourcePreferences(): SharedPreferences = sourcePreferences(preferenceKey())
fun sourcePreferences(key: String): SharedPreferences =
    requireNotNull(TankobunInjektRegistry.applicationOrNull()).getSharedPreferences(key, Context.MODE_PRIVATE)
