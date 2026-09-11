package com.tankobun.app.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.tankobunString
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.previousInReadingOrderBefore
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.model.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NovelReader(state: TankobunUiState, viewModel: MainViewModel) {
    val chapter = state.activeChapter ?: return
    if (state.readerPages.isEmpty()) return
    val p = state.novelReaderPreferences
    var controls by rememberSaveable(state.selectedMedia?.id) { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var chapters by rememberSaveable { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(chapter.url) { mutableStateOf("") }
    var jump by remember { mutableStateOf<NovelJump?>(null) }
    var request by remember { mutableIntStateOf(0) }
    var turnPage by remember { mutableStateOf<(Int) -> Unit>({}) }
    var seekPage by remember { mutableStateOf<(Int) -> Unit>({}) }
    var pageNumber by remember { mutableIntStateOf(1) }
    var lastPageNumber by remember { mutableIntStateOf(1) }
    var pageCount by remember { mutableIntStateOf(1) }
    val dark = p.theme == NovelTheme.DARK || p.theme == NovelTheme.BLACK || (p.theme == NovelTheme.SYSTEM && isSystemInDarkTheme())
    val background = when (p.theme) {
        NovelTheme.SEPIA -> Color(0xFFF3E8CF)
        NovelTheme.BLACK -> Color.Black
        else -> if (dark) Color(0xFF191B1F) else Color(0xFFFAF9F6)
    }
    val foreground = if (dark) Color(0xFFE1E1E3) else Color(0xFF302D29)
    val font = when (p.font) { NovelFont.SERIF -> FontFamily.Serif; NovelFont.SANS_SERIF -> FontFamily.SansSerif; NovelFont.MONOSPACE -> FontFamily.Monospace }
    // Color and chrome changes do not repaginate the document.
    val textStyle = remember(font, p.fontSize, p.lineHeightPercent, p.justified) {
        TextStyle(fontFamily = font, fontSize = p.fontSize.sp, lineHeight = (p.fontSize * p.lineHeightPercent / 100f).sp,
            textAlign = if (p.justified) TextAlign.Justify else TextAlign.Start,
            platformStyle = PlatformTextStyle(includeFontPadding = false))
    }
    val view = LocalView.current
    val window = remember(view) { generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }.filterIsInstance<android.app.Activity>().firstOrNull()?.window }
    DisposableEffect(view, window) {
        val keepScreenOn = view.keepScreenOn
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val oldStatus = controller?.isAppearanceLightStatusBars
        val oldNavigation = controller?.isAppearanceLightNavigationBars
        val oldBehavior = controller?.systemBarsBehavior
        val insets = ViewCompat.getRootWindowInsets(view)
        onDispose {
            view.keepScreenOn = keepScreenOn
            if (oldStatus != null) controller.isAppearanceLightStatusBars = oldStatus
            if (oldNavigation != null) controller.isAppearanceLightNavigationBars = oldNavigation
            if (oldBehavior != null) controller.systemBarsBehavior = oldBehavior
            listOf(WindowInsetsCompat.Type.statusBars(), WindowInsetsCompat.Type.navigationBars()).forEach { type ->
                if (insets?.isVisible(type) != false) controller?.show(type) else controller?.hide(type)
            }
        }
    }
    SideEffect {
        view.keepScreenOn = p.keepScreenOn
        window?.let { WindowCompat.getInsetsController(it, view) }?.apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (controls || settings || chapters) show(WindowInsetsCompat.Type.systemBars()) else hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    BackHandler {
        when { searching -> searching = false; controls -> controls = false; else -> viewModel.closeReader() }
    }
    val segments = remember(chapter, state.readerPages, state.readerPreviousSegment, state.readerNextSegment, p.continuousReading) {
        buildList {
            if (p.continuousReading) state.readerPreviousSegment?.let(::add)
            add(ReaderChapterSegment(chapter, state.readerPages))
            if (p.continuousReading) state.readerNextSegment?.let(::add)
        }.distinctBy { it.chapter.url }
    }
    LaunchedEffect(chapter.url, p.continuousReading) { viewModel.ensureNovelReaderSegmentsLoaded() }
    val toggleLabel = tankobunString(if (controls) R.string.reader_hide_controls else R.string.reader_show_controls)
    val previousPageLabel = tankobunString(R.string.novel_previous_page)
    val nextPageLabel = tankobunString(R.string.novel_next_page)
    val scheme = if (dark) darkColorScheme(surface = background, onSurface = foreground, primary = foreground, onPrimary = background)
        else lightColorScheme(surface = background, onSurface = foreground, primary = foreground, onPrimary = background)
    MaterialTheme(colorScheme = scheme) {
        Surface(color = background, contentColor = foreground, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)
                    .testTag("novel_reading_area")
                    .semantics {
                        customActions = buildList {
                            add(CustomAccessibilityAction(toggleLabel) { controls = !controls; if (!controls) searching = false; true })
                            if (p.readingMode == NovelReadingMode.PAGED) {
                                add(CustomAccessibilityAction(previousPageLabel) { turnPage(-1); true })
                                add(CustomAccessibilityAction(nextPageLabel) { turnPage(1); true })
                            }
                        }
                    }
                    .pointerInput(controls, p.readingMode) {
                        detectTapGestures(onTap = { tap ->
                            when {
                                tap.x >= size.width * ReaderPreviousTapZoneEndFraction && tap.x <= size.width * ReaderNextTapZoneStartFraction -> {
                                    controls = !controls
                                    if (!controls) searching = false
                                }
                                !controls && p.readingMode == NovelReadingMode.PAGED -> turnPage(if (tap.x < size.width * ReaderPreviousTapZoneEndFraction) -1 else 1)
                            }
                        })
                    }) {
                    val boundary: @Composable (SourceChapter, Boolean) -> Unit = { item, before ->
                        val adjacent = if (before) state.sourceChapters.previousInReadingOrderBefore(item) else state.sourceChapters.nextInReadingOrderAfter(item)
                        val present = segments.any { it.chapter.url == adjacent?.url }
                        val loading = if (before) state.novelPreviousLoading else state.novelNextLoading
                        if (!before || (item.url == chapter.url && adjacent != null && !present && p.continuousReading)) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!before) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                                    Text(tankobunString(R.string.novel_chapter_end), style = MaterialTheme.typography.labelLarge)
                                }
                                if (adjacent != null && !present && item.url == chapter.url) {
                                    if (p.continuousReading) {
                                        if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        else TextButton(onClick = viewModel::ensureNovelReaderSegmentsLoaded) { Text(tankobunString(R.string.common_retry)) }
                                    } else TextButton(onClick = if (before) viewModel::openPreviousChapter else viewModel::openNextChapter) {
                                        Text(tankobunString(if (before) R.string.reader_previous_chapter else R.string.reader_next_chapter))
                                    }
                                }
                            }
                        }
                    }
                    val position: (NovelAnchor) -> Unit = { viewModel.setNovelReaderPosition(it.chapterUrl, it.block, it.character) }
                    if (p.readingMode == NovelReadingMode.SCROLL) NovelScrollContent(state, segments, textStyle, jump, position, boundary)
                    else NovelPagedContent(state, segments, textStyle, jump, position, { turnPage = it }, { seekPage = it }, { number, last, count -> pageNumber = number; lastPageNumber = last; pageCount = count }, boundary)
                }
                if (controls) {
                    Surface(color = background.copy(alpha = 0.97f), modifier = Modifier.align(Alignment.TopCenter)) {
                        Column(Modifier.statusBarsPadding()) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = viewModel::closeReader) { Icon(TankobunIcons.ArrowBack, tankobunString(R.string.reader_close)) }
                                Text(chapter.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                                IconButton(onClick = { searching = !searching }) { Icon(TankobunIcons.Search, tankobunString(R.string.common_search)) }
                                TextButton(onClick = { settings = true }) { Text("Aa", fontSize = 22.sp) }
                                IconButton(onClick = { chapters = true }) { Icon(TankobunIcons.FormatListBulleted, tankobunString(R.string.novel_chapters)) }
                                IconButton(onClick = { controls = false; searching = false }) { Icon(TankobunIcons.VisibilityOff, tankobunString(R.string.reader_hide_controls)) }
                            }
                            if (searching) {
                                val found = remember(query, state.readerPages) { if (query.isBlank()) emptyList() else state.readerPages.indices.filter { state.readerPages[it].novelBlock?.text?.contains(query, true) == true } }
                                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, label = { Text(tankobunString(R.string.common_search)) })
                                    TextButton(enabled = found.isNotEmpty(), onClick = {
                                        val block = found.firstOrNull { it > state.currentPageIndex } ?: found.first()
                                        jump = NovelJump(NovelAnchor(chapter.url, block), ++request)
                                    }) { Text("${found.size} →") }
                                }
                            }
                        }
                    }
                    Surface(color = background.copy(alpha = 0.97f), modifier = Modifier.align(Alignment.BottomCenter)) {
                        Column(Modifier.navigationBarsPadding().padding(horizontal = 8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(enabled = state.sourceChapters.previousInReadingOrderBefore(chapter) != null, onClick = viewModel::openPreviousChapter) { Icon(TankobunIcons.ArrowBack, tankobunString(R.string.reader_previous_chapter)) }
                                val paged = p.readingMode == NovelReadingMode.PAGED
                                Slider(if (paged) (pageNumber - 1).toFloat() else state.currentPageIndex.toFloat(), {
                                    if (paged) seekPage(it.roundToInt())
                                    else jump = NovelJump(NovelAnchor(chapter.url, it.roundToInt().coerceIn(0, state.readerPages.lastIndex)), ++request)
                                }, valueRange = 0f..(if (paged) pageCount - 1 else state.readerPages.lastIndex).coerceAtLeast(1).toFloat(), modifier = Modifier.weight(1f))
                                Text(if (p.readingMode == NovelReadingMode.PAGED) (if (lastPageNumber == pageNumber) "$pageNumber / $pageCount" else "$pageNumber–$lastPageNumber / $pageCount") else "${(state.currentPageIndex * 100f / state.readerPages.lastIndex.coerceAtLeast(1)).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                                IconButton(enabled = state.sourceChapters.nextInReadingOrderAfter(chapter) != null, onClick = viewModel::openNextChapter) { Icon(TankobunIcons.ChevronRight, tankobunString(R.string.reader_next_chapter)) }
                            }
                            Text(tankobunString(R.string.novel_controls_hint), Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    if (settings) NovelReaderSettings(p, viewModel::setNovelReaderPreferences) { settings = false }
    if (chapters) ModalBottomSheet(onDismissRequest = { chapters = false }) {
        Text(tankobunString(R.string.novel_chapters), Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        val list = rememberLazyListState(initialFirstVisibleItemIndex = state.sourceChapters.indexOfFirst { it.url == chapter.url }.coerceAtLeast(0))
        LazyColumn(state = list, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            itemsIndexed(state.sourceChapters, key = { _, item -> item.url }) { _, item ->
                ListItem(headlineContent = { Text(item.name) }, supportingContent = { if (state.chapterProgress[item.url]?.completed == true) Text("✓") },
                    colors = ListItemDefaults.colors(containerColor = if (item.url == chapter.url) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                    modifier = Modifier.clickable { chapters = false; viewModel.persistReaderProgress(); viewModel.openChapter(item) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NovelReaderSettings(value: NovelReaderPreferences, onChange: (NovelReaderPreferences) -> Unit, onClose: () -> Unit) {
    AlertDialog(onDismissRequest = onClose, title = { Text(tankobunString(R.string.novel_appearance)) },
        confirmButton = { TextButton(onClick = onClose) { Text(tankobunString(R.string.common_close)) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(tankobunString(R.string.novel_reading_mode), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NovelReadingMode.entries.forEach { mode -> FilterChip(value.readingMode == mode, { onChange(value.copy(readingMode = mode)) }, label = { Text(tankobunString(if (mode == NovelReadingMode.PAGED) R.string.reader_paged else R.string.novel_scroll)) }) }
                }
                NovelToggle(tankobunString(R.string.novel_two_pages), value.landscapeTwoPages) { onChange(value.copy(landscapeTwoPages = it)) }
                NovelToggle(tankobunString(R.string.novel_continuous), value.continuousReading) { onChange(value.copy(continuousReading = it)) }
                Text(tankobunString(R.string.novel_continuous_hint), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NovelFont.entries.forEach { font -> FilterChip(value.font == font, { onChange(value.copy(font = font)) }, label = { Text(tankobunString(when (font) { NovelFont.SERIF -> R.string.novel_serif; NovelFont.SANS_SERIF -> R.string.novel_sans; NovelFont.MONOSPACE -> R.string.novel_mono })) }) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NovelTheme.entries.forEach { theme -> FilterChip(value.theme == theme, { onChange(value.copy(theme = theme)) }, label = { Text(tankobunString(when (theme) { NovelTheme.SYSTEM -> R.string.novel_system; NovelTheme.LIGHT -> R.string.novel_light; NovelTheme.SEPIA -> R.string.novel_sepia; NovelTheme.DARK -> R.string.novel_dark; NovelTheme.BLACK -> R.string.novel_black })) }) }
                }
                NovelSlider(tankobunString(R.string.novel_font_size), value.fontSize, 14..36) { onChange(value.copy(fontSize = it)) }
                NovelSlider(tankobunString(R.string.novel_line_spacing), value.lineHeightPercent, 120..220) { onChange(value.copy(lineHeightPercent = it)) }
                NovelSlider(tankobunString(R.string.novel_paragraph_spacing), value.paragraphSpacing, 0..32) { onChange(value.copy(paragraphSpacing = it)) }
                NovelSlider(tankobunString(R.string.novel_margins), value.margin, 8..48) { onChange(value.copy(margin = it)) }
                NovelSlider(tankobunString(R.string.novel_text_width), value.maxTextWidth, 400..1000) { onChange(value.copy(maxTextWidth = it)) }
                NovelToggle(tankobunString(R.string.novel_justify), value.justified) { onChange(value.copy(justified = it)) }
                NovelToggle(tankobunString(R.string.novel_keep_awake), value.keepScreenOn) { onChange(value.copy(keepScreenOn = it)) }
            }
        })
}

@Composable
private fun NovelToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onCheckedChange = null)
    }
}

@Composable
private fun NovelSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column { Text("$label · $value"); Slider(value.toFloat(), { onChange(it.roundToInt()) }, valueRange = range.first.toFloat()..range.last.toFloat()) }
}
