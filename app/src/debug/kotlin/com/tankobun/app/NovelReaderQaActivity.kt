package com.tankobun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.reader.NovelReader
import com.tankobun.core.extensions.novel.NovelDocument
import com.tankobun.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow

/** Fictional, debug-only reader fixture. It cannot run against the normal application ID. */
class NovelReaderQaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val container = (application as TankobunApplication).container
        val model = androidx.lifecycle.ViewModelProvider(this, object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = MainViewModel(container) as T
        })[MainViewModel::class.java]
        val chapter = SourceChapter(991, "paper", "paper/chapter-1", "The Paper Observatory", 1f, null, null)
        val media = AnilistMedia(199199, null, AnilistTitle("The Paper Observatory", null, null, "The Paper Observatory"), null, null, null, 2, 1, "NOVEL", "RELEASING", null, null, 2026, null, null, listOf("Fantasy"), emptyList(), false, null)
        val html = buildString {
            append("<h2>The Paper Observatory</h2><p><i>An original, fictional chapter for reader testing.</i></p>")
            repeat(32) { n -> append("<p>Paragraph ${n+1}. At dawn, Mara opened the roof of her paper observatory. Every star was a tiny folded lantern, and every map carried a path she had yet to explore. She checked the brass compass, wrote a note in her journal, and waited for the wind to turn the quiet sky.</p>") }
        }
        val field = MainViewModel::class.java.getDeclaredField("_state").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST") val state = field.get(model) as MutableStateFlow<TankobunUiState>
        val progress = savedInstanceState?.getInt("block") ?: intent.getIntExtra("block", 0)
        if (state.value.selectedMedia == null) state.value = state.value.copy(selectedMedia = media, activeChapter = chapter, sourceChapters = listOf(chapter), readerPages = NovelDocument.parse(html, "https://example.invalid"), currentPageIndex = progress, novelReaderPreferences = container.settingsStore.novelReaderPreferences())
        setContent { val snapshot = state.collectAsState().value; TankobunTheme(container.settingsStore.themePreference()) { NovelReader(snapshot, model) } }
    }
}
