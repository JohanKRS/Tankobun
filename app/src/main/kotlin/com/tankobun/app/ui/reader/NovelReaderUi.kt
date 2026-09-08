package com.tankobun.app.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.tankobun.app.MainViewModel
import com.tankobun.app.R
import com.tankobun.app.ReaderPageImageModel
import com.tankobun.app.tankobunString
import com.tankobun.app.logic.nextInReadingOrderAfter
import com.tankobun.app.logic.previousInReadingOrderBefore
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.ui.icons.TankobunIcons
import com.tankobun.core.model.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NovelReader(state: TankobunUiState, viewModel: MainViewModel) {
    val chapter = state.activeChapter ?: return
    val pages = state.readerPages
    val preferences = state.novelReaderPreferences
    val view = LocalView.current
    DisposableEffect(view, preferences.keepScreenOn) {
        val previous = view.keepScreenOn
        view.keepScreenOn = preferences.keepScreenOn
        onDispose { view.keepScreenOn = previous }
    }
    BackHandler { viewModel.closeReader() }
    val dark = preferences.theme == NovelTheme.DARK || preferences.theme == NovelTheme.BLACK ||
        (preferences.theme == NovelTheme.SYSTEM && isSystemInDarkTheme())
    val background = when (preferences.theme) {
        NovelTheme.SEPIA -> Color(0xFFF3E8CF); NovelTheme.BLACK -> Color.Black
        else -> if (dark) Color(0xFF191B1F) else Color(0xFFFAF9F6)
    }
    val foreground = if (dark) Color(0xFFE1E1E3) else Color(0xFF302D29)
    val window = remember(view) { generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }.filterIsInstance<android.app.Activity>().firstOrNull()?.window }
    DisposableEffect(window, view) {
        val controller = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val oldStatus = controller?.isAppearanceLightStatusBars
        val oldNavigation = controller?.isAppearanceLightNavigationBars
        onDispose {
            if (oldStatus != null) controller.isAppearanceLightStatusBars = oldStatus
            if (oldNavigation != null) controller.isAppearanceLightNavigationBars = oldNavigation
        }
    }
    SideEffect {
        window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }?.apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    val font = when (preferences.font) { NovelFont.SERIF -> FontFamily.Serif; NovelFont.SANS_SERIF -> FontFamily.SansSerif; NovelFont.MONOSPACE -> FontFamily.Monospace }
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val configuration = LocalConfiguration.current
    val layouts = remember(chapter.url, preferences, configuration.screenWidthDp, configuration.fontScale) { mutableStateMapOf<Int, TextLayoutResult>() }
    var restoring by remember(chapter.url) { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var chapters by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var query by remember(chapter.url) { mutableStateOf("") }
    var controls by remember { mutableStateOf(true) }
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(chapter.url, preferences, configuration.screenWidthDp, configuration.fontScale) {
        restoring = true
        val target = latestState.currentPageIndex.coerceIn(0, pages.lastIndex)
        val character = latestState.currentPageScrollOffset
        list.scrollToItem(target)
        if (pages[target].novelBlock?.text?.isNotEmpty() == true) {
            val layout = snapshotFlow { layouts[target] }.filterNotNull().first()
            val line = layout.getLineForOffset(character.coerceIn(0, layout.layoutInput.text.length))
            list.scrollToItem(target, layout.getLineTop(line).roundToInt().coerceAtLeast(0))
        }
        restoring = false
    }
    LaunchedEffect(chapter.url, list, layouts) {
        snapshotFlow {
            if (restoring) null else {
                val endVisible = list.layoutInfo.visibleItemsInfo.any { it.index == pages.lastIndex && it.offset + it.size <= list.layoutInfo.viewportEndOffset }
                val index = if (endVisible) pages.lastIndex else list.firstVisibleItemIndex.coerceIn(0, pages.lastIndex)
                val layout = layouts[index]
                val character = if (layout != null && !endVisible) layout.getLineStart(layout.getLineForVerticalPosition(list.firstVisibleItemScrollOffset.toFloat())) else 0
                index to character
            }
        }.filterNotNull().distinctUntilChanged().collect { (index, character) -> viewModel.setReaderPage(index, character) }
    }
    Surface(color = background, contentColor = foreground, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::closeReader) { Icon(TankobunIcons.ArrowBack, tankobunString(R.string.reader_close)) }
                Text(chapter.name, Modifier.weight(1f).clickable { controls = !controls }, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { searching = !searching }) { Icon(TankobunIcons.Search, tankobunString(R.string.common_search)) }
                TextButton(onClick = { settings = true }) { Text("Aa", color = foreground, fontSize = 22.sp) }
                IconButton(onClick = { chapters = true }) { Icon(TankobunIcons.FormatListBulleted, tankobunString(R.string.novel_chapters)) }
            }
            if (searching) {
                val found = remember(query, pages) { if (query.isBlank()) emptyList() else pages.indices.filter { pages[it].novelBlock?.text?.contains(query, true) == true } }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(query, { query = it }, Modifier.weight(1f), singleLine = true, label = { Text(tankobunString(R.string.common_search)) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = foreground, unfocusedTextColor = foreground,
                            focusedLabelColor = foreground, unfocusedLabelColor = foreground.copy(alpha = 0.7f),
                            focusedBorderColor = foreground, unfocusedBorderColor = foreground.copy(alpha = 0.4f), cursorColor = foreground))
                    TextButton(enabled = found.isNotEmpty(), onClick = { scope.launch { list.animateScrollToItem(found.firstOrNull { it > list.firstVisibleItemIndex } ?: found.first()) } }) { Text("${found.size} →") }
                }
            }
            LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(horizontal = preferences.margin.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(preferences.paragraphSpacing.dp)) {
                itemsIndexed(pages, key = { _, page -> "${chapter.url}:${page.index}" }) { index, page ->
                    val block = page.novelBlock
                    if (block?.endOfChapter == true) {
                        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tankobunString(R.string.novel_chapter_end), style = MaterialTheme.typography.labelLarge)
                            if (state.sourceChapters.nextInReadingOrderAfter(chapter) != null) TextButton(onClick = viewModel::openNextChapter) { Text(tankobunString(R.string.reader_next_chapter)) }
                        }
                    } else if (block != null) {
                        val content = remember(block.html) { AnnotatedString.fromHtml(block.html) }
                        SelectionContainer {
                            Text(content, Modifier.widthIn(max = 720.dp).fillMaxWidth(), color = foreground, fontFamily = font,
                                fontSize = (preferences.fontSize + if (block.headingLevel > 0) 5 else 0).sp,
                                lineHeight = (preferences.fontSize * preferences.lineHeightPercent / 100f).sp,
                                fontWeight = if (block.headingLevel > 0) FontWeight.Bold else FontWeight.Normal,
                                textAlign = if (preferences.justified) TextAlign.Justify else TextAlign.Start,
                                onTextLayout = { layouts[index] = it })
                        }
                    } else {
                        val source = state.allInstalledSources.firstOrNull { it.id == chapter.sourceId }
                        val model: Any = page.cachedFilePath?.let(::File) ?: source?.let { ReaderPageImageModel(state.selectedMedia!!.id, chapter, it, page) } ?: page.imageUrl
                        AsyncImage(model, null, Modifier.widthIn(max = 720.dp).fillMaxWidth().heightIn(min = 160.dp))
                    }
                }
            }
            if (controls) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(enabled = state.sourceChapters.previousInReadingOrderBefore(chapter) != null, onClick = viewModel::openPreviousChapter) { Icon(TankobunIcons.ArrowBack, tankobunString(R.string.reader_previous_chapter)) }
                    Slider(value = state.currentPageIndex.toFloat(), onValueChange = { scope.launch { list.scrollToItem(it.roundToInt().coerceIn(0, pages.lastIndex)) } }, valueRange = 0f..pages.lastIndex.coerceAtLeast(1).toFloat(), modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = foreground, activeTrackColor = foreground.copy(alpha = 0.75f), inactiveTrackColor = foreground.copy(alpha = 0.10f)))
                    Text("${((state.currentPageIndex.toFloat() / pages.lastIndex.coerceAtLeast(1)) * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    IconButton(enabled = state.sourceChapters.nextInReadingOrderAfter(chapter) != null, onClick = viewModel::openNextChapter) { Icon(TankobunIcons.ChevronRight, tankobunString(R.string.reader_next_chapter)) }
                }
            }
        }
    }
    if (settings) NovelReaderSettings(preferences, { viewModel.setNovelReaderPreferences(it) }, { settings = false })
    if (chapters) ModalBottomSheet(onDismissRequest = { chapters = false }) {
        Text(tankobunString(R.string.novel_chapters), Modifier.padding(24.dp), style = MaterialTheme.typography.titleLarge)
        val chapterList = rememberLazyListState(initialFirstVisibleItemIndex = state.sourceChapters.indexOfFirst { it.url == chapter.url }.coerceAtLeast(0))
        LazyColumn(state = chapterList, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            itemsIndexed(state.sourceChapters, key = { _, item -> item.url }) { _, item ->
                ListItem(headlineContent = { Text(item.name) }, supportingContent = { if (state.chapterProgress[item.url]?.completed == true) Text("✓") },
                    colors = ListItemDefaults.colors(containerColor = if (item.url == chapter.url) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent),
                    modifier = Modifier.clickable { chapters = false; viewModel.closeReader(); viewModel.openChapter(item) })
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
                Row(verticalAlignment = Alignment.CenterVertically) { Text(tankobunString(R.string.novel_justify), Modifier.weight(1f)); Switch(value.justified, { onChange(value.copy(justified = it)) }) }
                Row(verticalAlignment = Alignment.CenterVertically) { Text(tankobunString(R.string.novel_keep_awake), Modifier.weight(1f)); Switch(value.keepScreenOn, { onChange(value.copy(keepScreenOn = it)) }) }
            }
        })
}

@Composable
private fun NovelSlider(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Column { Text("$label · $value"); Slider(value.toFloat(), { onChange(it.roundToInt()) }, valueRange = range.first.toFloat()..range.last.toFloat()) }
}
