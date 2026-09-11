package com.tankobun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.reader.FullScreenReader
import com.tankobun.core.database.DownloadPageEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.extensions.novel.NovelDocument
import com.tankobun.core.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Original offline chapters exercise the real reader loader without an installed source or network. */
class NovelReaderQaActivity : ComponentActivity() {
    lateinit var model: MainViewModel
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val container = (application as TankobunApplication).container
        model = androidx.lifecycle.ViewModelProvider(this, object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = MainViewModel(container).also { model ->
                MainViewModel::class.java.declaredFields.filter { Job::class.java.isAssignableFrom(it.type) }.forEach { field ->
                    field.isAccessible = true
                    (field.get(model) as? Job)?.cancel()
                }
            } as T
        })[MainViewModel::class.java]
        val field = MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST") val state = field.get(model) as MutableStateFlow<TankobunUiState>
        if (state.value.selectedMedia == null) lifecycleScope.launch {
            val media = AnilistMedia(199199, null, AnilistTitle("The Paper Observatory", null, null, "The Paper Observatory"), null, null, null, 4, 1, "NOVEL", "RELEASING", null, null, 2026, null, null, listOf("Fantasy"), emptyList(), false, null)
            val chapters = (1..4).map { number -> SourceChapter(991, "paper", "paper/chapter-$number", "Paper Observatory · Chapter $number", number.toFloat(), null, null) }
            chapters.forEach { chapter ->
                val html = buildString {
                    append("<h2>${chapter.name}</h2>")
                    repeat(12) { n -> append("<p>Chapter ${chapter.chapterNumber.toInt()}, paragraph ${n+1}. At dawn, Mara opened the roof of her paper observatory. Every star was a tiny folded lantern, and every map carried a path she had yet to explore. She checked the brass compass, wrote a note in her journal, and waited for the wind to turn the quiet sky. The final words of this paragraph are silver lantern.</p>") }
                    append("<p>Long paragraph. ")
                    repeat(60) { append("Mara followed the paper river beyond the quiet observatory. ") }
                    append("The last words are golden compass.</p>")
                }
                val pages = NovelDocument.parse(html, "https://example.invalid")
                val job = DownloadJob("reader-qa-${chapter.chapterNumber}", media.id, chapter.sourceId, chapter.mangaUrl, chapter.url, chapter.name, DownloadState.COMPLETE, pages.size, pages.size, 0, 1, 1)
                container.database.downloadDao().upsertDownload(job.toEntity())
                val entities = pages.map { page ->
                    val file = File(filesDir, "novel-reader-qa/${chapter.chapterNumber}/${page.index}.json").apply { parentFile!!.mkdirs(); writeBytes(NovelDocument.encodeBlock(page.novelBlock!!)) }
                    DownloadPageEntity(job.id, media.id, chapter.sourceId, chapter.mangaUrl, chapter.url, page.index, page.imageUrl, file.absolutePath, 1)
                }
                entities.forEach { container.database.downloadPageDao().upsertPage(it) }
            }
            state.value = state.value.copy(selectedMedia = media, sourceChapters = chapters.reversed(), libraryMode = LibraryMode.LOCAL, autoUpdateStatusFromReading = false)
            model.openChapter(chapters[(intent.getIntExtra("chapter", 2) - 1).coerceIn(0, 3)], startPageIndexOverride = if (intent.hasExtra("block")) intent.getIntExtra("block", 0) else null)
        }
        setContent { val snapshot = state.collectAsState().value; TankobunTheme(container.settingsStore.themePreference()) { if (!intent.getBooleanExtra("contract", false)) FullScreenReader(snapshot, model) } }
    }
}
