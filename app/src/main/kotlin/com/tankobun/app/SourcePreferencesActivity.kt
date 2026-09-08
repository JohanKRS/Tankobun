package com.tankobun.app

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.preference.*
import androidx.recyclerview.widget.RecyclerView
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.extensions.SourcePreferenceStore
import eu.kanade.tachiyomi.source.ConfigurableSource

/** Hosts the community's real Preference controls, including extension callbacks and custom views. */
class SourcePreferencesActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    internal val container get() = (application as TankobunApplication).container
    private val sourcePackage get() = intent.getStringExtra(PACKAGE).orEmpty()
    private val sourceId get() = intent.getLongExtra(SOURCE_ID, 0)
    private var sourcePreferences: SharedPreferences? = null
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        container.sourceHost.clearCache(sourcePackage)
    }
    internal val sourceColors get() = tankobunColorScheme(SettingsStore(this).themePreference().resolve(systemDark()).palette)
    private fun systemDark() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withAppLanguage(SettingsStore(newBase).appLanguage()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val dark = SettingsStore(this).themePreference().isDark(systemDark())
        delegate.localNightMode = if (dark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        val root = FrameLayout(this).apply { setBackgroundColor(sourceColors.background.toArgb()) }
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(column, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        root.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            val width = minOf(right - left, (720 * resources.displayMetrics.density).toInt())
            if (column.layoutParams.width != width) column.layoutParams = (column.layoutParams as FrameLayout.LayoutParams).apply { this.width = width }
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        val header = ComposeView(this)
        column.addView(header)
        header.setContent {
            TankobunTheme(SettingsStore(this).themePreference()) {
                Surface {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        IconButton(onClick = { onBackPressedDispatcher.onBackPressed() }) { Icon(TankobunIcons.ArrowBack, tankobunString(R.string.common_close)) }
                        Column(Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(intent.getStringExtra(SOURCE_NAME).orEmpty(), style = MaterialTheme.typography.titleLarge)
                            Text(tankobunString(R.string.source_preferences_autosave), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        column.addView(FragmentContainerView(this).apply { id = R.id.source_preferences_container }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
        if (savedInstanceState == null) supportFragmentManager.beginTransaction()
            .replace(R.id.source_preferences_container, SourcePreferencesFragment.create(sourcePackage, sourceId)).commit()
        sourcePreferences = SourcePreferenceStore(this).preferences(sourceId).also { it.registerOnSharedPreferenceChangeListener(preferenceListener) }
    }

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, screen: PreferenceScreen): Boolean {
        supportFragmentManager.beginTransaction().replace(R.id.source_preferences_container,
            SourcePreferencesFragment.create(sourcePackage, sourceId, screen.key)).addToBackStack(screen.key).commit()
        return true
    }

    override fun onDestroy() {
        sourcePreferences?.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onDestroy()
    }

    companion object {
        const val PACKAGE = "source_package"
        const val SOURCE_ID = "source_id"
        const val SOURCE_NAME = "source_name"
    }
}

class SourcePreferencesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val packageName = requireArguments().getString(SourcePreferencesActivity.PACKAGE).orEmpty()
        val sourceId = requireArguments().getLong(SourcePreferencesActivity.SOURCE_ID)
        preferenceManager.sharedPreferencesName = "source_$sourceId"
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
        val host = (requireActivity() as SourcePreferencesActivity).container.sourceHost
        // Native dialog restoration needs its Preference immediately. The settings button warms
        // the source off the UI thread; restoring the activity must rebuild this hierarchy in order.
        try {
            val source = host.loadSources(packageName).firstOrNull { it.id == sourceId }
            check(source is ConfigurableSource) { getString(R.string.source_preferences_unavailable) }
            val screen = preferenceManager.createPreferenceScreen(requireContext())
            preferenceScreen = screen
            source.setupPreferenceScreen(screen)
            prepare(screen)
            SourcePreferenceStore(requireContext()).register(packageName, sourceId, screen)
            preferenceScreen = requireArguments().getString(ROOT)?.let { screen.findPreference<PreferenceScreen>(it) } ?: screen
            if (preferenceScreen.preferenceCount == 0) message(getString(R.string.novel_source_no_settings))
        } catch (error: Throwable) {
            android.util.Log.w("SourcePreferences", "Could not open settings for $packageName", error)
            message(getString(R.string.source_preferences_unavailable))
        }
    }

    private fun prepare(group: PreferenceGroup) {
        for (index in 0 until group.preferenceCount) {
            val preference = group.getPreference(index)
            preference.isIconSpaceReserved = preference.icon != null
            preference.isSingleLineTitle = false
            if (preference is DialogPreference && preference.dialogTitle.isNullOrEmpty()) preference.dialogTitle = preference.title
            if (preference is PreferenceGroup) prepare(preference)
        }
    }

    private fun message(text: String) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireContext()).apply {
            addPreference(Preference(context).apply { summary = text; isSelectable = false; isIconSpaceReserved = false })
        }
    }

    // The pinned preference 1.2.1 adapter preserves custom APK row bindings while applying our palette.
    @android.annotation.SuppressLint("RestrictedApi")
    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> = object : PreferenceGroupAdapter(preferenceScreen) {
        override fun onBindViewHolder(holder: PreferenceViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            val colors = (requireActivity() as SourcePreferencesActivity).sourceColors
            val preference = getItem(position)
            fun style(view: View) {
                if (view is TextView) {
                    val color = if (view.id == android.R.id.summary) colors.onSurfaceVariant else if (preference is PreferenceCategory) colors.primary else colors.onSurface
                    view.setTextColor(android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()), intArrayOf(color.toArgb(), color.copy(alpha = 0.38f).toArgb())))
                }
                if (view is CompoundButton) view.buttonTintList = android.content.res.ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(colors.primary.toArgb(), colors.onSurfaceVariant.toArgb()))
                if (view is androidx.appcompat.widget.SwitchCompat) {
                    view.thumbTintList = android.content.res.ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(colors.primary.toArgb(), colors.onSurfaceVariant.toArgb()))
                    view.trackTintList = android.content.res.ColorStateList.valueOf(colors.primary.copy(alpha = 0.35f).toArgb())
                }
                if (view is SeekBar) { view.progressTintList = android.content.res.ColorStateList.valueOf(colors.primary.toArgb()); view.thumbTintList = view.progressTintList }
                if (view is ViewGroup) for (index in 0 until view.childCount) style(view.getChildAt(index))
            }
            style(holder.itemView)
        }
    }

    companion object {
        private const val ROOT = "root_preference"
        fun create(packageName: String, sourceId: Long, root: String? = null): Fragment = SourcePreferencesFragment().apply {
            arguments = Bundle().apply {
                putString(SourcePreferencesActivity.PACKAGE, packageName)
                putLong(SourcePreferencesActivity.SOURCE_ID, sourceId)
                putString(ROOT, root)
            }
        }
    }
}
