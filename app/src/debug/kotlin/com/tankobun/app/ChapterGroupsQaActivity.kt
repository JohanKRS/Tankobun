package com.tankobun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.media.MangaDetailScreen
import com.tankobun.core.database.DownloadPageEntity
import com.tankobun.core.database.toEntity
import com.tankobun.core.extensions.novel.NovelDocument
import com.tankobun.core.model.*
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Fictional, offline-only fixture. This activity and its content are excluded from release builds. */
class ChapterGroupsQaActivity : ComponentActivity() {
    lateinit var model: MainViewModel
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val container = (application as TankobunApplication).container
        model = androidx.lifecycle.ViewModelProvider(this, object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                MainViewModel(container).also { model ->
                    MainViewModel::class.java.declaredFields.filter { Job::class.java.isAssignableFrom(it.type) }.forEach { field ->
                        field.isAccessible = true; (field.get(model) as? Job)?.cancel()
                    }
                } as T
        })[MainViewModel::class.java]
        val field = MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST") val state = field.get(model) as MutableStateFlow<TankobunUiState>
        if (state.value.selectedMedia == null) lifecycleScope.launch {
            val media = AnilistMedia(199299, null, AnilistTitle("Caderno das Estrelas", null, null, "Caderno das Estrelas"),
                null, null, null, 3, 1, "NOVEL", "RELEASING", null, null, 2026, null, null, emptyList(), emptyList(), false, null)
            val chapters = listOf(1 to "Equipe Aurora", 1 to "Equipe Brisa", 2 to "Equipe Brisa", 3 to "Equipe Aurora", 3 to "Equipe Brisa").map { (number, group) ->
                SourceChapter(992, "paper", "paper/$number/$group", "Capítulo $number", number.toFloat(), group, null)
            }
            container.database.chapterDao().upsertChapters(chapters.map { it.toEntity(System.currentTimeMillis()) })
            chapters.forEachIndexed { index, chapter ->
                val pages = NovelDocument.parse("<p>Texto original para testar os créditos de tradução.</p><p>Uma estrela de papel marcou a próxima página do caderno.</p>", "https://example.invalid")
                val job = DownloadJob("chapter-groups-qa-$index", media.id, chapter.sourceId, chapter.mangaUrl, chapter.url, chapter.name,
                    DownloadState.COMPLETE, pages.size, pages.size, 0, 1, 1)
                container.database.downloadDao().upsertDownload(job.toEntity())
                pages.forEach { page ->
                    val file = File(filesDir, "chapter-groups-qa/$index/${page.index}.json").apply {
                        parentFile!!.mkdirs(); writeBytes(NovelDocument.encodeBlock(page.novelBlock!!))
                    }
                    container.database.downloadPageDao().upsertPage(DownloadPageEntity(job.id, media.id, chapter.sourceId, chapter.mangaUrl,
                        chapter.url, page.index, page.imageUrl, file.absolutePath, 1))
                }
            }
            val source = SourceDescriptor(992, "Acervo de teste", "pt", "fixture.chaptergroups", "1", 1, false, true, ReadingContentKind.NOVEL)
            state.value = state.value.copy(selectedMedia = media, selectedSourceId = 992, selectedSourcePackageName = "fixture.chaptergroups",
                installedSources = listOf(source), allInstalledSources = listOf(source),
                selectedSourceManga = SourceManga(992, "paper", "Caderno das Estrelas", null, null, null, null, null),
                sourceChapters = chapters.reversed(), libraryMode = LibraryMode.LOCAL, autoUpdateStatusFromReading = false,
                anilistAutoSyncReaderProgress = false, anilistSyncManualReadProgress = false, keepNextTenDownloads = false,
                chapterGroupPreferences = container.settingsStore.chapterGroupPreferences(), busy = false)
        }
        setContent {
            val snapshot = state.collectAsState().value
            TankobunTheme(container.settingsStore.themePreference()) {
                snapshot.selectedMedia?.let { media ->
                    if (!intent.getBooleanExtra("contract", false)) MangaDetailScreen(snapshot, model, media, {}, {}, {}, {})
                }
            }
        }
    }
}
