package com.tankobun.app.ui.settings

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tankobun.app.R
import com.tankobun.app.TankobunApplication
import com.tankobun.app.tankobunString
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.extensions.novel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun NovelSourceSettingsButton(packageName: String, name: String) {
    var open by remember(packageName) { mutableStateOf(false) }
    SourceSettingsIconActionButton(TankobunIcons.Settings, tankobunString(R.string.novel_source_settings, name), { open = true })
    if (open) NovelSourceSettingsDialog(packageName, name, { open = false })
}

@Composable
internal fun NovelSourceSettingsDialog(packageName: String, name: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(packageName) { LnReaderSettingsStore(context, packageName) }
    val scope = rememberCoroutineScope()
    var definitions by remember(packageName) { mutableStateOf<JSONObject?>(null) }
    var values by remember(packageName) { mutableStateOf(JSONObject()) }
    var site by remember(packageName) { mutableStateOf<String?>(null) }
    var error by remember(packageName) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var website by remember { mutableStateOf<String?>(null) }
    val host = (context.applicationContext as TankobunApplication).container.sourceHost

    suspend fun load() {
        val metadata = withContext(Dispatchers.IO) {
            val (plugin, code) = LnReaderPluginStore(context).record(packageName) ?: error("Novel plugin is not installed")
            LnReaderRuntime(context).call(plugin, code, "metadata", JSONArray()) as JSONObject
        }
        val schema = metadata.optJSONObject("pluginSettings") ?: JSONObject()
        store.saveDefinitions(schema)
        val configured = store.values()
        values = JSONObject().apply { lnReaderSettings(schema, configured).forEach { put(it.key, it.value) } }
        definitions = schema
        site = metadata.optString("site").takeIf { webOrigin(it) != null }
    }
    LaunchedEffect(packageName) { try { load() } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel } catch (failure: Exception) { error = failure.message } }
    fun save(openWebsite: Boolean) {
        scope.launch {
            saving = true
            try {
                withContext(Dispatchers.IO) { store.save(values) }
                host.clearCache(packageName)
                if (openWebsite) { load(); website = site } else onClose()
            } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
            catch (failure: Exception) { error = failure.message }
            finally { saving = false }
        }
    }
    val fields = definitions?.let { schema -> runCatching { lnReaderSettings(schema, values) }.getOrNull() }
    AlertDialog(onDismissRequest = { if (!saving) onClose() }, title = { Text(name) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (definitions == null && error == null) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (fields?.isEmpty() == true) Text(tankobunString(R.string.novel_source_no_settings))
                fields.orEmpty().forEach { field ->
                    key(field.key) {
                        NovelSourceField(field) { value -> values = JSONObject(values.toString()).put(field.key, value) }
                    }
                }
                if (definitions != null) {
                    OutlinedButton(onClick = { save(true) }, enabled = !saving && site != null, modifier = Modifier.fillMaxWidth()) {
                        Icon(TankobunIcons.Link, null)
                        Spacer(Modifier.width(8.dp))
                        Text(tankobunString(R.string.novel_source_website))
                    }
                    Text(tankobunString(R.string.novel_source_website_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { save(false) }, enabled = !saving && definitions != null) { Text(tankobunString(R.string.novel_source_save)) } },
        dismissButton = { TextButton(onClick = onClose, enabled = !saving) { Text(tankobunString(R.string.common_close)) } })
    website?.let { url ->
        NovelSourceWebsiteDialog(packageName, name, url) {
            host.clearCache(packageName)
            website = null
        }
    }
}

@Composable
private fun NovelSourceField(field: LnReaderSetting, onValue: (Any) -> Unit) {
    when (field.type) {
        LnReaderSettingType.TEXT -> OutlinedTextField(
            value = field.value as? String ?: "", onValueChange = onValue, label = { Text(field.label) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            visualTransformation = if (field.sensitive) PasswordVisualTransformation() else VisualTransformation.None)
        LnReaderSettingType.SWITCH -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(field.label, Modifier.weight(1f)); Switch(field.value as? Boolean ?: false, onValue)
        }
        LnReaderSettingType.SELECT, LnReaderSettingType.CHECKBOX_GROUP -> Column {
            Text(field.label, style = MaterialTheme.typography.titleSmall)
            val selected = field.value as? JSONArray
            field.options.forEach { option ->
                val checked = if (selected == null) field.value == option.value else (0 until selected.length()).any { selected.optString(it) == option.value }
                val toggle = {
                    if (field.type == LnReaderSettingType.SELECT) onValue(option.value)
                    else {
                        val next = (0 until (selected?.length() ?: 0)).map { selected!!.getString(it) }.toMutableList()
                        if (checked) next.remove(option.value) else next.add(option.value)
                        onValue(JSONArray(next))
                    }
                }
                Surface(onClick = toggle, modifier = Modifier.fillMaxWidth(), color = androidx.compose.ui.graphics.Color.Transparent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (field.type == LnReaderSettingType.SELECT) RadioButton(checked, null) else Checkbox(checked, null)
                        Text(option.label, Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun NovelSourceWebsiteDialog(packageName: String, name: String, url: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val session = remember(packageName, url) { LnReaderWebsiteSession(context, packageName, url) }
    var view by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(url) }
    var loading by remember { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(closing) {
        // A stalled website renderer must not trap the user inside the browser.
        if (closing) { kotlinx.coroutines.delay(1500); onClose() }
    }
    fun close() {
        if (closing) return
        closing = true
        val browser = view
        if (browser == null) onClose() else session.capture(browser) { onClose() }
    }
    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        BackHandler { if (view?.canGoBack() == true) view?.goBack() else close() }
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::close, enabled = !closing) { Icon(TankobunIcons.ArrowBack, tankobunString(R.string.common_close)) }
                    Column(Modifier.weight(1f)) {
                        Text(name, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                        Text(webOrigin(currentUrl).orEmpty(), maxLines = 1, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { view?.reload() }) { Icon(TankobunIcons.Refresh, tankobunString(R.string.novel_source_reload)) }
                    TextButton(onClick = ::close, enabled = !closing) { Text(tankobunString(R.string.novel_source_done)) }
                }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                AndroidView(factory = { ctx -> WebView(ctx).also { browser ->
                    view = browser
                    session.configure(browser) { nextUrl, busy -> currentUrl = nextUrl; loading = busy }
                    browser.loadUrl(url)
                } }, modifier = Modifier.weight(1f).fillMaxWidth(), onRelease = { browser ->
                    view = null; browser.stopLoading(); browser.destroy()
                })
            }
        }
    }
}
