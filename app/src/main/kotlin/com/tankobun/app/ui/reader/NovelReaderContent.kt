package com.tankobun.app.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import coil3.compose.AsyncImage
import com.tankobun.app.R
import com.tankobun.app.ReaderPageImageModel
import com.tankobun.app.state.ReaderChapterSegment
import com.tankobun.app.state.TankobunUiState
import com.tankobun.app.tankobunString
import com.tankobun.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

internal data class NovelAnchor(val chapterUrl: String, val block: Int, val character: Int = 0)
internal data class NovelJump(val anchor: NovelAnchor, val request: Int)
internal data class NovelBlockItem(val chapter: SourceChapter, val index: Int, val page: ReaderPage) {
    val key get() = "${chapter.sourceId}:${chapter.url}:$index"
}
internal data class NovelLeaf(val chapter: SourceChapter, val blocks: List<ReaderPage>, val page: NovelTextPage) {
    val anchor get() = NovelAnchor(chapter.url, page.blockIndex, page.character)
    val key get() = "${chapter.sourceId}:${chapter.url}:${page.blockIndex}:${page.character}"
}

@Composable
internal fun NovelIllustration(state: TankobunUiState, item: NovelBlockItem, modifier: Modifier = Modifier) {
    val source = state.allInstalledSources.firstOrNull { it.id == item.chapter.sourceId }
    val model: Any? = item.page.cachedFilePath?.let(::File) ?: source?.let {
        state.selectedMedia?.let { media -> ReaderPageImageModel(media.id, item.chapter, it, item.page) }
    } ?: item.page.imageUrl
    AsyncImage(model, tankobunString(R.string.novel_illustration), modifier, contentScale = ContentScale.Fit)
}

@Composable
internal fun NovelScrollContent(
    state: TankobunUiState, segments: List<ReaderChapterSegment>, style: TextStyle,
    jump: NovelJump?, onPosition: (NovelAnchor) -> Unit,
    boundary: @Composable (SourceChapter, Boolean) -> Unit,
) {
    val p = state.novelReaderPreferences
    val items = remember(segments) { segments.flatMap { segment -> segment.pages.mapIndexed { index, page -> NovelBlockItem(segment.chapter, index, page) } } }
    if (items.isEmpty()) return
    val initial = remember { NovelAnchor(state.activeChapter!!.url, state.currentPageIndex, state.currentPageScrollOffset) }
    val list = rememberLazyListState(initialFirstVisibleItemIndex = 1 + items.indexOfFirst { it.chapter.url == initial.chapterUrl && it.index == initial.block }.coerceAtLeast(0))
    val latestItems by rememberUpdatedState(items)
    val latestPosition by rememberUpdatedState(onPosition)
    var anchor by remember { mutableStateOf(initial) }
    var restoring by remember { mutableStateOf(true) }
    var appliedJump by remember { mutableStateOf(jump?.request) }
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        val layouts = remember(style, p.maxTextWidth, p.margin, width, density.fontScale) { mutableStateMapOf<String, TextLayoutResult>() }
        LaunchedEffect(items) { layouts.keys.retainAll(items.map { it.key }.toSet()) }
        // Stable keys retain the viewport when an earlier chapter is inserted or an old one evicted.
        LaunchedEffect(style, p.paragraphSpacing, p.maxTextWidth, p.margin, width, density.fontScale, jump) {
            restoring = true
            val target = if (jump != null && jump.request != appliedJump) jump.anchor else anchor
            val index = latestItems.indexOfFirst { it.chapter.url == target.chapterUrl && it.index == target.block }.coerceAtLeast(0)
            val item = latestItems[index]
            list.scrollToItem(index + 1)
            if (item.page.novelBlock?.text?.isNotEmpty() == true) {
                val layout = snapshotFlow { layouts[item.key] }.filterNotNull().first()
                val line = layout.getLineForOffset(target.character.coerceIn(0, layout.layoutInput.text.length))
                withFrameNanos { }
                val currentIndex = latestItems.indexOfFirst { it.key == item.key }
                if (currentIndex >= 0) list.scrollToItem(currentIndex + 1, layout.getLineTop(line).roundToInt().coerceAtLeast(0))
            }
            appliedJump = jump?.request
            withFrameNanos { }
            restoring = false
        }
        LaunchedEffect(list, layouts) {
            snapshotFlow {
                if (restoring) null else {
                    val info = list.layoutInfo
                    val first = info.visibleItemsInfo.firstOrNull { it.offset + it.size > info.viewportStartOffset && latestItems.any { item -> item.key == it.key } }
                    val item = first?.key?.let { key -> latestItems.firstOrNull { it.key == key } }
                    item?.let {
                        val layout = layouts[it.key]
                        val offset = (info.viewportStartOffset - first.offset).coerceAtLeast(0)
                        val character = layout?.let { text -> text.getLineStart(text.getLineForVerticalPosition(offset.toFloat())) } ?: 0
                        val end = info.visibleItemsInfo.lastOrNull { visible ->
                            visible.offset + visible.size <= info.viewportEndOffset &&
                                latestItems.firstOrNull { block -> block.key == visible.key }?.let { block -> block.chapter.url == it.chapter.url && block.page.novelBlock?.endOfChapter == true } == true
                        }
                        val endItem = end?.let { visible -> latestItems.firstOrNull { it.key == visible.key } }
                        NovelAnchor(it.chapter.url, endItem?.index ?: it.index, if (endItem == null) character else 0)
                    }
                }
            }.filterNotNull().distinctUntilChanged().collect { anchor = it; latestPosition(it) }
        }
        // Long text is composed lazily, but the complete chapter document is already cached.
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = p.margin.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(p.paragraphSpacing.dp)) {
            item(key = "previous-boundary") { boundary(items.first().chapter, true) }
            itemsIndexed(items, key = { _, item -> item.key }) { _, item ->
                val block = item.page.novelBlock
                if (block?.endOfChapter == true) boundary(item.chapter, false)
                else if (block != null) {
                    val text = remember(block.html) { AnnotatedString.fromHtml(block.html) }
                    Column(Modifier.widthIn(max = p.maxTextWidth.dp).fillMaxWidth()) {
                        SelectionContainer {
                            Text(text, Modifier.fillMaxWidth(), style = novelBlockStyle(style, block), onTextLayout = { layouts[item.key] = it })
                        }
                    }
                } else NovelIllustration(state, item, Modifier.widthIn(max = p.maxTextWidth.dp).fillMaxWidth().heightIn(min = 160.dp))
            }
        }
    }
}

