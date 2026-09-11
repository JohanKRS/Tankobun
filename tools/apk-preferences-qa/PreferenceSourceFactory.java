package eu.kanade.tachiyomi.extension.en.preferencesqa;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import androidx.preference.*;
import eu.kanade.tachiyomi.source.*;
import eu.kanade.tachiyomi.source.model.*;
import java.util.*;
import java.lang.reflect.Proxy;

/** Original, content-free APK fixture. Never packaged in Tankobun. */
public class PreferenceSourceFactory implements SourceFactory {
    public PreferenceSourceFactory() { System.setProperty("tankobun.preferences.fixture.loaded", "true"); }
    public List<Source> createSources() { return Arrays.asList(source(987654401L), source(987654402L)); }
    private Source source(long id) {
        Context app = uy.kohesive.injekt.TankobunInjektRegistry.INSTANCE.applicationOrNull();
        SharedPreferences preferences = app.getSharedPreferences("source_" + id, 0);
        String initial = preferences.getString("quality", "high");
        return (Source) Proxy.newProxyInstance(Source.class.getClassLoader(), new Class[]{ConfigurableSource.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "getId": return id;
                case "getName": return id == 987654401L ? "Paper Panels" : "Other Paper Panels";
                case "getLang": return "en";
                case "getSupportsLatest": case "isNovelSource": return false;
                case "getSourcePreferences": return preferences;
                case "getFilterList": return new FilterList();
                case "getSearchManga": {
                    SManga manga = SManga.Companion.create(); manga.setUrl("fiction"); manga.setTitle("Fictional QA"); manga.setDescription(initial);
                    return new MangasPage(Collections.singletonList(manga), false);
                }
                case "setupPreferenceScreen": populate((PreferenceScreen)args[0], preferences); return null;
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                case "toString": return "Paper Panels settings fixture";
                default: throw new UnsupportedOperationException(method.getName());
            }
        });
    }
    private void populate(PreferenceScreen screen, SharedPreferences stored) {
        Context context = screen.getContext();
        SwitchPreferenceCompat enabled = new SwitchPreferenceCompat(context);
        enabled.setKey("enabled"); enabled.setTitle("Use image preferences"); enabled.setDefaultValue(true); screen.addPreference(enabled);
        ListPreference quality = new ListPreference(context);
        quality.setKey("quality"); quality.setTitle("Image quality"); quality.setEntries(new String[]{"High", "Standard"});
        quality.setEntryValues(new String[]{"high", "standard"}); quality.setDefaultValue("high");
        quality.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        quality.setOnPreferenceChangeListener((preference, value) -> { stored.edit().putInt("callbackCount", stored.getInt("callbackCount", 0) + 1).apply(); return !"forbidden".equals(value); });
        screen.addPreference(quality); quality.setDependency("enabled");
        MultiSelectListPreference excluded = new MultiSelectListPreference(context);
        excluded.setKey("excluded"); excluded.setTitle("Exclude chapter types"); excluded.setEntries(new String[]{"Previews", "Announcements"});
        excluded.setEntryValues(new String[]{"previews", "announcements"}); excluded.setDefaultValue(Collections.emptySet()); screen.addPreference(excluded);
        EditTextPreference nickname = new EditTextPreference(context);
        nickname.setKey("nickname"); nickname.setTitle("Reader name"); nickname.setDefaultValue("");
        nickname.setOnPreferenceChangeListener((preference, value) -> !"rejected".equals(value)); screen.addPreference(nickname);
        EditTextPreference password = new EditTextPreference(context);
        password.setKey("password"); password.setTitle("Password"); password.setDefaultValue("");
        password.setOnBindEditTextListener(edit -> edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)); screen.addPreference(password);
        SeekBarPreference margin = new SeekBarPreference(context);
        margin.setKey("margin"); margin.setTitle("Source image margin"); margin.setMin(0); margin.setMax(20); margin.setDefaultValue(5); margin.setShowSeekBarValue(true); screen.addPreference(margin);
        Preference action = new Preference(context);
        action.setKey("action"); action.setTitle("Check source settings"); action.setSummary("Runs an original local action");
        action.setOnPreferenceClickListener(pref -> { pref.setSummary("Local action completed"); stored.edit().putBoolean("actionRan", true).apply(); return true; }); screen.addPreference(action);
        PreferenceScreen advanced = screen.getPreferenceManager().createPreferenceScreen(context);
        advanced.setKey("advanced"); advanced.setTitle("Advanced options"); screen.addPreference(advanced);
        CheckBoxPreference bonus = new CheckBoxPreference(context);
        bonus.setKey("bonus"); bonus.setTitle("Show bonus chapters"); bonus.setDefaultValue(true); advanced.addPreference(bonus);
    }
}
