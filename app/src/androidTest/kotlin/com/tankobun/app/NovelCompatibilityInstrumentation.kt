package com.tankobun.app

import android.app.Instrumentation
import android.os.Bundle
import com.tankobun.core.extensions.novel.*
import com.tankobun.core.extensions.readingContentKind
import com.tankobun.core.model.*
import com.tankobun.core.database.toEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/** Device contract tests run only on the separate .novelqa application. No source content is bundled. */
class NovelCompatibilityInstrumentation : Instrumentation() {
    private var options = Bundle()
    override fun onCreate(arguments: Bundle?) { options = arguments ?: Bundle(); super.onCreate(arguments); start() }
    override fun onStart() {
        val report = Bundle()
        try {
            check(targetContext.packageName.endsWith(".novelqa")) { "Use -PqaApplicationIdSuffix=.novelqa" }
            if (options.getString("novelReader") == "true") {
                runBlocking { checkNovelReader() }
                report.putString("stream", "PASS: novel reader adjacent chapters, offline loading, character anchors and preference backup/restore\n")
                finish(-1, report)
                return
            }
            if (options.getString("catalogStartup") == "true") {
                runBlocking { checkCatalogStartup() }
                report.putString("stream", "PASS: catalog startup priority, fallback and saved preferences\n")
                finish(-1, report)
                return
            }
            if (options.getString("repositories") == "true") {
                runBlocking { checkRepositoryManagement() }
                report.putString("stream", "PASS: repository management contract\n")
                finish(-1, report)
                return
            }
            runBlocking {
                options.getString("privateExtensions")?.let { checkPrivateExtensions(afterSystemRemoval = it == "after-removal") }
                options.getString("apkPreferences")?.let { checkApkSourcePreferences(it) }
                val runtime = LnReaderRuntime(targetContext)
                val plugin = LnReaderPlugin("fiction", "Paper Observatory", "https://example.invalid", "English", "1.0.0", "https://example.invalid/fixture.js", repositoryUrl = "https://example.invalid/plugins.json")
                val code = """
                    const {load}=require('cheerio');
                    const {storage}=require('@libs/storage');
                    const dayjs=require('dayjs');
                    const urlencode=require('urlencode');
                    exports.default={id:'fiction',name:'Paper Observatory',version:'1.0.0',site:'https://example.invalid',webStorageUtilized:true,
                      pluginSettings:{hideLocked:{label:'Hide locked',type:'Switch',value:false}},
                      parseChapter:async()=>'<h2>A paper sky</h2><p>A wholly fictional chapter written for this test.</p>',
                      searchNovels:async(q,p)=>{const s=load('<a data-id="paper">The Paper Observatory</a>');storage.set('query',q);return [{path:s('a').attr('data-id'),name:s('a').text(),date:dayjs('2026-01-02').format('YYYY'),encoded:urlencode.encode('hello world')}];},
                      parseNovel:async(path)=>({path,name:'The Paper Observatory',chapters:[{path:'1',name:'Dawn'}],totalPages:2}),
                      parsePage:async(path,page)=>({chapters:[{path:page,name:'Twilight'}]})};
                """.trimIndent()
                val server = java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
                val serverThread = Thread {
                    runCatching { server.accept().use { socket ->
                        val input = socket.getInputStream().bufferedReader()
                        while (!input.readLine().isNullOrEmpty()) { }
                        val body = "<p>Fictional caf\u00e9.</p>".toByteArray(Charsets.UTF_8)
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                        socket.getOutputStream().write(body)
                    } }
                }.apply { isDaemon = true; start() }
                try {
                    val networkCode = "const {fetchText}=require('@libs/fetch');exports.default={parseChapter:()=>fetchText('http://127.0.0.1:${server.localPort}/fiction')};"
                    check((runtime.call(plugin, networkCode, "parseChapter", JSONArray()) as String).contains("caf\u00e9"))
                } finally { server.close(); serverThread.join(1000) }
                val found = runtime.call(plugin, code, "searchNovels", JSONArray().put("paper").put(1)) as JSONArray
                check(found.getJSONObject(0).getString("name") == "The Paper Observatory")
                check(found.getJSONObject(0).getString("date") == "2026")
                val details = runtime.call(plugin, code, "details", JSONArray().put("paper")) as JSONObject
                check(details.getJSONArray("chapters").length() == 2)
                val html = runtime.call(plugin, code, "parseChapter", JSONArray().put("1")) as String
                check(NovelDocument.parse(html, plugin.site).last().novelBlock!!.endOfChapter)
                val preferences = targetContext.getSharedPreferences("novel_${plugin.packageName}", 0)
                check(JSONObject(preferences.getString("storage", "{}").orEmpty()).getJSONObject("query").getString("value") == "paper")
                val store = SettingsStore(targetContext)
                val original = store.novelReaderPreferences()
                store.saveNovelReaderPreferences(original.copy(fontSize = 28, margin = 32))
                check(SettingsStore(targetContext).novelReaderPreferences().fontSize == 28)
                store.saveNovelReaderPreferences(original)

                val pluginSettings = LnReaderSettingsStore(targetContext, plugin.packageName)
                val schema = JSONObject("""{"hideLocked":{"label":"Hide locked","type":"Switch","value":false},"password":{"label":"Password","type":"Text","value":""}}""")
                pluginSettings.saveDefinitions(schema)
                pluginSettings.save(JSONObject().put("hideLocked", true).put("password", "fictional-test-secret"))
                check(pluginSettings.backupValues().getBoolean("hideLocked") && !pluginSettings.backupValues().has("password"))
                val browserResult = kotlinx.coroutines.CompletableDeferred<Boolean>()
                var browser: android.webkit.WebView? = null
                val website = LnReaderWebsiteSession(targetContext, plugin.packageName, plugin.site)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    browser = android.webkit.WebView(targetContext).also { view ->
                        website.configure(view) { _, loading -> if (!loading) website.capture(view) { browserResult.complete(it) } }
                        view.loadDataWithBaseURL(plugin.site, "<p>Original fictional login page.</p><script>localStorage.setItem('fixture','local-value');sessionStorage.setItem('fixture','session-value');</script>", "text/html", "UTF-8", plugin.site)
                    }
                }
                check(kotlinx.coroutines.withTimeout(15_000) { browserResult.await() }) { "Website capture failed for fixture URL: ${kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { browser!!.url }}" }
                val captured = pluginSettings.webStorage().getJSONObject("https://example.invalid")
                check(captured.getJSONObject("local").getString("fixture") == "local-value")
                check(captured.getJSONObject("session").getString("fixture") == "session-value")
                val websiteCode = "const {localStorage,sessionStorage}=require('@libs/storage');exports.default={site:'https://example.invalid',parseChapter:()=>localStorage.get().fixture+':'+sessionStorage.get().fixture};"
                check(runtime.call(plugin, websiteCode, "parseChapter", JSONArray()) == "local-value:session-value")
                val other = plugin.copy(id = "other-fixture")
                check(LnReaderSettingsStore(targetContext, other.packageName).webStorage().length() == 0)
                val rotatedSettings = LnReaderSettingsStore(targetContext, other.packageName)
                rotatedSettings.saveDefinitions(JSONObject("""{"token":{"label":"Token","type":"Text","value":""}}"""))
                rotatedSettings.save(JSONObject().put("token", "fixture"))
                val rotatingCode = "const {storage}=require('@libs/storage');exports.default={parseChapter:()=>{const value=storage.get('token');storage.set('token',value+'!');return value}};"
                check(runtime.call(other, rotatingCode, "parseChapter", JSONArray()) == "fixture")
                check(runtime.call(other, rotatingCode, "parseChapter", JSONArray()) == "fixture!")
                check(rotatedSettings.values().getString("token") == "fixture!!")
                rotatedSettings.save(JSONObject().put("token", "fixture"))
                check(rotatedSettings.values().getString("token") == "fixture")
                check(runtime.call(other, rotatingCode, "parseChapter", JSONArray()) == "fixture")
                rotatedSettings.restoreValues(JSONObject().put("token", "restored-fixture"))
                check(rotatedSettings.values().getString("token") == "restored-fixture")
                check(runtime.call(other, rotatingCode, "parseChapter", JSONArray()) == "restored-fixture")
                val foreignResult = kotlinx.coroutines.CompletableDeferred<Boolean>()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    website.configure(browser!!) { _, loading -> if (!loading) website.capture(browser!!) { foreignResult.complete(it) } }
                    browser!!.loadDataWithBaseURL("https://foreign.invalid", "<p>Unrelated fictional site.</p><script>localStorage.setItem('fixture','foreign-value');</script>", "text/html", "UTF-8", "https://foreign.invalid")
                }
                check(!kotlinx.coroutines.withTimeout(15_000) { foreignResult.await() })
                check(pluginSettings.webStorage().getJSONObject("https://example.invalid").getJSONObject("local").getString("fixture") == "local-value")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { browser!!.destroy() }

                val app = targetContext.applicationContext as TankobunApplication
                // Exercise the actual source host, cache, download worker and offline reader with original text.
                val installedFile = File(targetContext.filesDir, "novel_plugins/${plugin.packageName}.json")
                installedFile.parentFile!!.mkdirs()
                val chapterScript = "document.addEventListener('DOMContentLoaded',()=>setTimeout(()=>{document.querySelector('#LNReader-chapter p').append(' '+localStorage.getItem('fixture')+' Script ready.');sessionStorage.setItem('rendered','yes');const hidden=document.createElement('p');hidden.className='fixture-hidden';hidden.textContent='Do not render this.';document.querySelector('#LNReader-chapter').append(hidden);},30));"
                val chapterCss = ".fixture-hidden { display:none }"
                fun hash(value: String) = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
                val pluginWithAssets = plugin.copy(customJS = "https://example.invalid/chapter.js", customCSS = "https://example.invalid/chapter.css")
                fun fixtureClient(failCss: Boolean = false) = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                    val path = chain.request().url.encodedPath
                    val text = when (path) { "/chapter.js" -> chapterScript; "/chapter.css" -> chapterCss; else -> code }
                    okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(if (failCss && path == "/chapter.css") 503 else 200).message("Fixture")
                        .body(okhttp3.ResponseBody.create(null, text)).build()
                }.build()
                LnReaderPluginStore(targetContext).install(pluginWithAssets, fixtureClient(), targetContext)
                val installedRecord = installedFile.readText()
                check(runCatching { LnReaderPluginStore(targetContext).install(pluginWithAssets, fixtureClient(true), targetContext) }.isFailure)
                check(installedFile.readText() == installedRecord)
                val source = app.container.sourceHost.loadSources(plugin.packageName).single()
                val sourceNovel = eu.kanade.tachiyomi.source.model.SManga.create().apply { url = "paper"; title = "The Paper Observatory" }
                val chapterList = source.getMangaUpdate(sourceNovel, emptyList(), true, true).chapters
                check(chapterList?.map { it.url } == listOf("2", "1"))
                val chapter = SourceChapter(plugin.sourceId, "paper", "paper/chapter-1", "Fictional dawn", 1f, null, null)
                val reader = com.tankobun.app.reader.ReaderDataSource(app.container)
                ReaderPageCache.clear(targetContext)
                val pages = reader.loadPagesForChapter(199299, chapter, plugin.descriptor())
                check(pages.size == 3 && pages.last().novelBlock!!.endOfChapter)
                check(pages[1].novelBlock!!.text.endsWith("Script ready."))
                check(pages[1].novelBlock!!.text.contains("local-value"))
                check(pluginSettings.webStorage().getJSONObject("https://example.invalid").getJSONObject("session").getString("rendered") == "yes")
                val goodRecord = installedFile.readText()
                val brokenScript = "throw new Error('Fictional script failure');"
                installedFile.writeText(JSONObject(goodRecord).put("chapterJS", brokenScript).put("chapterJSSha256", hash(brokenScript)).toString())
                check(runCatching { reader.pagesForSource(199299, chapter, plugin.descriptor()) }.isFailure)
                installedFile.writeText(goodRecord)
                // Remove executable access; a cached chapter must still open without executing the source.
                val savedCode = installedFile.readText()
                installedFile.delete()
                app.container.sourceHost.clearCache(plugin.packageName)
                check(reader.loadPagesForChapter(199299, chapter, plugin.descriptor()) == pages)
                installedFile.writeText(savedCode)
                val progress = reader.saveProgress(199299, chapter, pages, ReaderMode.PAGED, 1, 19, System.currentTimeMillis())
                check(!progress.completed && reader.cachedProgressForChapter(199299, chapter.url)?.pageScrollOffset == 19)
                val job = DownloadJob("novel-fixture-job", 199299, plugin.sourceId, "paper", chapter.url, chapter.name, DownloadState.QUEUED, 0, 0, 0, System.currentTimeMillis(), System.currentTimeMillis())
                app.container.database.downloadDao().upsertDownload(job.toEntity())
                com.tankobun.core.downloads.DownloadWorkerDelegateRegistry.delegate!!.run(job.id)
                check(app.container.database.downloadDao().getDownload(job.id)?.state == DownloadState.COMPLETE)
                installedFile.delete()
                app.container.sourceHost.clearCache(plugin.packageName)
                ReaderPageCache.clear(targetContext)
                val offline = reader.loadPagesForChapter(199299, chapter, null)
                check(offline.map { it.novelBlock } == pages.map { it.novelBlock })
                check(reader.saveProgress(199299, chapter, offline, ReaderMode.PAGED, offline.lastIndex, 0, System.currentTimeMillis()).completed)
                app.container.downloadCoordinator.remove(job.id)
                val backup = com.tankobun.app.backup.AppSettingsBackupDataSource(app.container)
                val file = File(targetContext.cacheDir, "novel-settings-test.json")
                val savedPreferences = original.copy(fontSize = 27, theme = NovelTheme.SEPIA)
                val urls = listOf("https://example.invalid/manga-index.json", "https://example.invalid/novel-index.json")
                backup.saveBackup(android.net.Uri.fromFile(file), com.tankobun.app.state.TankobunUiState(novelReaderPreferences = savedPreferences, extensionRepositories = urls, allInstalledSources = listOf(plugin.descriptor())))
                val backupText = file.readText()
                check(!backupText.contains("fictional-test-secret") && !backupText.contains("local-value") && !backupText.contains("session-value"))
                pluginSettings.save(JSONObject().put("hideLocked", false))
                backup.restoreBackup(android.net.Uri.fromFile(file))
                check(store.novelReaderPreferences() == savedPreferences && store.extensionRepositories() == urls)
                check(pluginSettings.values().getBoolean("hideLocked"))
                store.saveNovelReaderPreferences(original)
                store.saveExtensionRepositories(emptyList())
                file.delete()
                val extensions = app.container.extensionScanner.installedExtensions().filter { it.contentKind == ReadingContentKind.NOVEL && !it.packageName.startsWith(LNREADER_PACKAGE_PREFIX) }
                extensions.forEach { descriptor ->
                    app.container.extensionTrustStore.untrustedExtension(descriptor)?.let { check(app.container.extensionTrustStore.approve(it)) }
                    val sources = app.container.sourceHost.loadSources(descriptor.packageName)
                    check(sources.isNotEmpty()) { "Novel APK failed to initialize: ${descriptor.packageName}" }
                    sources.forEach { check(it.readingContentKind() == ReadingContentKind.NOVEL); it.getFilterList() }
                }
                options.getString("pluginManifest")?.let { encoded ->
                    val manifest = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
                    val actual = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<LnReaderPlugin>(manifest)
                    LnReaderPluginStore(targetContext).install(actual, app.container.okHttpClient, targetContext)
                    check(app.container.extensionScanner.installedExtensions().any { it.packageName == actual.packageName })
                    check(app.container.sourceHost.loadSources(actual.packageName).single().readingContentKind() == ReadingContentKind.NOVEL)
                    check(LnReaderPluginStore(targetContext).record(actual.packageName) != null)
                }
                val apkReport = if (options.containsKey("apkPreferences")) "PASS: installed APK settings / native dialogs / callbacks / dependencies / rotation draft restore / per-source persistence / backup and restore\n" else ""
                report.putString("stream", apkReport + "PASS: WebView CommonJS / selectors / dayjs / URL encoding / storage / pagination / chapter scripts / origin-scoped website session / plugin preferences backup / cache / offline downloads / paragraph progress; novel APKs initialized: ${extensions.size}\n")
            }
            finish(-1, report)
        } catch (error: Throwable) {
            report.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(0, report)
        }
    }
}