internal fun novelBlockStyle(style: TextStyle, block: NovelBlock): TextStyle = if (block.headingLevel > 0) {
    style.copy(fontSize = (style.fontSize.value + 5).sp, lineHeight = (style.lineHeight.value + 5).sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
} else style

@Composable
internal fun NovelPagedContent(
    state: TankobunUiState, segments: List<ReaderChapterSegment>, style: TextStyle,
    jump: NovelJump?, onPosition: (NovelAnchor) -> Unit,
    onPageAction: ((Int) -> Unit) -> Unit, onPageSeek: ((Int) -> Unit) -> Unit, onPageCount: (Int, Int, Int) -> Unit,
    boundary: @Composable (SourceChapter, Boolean) -> Unit,
) {
    val p = state.novelReaderPreferences
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 128)
    var spreads by remember { mutableStateOf(emptyList<List<NovelLeaf>>()) }
    var texts by remember { mutableStateOf(emptyMap<Pair<String, Int>, AnnotatedString>()) }
    val initial = remember { NovelAnchor(state.activeChapter!!.url, state.currentPageIndex, state.currentPageScrollOffset) }
    var anchor by remember { mutableStateOf(initial) }
    var restoring by remember { mutableStateOf(true) }
    val pager = rememberPagerState { spreads.size }
    val scope = rememberCoroutineScope()
    val latestState by rememberUpdatedState(state)
    val latestPosition by rememberUpdatedState(onPosition)
    var appliedJump by remember { mutableStateOf(jump?.request) }
    var restoredAnchor by remember { mutableStateOf<NovelAnchor?>(null) }
    var restoredSpreadKey by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = p.margin.dp, vertical = 24.dp), contentAlignment = Alignment.TopCenter) {
        val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val columns = novelColumnCount(p.landscapeTwoPages && landscape, maxWidth.value, maxHeight.value)
        val gutter = if (columns == 2) 32.dp else 0.dp
        val columnWidth = minOf((maxWidth - gutter) / columns, p.maxTextWidth.dp)
        val width = with(density) { columnWidth.roundToPx() }.coerceAtLeast(1)
        val height = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val spacing = with(density) { p.paragraphSpacing.dp.toPx() }
        LaunchedEffect(segments, style, width, height, spacing, density.fontScale, columns, jump) {
            restoring = true
            val target = if (jump != null && jump.request != appliedJump) jump.anchor else anchor
            val result = withContext(Dispatchers.Default) {
                val textMap = mutableMapOf<Pair<String, Int>, AnnotatedString>()
                val pages = segments.flatMap { segment ->
                    val measurements = segment.pages.mapIndexed { index, page ->
                        ensureActive()
                        val block = page.novelBlock
                        val lines = if (block == null || block.endOfChapter) emptyList() else {
                            val text = AnnotatedString.fromHtml(block.html)
                            textMap[segment.chapter.url to index] = text
                            val layout = measurer.measure(text, novelBlockStyle(style, block), constraints = Constraints(maxWidth = width))
                            (0 until layout.lineCount).map { line -> NovelTextLine(layout.getLineStart(line), layout.getLineEnd(line), layout.getLineBottom(line) - layout.getLineTop(line)) }
                        }
                        NovelMeasuredParagraph(index, lines)
                    }
                    paginateNovel(measurements, height, spacing).map { NovelLeaf(segment.chapter, segment.pages, it) }
                }
                pages to textMap
            }
            texts = result.second
            spreads = result.first.groupBy { it.chapter.url }.values.flatMap { it.chunked(columns) }
            restoredAnchor = target
            if (spreads.isNotEmpty()) {
                val chapterStart = spreads.indexOfFirst { it.first().chapter.url == target.chapterUrl }
                val targetIndex = if (chapterStart >= 0) chapterStart + spreads.drop(chapterStart)
                    .takeWhile { it.first().chapter.url == target.chapterUrl }.map { it.first().page }.pageForAnchor(target.block, target.character)
                else spreads.indexOfFirst { it.first().chapter.url == latestState.activeChapter?.url }.coerceAtLeast(0)
                restoredSpreadKey = spreads[targetIndex].first().key
                pager.scrollToPage(targetIndex)
            }
            appliedJump = jump?.request
            withFrameNanos { }
            restoring = false
        }
        LaunchedEffect(pager) {
            snapshotFlow { if (restoring || pager.isScrollInProgress) null else spreads.getOrNull(pager.currentPage) }
                .filterNotNull().collect { spread ->
                    val leaf = spread.first()
                    anchor = restoredAnchor?.takeIf { restoredSpreadKey == leaf.key } ?: leaf.anchor
                    restoredAnchor = null
                    restoredSpreadKey = null
                    latestPosition(anchor)
                    val chapterPages = spreads.flatten().filter { it.chapter.url == leaf.chapter.url }
                    onPageCount(chapterPages.indexOf(leaf) + 1, chapterPages.indexOf(spread.last()) + 1, chapterPages.size)
                }
        }
        val turn: (Int) -> Unit = { delta ->
            if (!restoring && !pager.isScrollInProgress) scope.launch {
                val target = pager.currentPage + delta
                if (target in spreads.indices) pager.animateScrollToPage(target)
            }
        }
        SideEffect {
            onPageAction(turn)
            onPageSeek { page ->
                if (!restoring) {
                    val chapter = spreads.getOrNull(pager.currentPage)?.first()?.chapter
                    val leaf = spreads.flatten().filter { it.chapter == chapter }.getOrNull(page)
                    val index = spreads.indexOfFirst { leaf != null && leaf in it }
                    if (index >= 0) scope.launch { pager.scrollToPage(index) }
                }
            }
        }
        if (spreads.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.Center))
        else HorizontalPager(state = pager, key = { spreads[it].first().key }, modifier = Modifier.fillMaxSize(), userScrollEnabled = !restoring) { index ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Row(Modifier.width(columnWidth * columns + gutter).fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(gutter)) {
                    repeat(columns) { column ->
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            spreads[index].getOrNull(column)?.let { leaf ->
                                leaf.page.parts.forEach { part ->
                                    val item = NovelBlockItem(leaf.chapter, part.blockIndex, leaf.blocks[part.blockIndex])
                                    val block = item.page.novelBlock
                                    if (part.gapBefore > 0) Spacer(Modifier.height(with(density) { part.gapBefore.toDp() }))
                                    when {
                                        block?.endOfChapter == true -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { boundary(leaf.chapter, false) }
                                        block != null -> {
                                            val text = texts[leaf.chapter.url to part.blockIndex] ?: AnnotatedString("")
                                            val chunk = text.subSequence(part.start.coerceAtMost(text.length), part.end.coerceAtMost(text.length))
                                            SelectionContainer { Text(chunk, Modifier.fillMaxWidth(), style = novelBlockStyle(style, block), maxLines = part.lineCount.coerceAtLeast(1)) }
                                        }
                                        else -> NovelIllustration(state, item, Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
