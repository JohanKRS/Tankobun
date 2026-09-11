package com.tankobun.app

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import com.tankobun.core.extensions.SourcePreferenceStore
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import kotlinx.coroutines.*
import java.io.File

/** Exercises a separately installed, original fixture APK through the real source host and UI. */
internal suspend fun Instrumentation.checkApkSourcePreferences(packageName: String) {
    val app = targetContext.applicationContext as TankobunApplication
    val host = app.container.sourceHost
    val descriptor = app.container.extensionScanner.installedExtensions().first { it.packageName == packageName }
    app.container.extensionTrustStore.untrustedExtension(descriptor)?.let { candidate ->
        check(host.loadSources(packageName).isEmpty())
        check(app.container.extensionTrustStore.approve(candidate))
    }
    val initial = host.loadSources(packageName).first { it.id == 987654401L }
    check(initial is ConfigurableSource)
    val store = SourcePreferenceStore(targetContext)
    store.preferences(initial.id).edit().clear().commit()
    store.preferences(987654402L).edit().putString("quality", "other-value").commit()
    host.clearCache(packageName)
    var activity = startActivitySync(Intent(targetContext, SourcePreferencesActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(SourcePreferencesActivity.PACKAGE, packageName)
        .putExtra(SourcePreferencesActivity.SOURCE_ID, initial.id)
        .putExtra(SourcePreferencesActivity.SOURCE_NAME, initial.name)) as SourcePreferencesActivity
    suspend fun screen(): PreferenceScreen = withTimeout(10_000) {
        var result: PreferenceScreen? = null
        while (result == null) {
            result = withContext(Dispatchers.Main) { (activity.supportFragmentManager.findFragmentById(R.id.source_preferences_container) as? PreferenceFragmentCompat)?.preferenceScreen?.takeIf { it.findPreference<Preference>("quality") != null } }
            if (result == null) delay(50)
        }
        result
    }
    val screen = screen()
    val originalInstance = host.loadSources(packageName).first { it.id == initial.id }
    withContext(Dispatchers.Main) {
        val quality = screen.findPreference<ListPreference>("quality")!!
        check(quality.value == "high")
        quality.performClick()
        activity.supportFragmentManager.executePendingTransactions()
        val dialog = (activity.supportFragmentManager.fragments.filterIsInstance<DialogFragment>().single().dialog as AlertDialog)
        dialog.listView.performItemClick(null, 1, 1)
    }
    delay(100)
    val preferences = store.preferences(initial.id)
    check(preferences.getString("quality", "") == "standard")
    check(preferences.getInt("callbackCount", 0) == 1)
    val refreshed = host.loadSources(packageName).first { it.id == initial.id }
    check(refreshed !== originalInstance)
    check(refreshed.getSearchManga(1, "fixture", FilterList()).mangas.single().description == "standard")
    check(store.preferences(987654402L).getString("quality", "") == "other-value")
    withContext(Dispatchers.Main) {
        val enabled = screen.findPreference<SwitchPreferenceCompat>("enabled")!!
        enabled.performClick()
        check(!screen.findPreference<Preference>("quality")!!.isEnabled)
        enabled.performClick()
        check(screen.findPreference<Preference>("quality")!!.isEnabled)
        val multi = screen.findPreference<MultiSelectListPreference>("excluded")!!
        multi.values = setOf("previews")
        screen.findPreference<SeekBarPreference>("margin")!!.value = 9
        screen.findPreference<Preference>("action")!!.performClick()
        check(screen.findPreference<Preference>("action")!!.summary == "Local action completed")
    }
    suspend fun enter(key: String, text: String, password: Boolean = false) {
        withTimeout(5_000) {
            while (withContext(Dispatchers.Main) { activity.supportFragmentManager.fragments.any { it is DialogFragment } }) delay(50)
        }
        withContext(Dispatchers.Main) {
            screen.findPreference<EditTextPreference>(key)!!.performClick()
            activity.supportFragmentManager.executePendingTransactions()
            val dialog = activity.supportFragmentManager.fragments.filterIsInstance<DialogFragment>().single().dialog as AlertDialog
            val edit = dialog.findViewById<EditText>(android.R.id.edit)!!
            if (password) check(edit.transformationMethod is android.text.method.PasswordTransformationMethod) { "Password field input type=${edit.inputType}, transformation=${edit.transformationMethod?.javaClass?.name}" }
            edit.setText(text)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        }
        withTimeout(5_000) {
            while (withContext(Dispatchers.Main) { activity.supportFragmentManager.fragments.any { it is DialogFragment } }) delay(50)
        }
    }
    enter("nickname", "rejected")
    check(preferences.getString("nickname", "").isNullOrEmpty())
    enter("password", "original-fixture-password", true)
    enter("nickname", "Original QA reader")
    val backup = com.tankobun.app.backup.AppSettingsBackupDataSource(app.container)
    val file = File(targetContext.cacheDir, "apk-source-preferences-test.json")
    backup.saveBackup(Uri.fromFile(file), com.tankobun.app.state.TankobunUiState())
    check(!file.readText().contains("original-fixture-password") && !file.readText().contains("Original QA reader"))
    preferences.edit().putString("quality", "high").putInt("margin", 2).putStringSet("excluded", emptySet()).commit()
    backup.restoreBackup(Uri.fromFile(file))
    check(preferences.getString("quality", "") == "standard" && preferences.getInt("margin", 0) == 9)
    check(preferences.getStringSet("excluded", emptySet()) == setOf("previews"))
    check(preferences.getString("password", "") == "original-fixture-password")
    file.delete()
    withContext(Dispatchers.Main) { screen.findPreference<PreferenceScreen>("advanced")!!.performClick() }
    withTimeout(10_000) {
        while (withContext(Dispatchers.Main) { activity.supportFragmentManager.backStackEntryCount == 0 }) delay(50)
    }
    withContext(Dispatchers.Main) {
        activity.supportFragmentManager.popBackStackImmediate()
        val root = activity.supportFragmentManager.findFragmentById(R.id.source_preferences_container) as PreferenceFragmentCompat
        root.findPreference<EditTextPreference>("nickname")!!.performClick()
        activity.supportFragmentManager.executePendingTransactions()
        val dialog = activity.supportFragmentManager.fragments.filterIsInstance<DialogFragment>().single().dialog as AlertDialog
        dialog.findViewById<EditText>(android.R.id.edit)!!.setText("Unsaved rotation draft")
    }
    val monitor = addMonitor(SourcePreferencesActivity::class.java.name, null, false)
    withContext(Dispatchers.Main) { activity.recreate() }
    activity = (waitForMonitorWithTimeout(monitor, 10_000) as? SourcePreferencesActivity) ?: error("Settings activity did not restore")
    removeMonitor(monitor)
    withContext(Dispatchers.Main) {
        val dialog = activity.supportFragmentManager.fragments.filterIsInstance<DialogFragment>().single().dialog as AlertDialog
        check(dialog.findViewById<EditText>(android.R.id.edit)!!.text.toString() == "Unsaved rotation draft")
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
    }
    withContext(Dispatchers.Main) { activity.finish() }
    println("PASS: installed APK settings / trust gate / native dialogs / callbacks / validation / password masking / dependencies / nested screens / rotation draft restore / source cache refresh / source isolation / settings backup and restore")
}
