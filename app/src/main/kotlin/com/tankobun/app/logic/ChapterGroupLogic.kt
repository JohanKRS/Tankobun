package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.SourceChapter
import java.text.Normalizer
import java.util.Locale
import com.tankobun.core.model.ChapterGroupPreference

data class ChapterTranslationGroup(val key: String, val label: String)

private val groupWhitespace = Regex("[\\s\\p{Z}\\p{C}]+")
private fun cleanGroup(value: String): String = groupWhitespace.replace(value, " ").trim()
internal fun chapterGroupKey(value: String): String =
    Normalizer.normalize(cleanGroup(value), Normalizer.Form.NFKC).lowercase(Locale.ROOT)

internal fun SourceChapter.translationGroups(): List<ChapterTranslationGroup> =
    scanlators.ifEmpty { listOfNotNull(scanlator) }.mapNotNull { raw ->
        cleanGroup(raw).takeIf(String::isNotEmpty)?.let { ChapterTranslationGroup(chapterGroupKey(it), it) }
    }.distinctBy { it.key }.ifEmpty {
        listOfNotNull(scanlator?.let(::cleanGroup)?.takeIf(String::isNotEmpty)
            ?.let { ChapterTranslationGroup(chapterGroupKey(it), it) })
    }

internal fun SourceChapter.translationCredit(): String? =
    translationGroups().joinToString(" · ") { it.label }.takeIf(String::isNotEmpty)

internal fun chapterGroupPreferenceKey(packageName: String?, sourceId: Long?, mangaUrl: String?): String? =
    if (sourceId == null || mangaUrl == null) null else "${packageName.orEmpty()}|$sourceId|$mangaUrl"

private val decimalNumber = Regex("[0-9]+(?:\\.[0-9]+)?")
private val namedVolume = Regex("(?i)\\b(?:vol(?:ume|umen)?)[.\\s]*([0-9]+(?:\\.[0-9]+)?)\\b")
private val namedChapterNumber = Regex("(?i)\\b(?:chapter|chap|ch|capítulo|capitulo|cap)[.\\s]*([0-9]+(?:\\.[0-9]+)*(?:[a-z])?)")
private val specialChapter = Regex("(?i)(?:\\b(?:extra|special|especial|omake|bonus|prologue|epilogue|oneshot|part|parte|pt)\\b|side\\s+story|one[ -]shot|番外|前編|後編|卷)")

internal fun SourceChapter.chapterVolumeKey(): String =
    (volume?.trim()?.takeIf(String::isNotEmpty) ?: namedVolume.find(name)?.groupValues?.get(1))
        ?.let { it.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: it.lowercase(Locale.ROOT) }.orEmpty()

internal data class ChapterEditionKey(val sourceId: Long, val mangaUrl: String, val volume: String, val number: String)

/** Never infer a chapter number from its title or group name; ambiguous entries stay separate. */
internal fun SourceChapter.editionKey(): ChapterEditionKey? {
    if (!chapterNumber.isFinite() || chapterNumber <= 0f || specialChapter.containsMatchIn(name)) return null
    val rawNumber = chapterNumberText?.trim()?.takeIf(String::isNotEmpty) ?: chapterNumber.toString()
    if (!decimalNumber.matches(rawNumber)) return null
    val number = rawNumber.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
    // A title can contradict rounded/broken metadata (for example 12.1 reported as 12).
    // Keep that entry rather than manufacturing a replacement number from the title.
    val namedNumber = namedChapterNumber.find(name)?.groupValues?.get(1)
    if (namedNumber != null && namedNumber.toBigDecimalOrNull()?.compareTo(number) != 0) return null
    return ChapterEditionKey(sourceId, mangaUrl, chapterVolumeKey(), number.stripTrailingZeros().toPlainString())
}

class ChapterGroupSelection internal constructor(
    val chapters: List<SourceChapter>,
    val groups: List<ChapterTranslationGroup>,
    private val editions: Map<String, List<SourceChapter>>,
) {
    fun versionsOf(chapter: SourceChapter): List<SourceChapter> = editions[chapter.url] ?: listOf(chapter)
}

/** State copies for reader position do not need to regroup an unchanged chapter list. Retain only one list. */
internal object ChapterGroupSelectionCache {
    private var previousChapters: List<SourceChapter>? = null
    private var previousPreference: ChapterGroupPreference? = null
    private var previousSelection: ChapterGroupSelection? = null

    @Synchronized
    fun select(chapters: List<SourceChapter>, preference: ChapterGroupPreference): ChapterGroupSelection {
        if (previousChapters === chapters && previousPreference == preference) return requireNotNull(previousSelection)
        return selectChapterGroups(chapters, preference).also {
            previousChapters = chapters
            previousPreference = preference
            previousSelection = it
        }
    }
}

internal fun selectChapterGroups(chapters: List<SourceChapter>, preference: ChapterGroupPreference): ChapterGroupSelection {
    val credits = chapters.associate { it.url to it.translationGroups() }
    val groups = credits.values.flatten().distinctBy { it.key }.sortedBy { it.key }
    if (!preference.oneVersionPerChapter) return ChapterGroupSelection(chapters, groups, emptyMap())
    val buckets = chapters.groupBy { it.editionKey() }
    // Prefer the group with the widest coverage, then use a stable name/URL tie-breaker.
    val coverage = chapters.flatMap { chapter -> credits[chapter.url].orEmpty().map { it.key to (chapter.editionKey() ?: chapter.url) } }
        .groupBy({ it.first }, { it.second }).mapValues { it.value.distinct().size }
    val groupRank = groups.sortedWith(compareByDescending<ChapterTranslationGroup> { coverage[it.key] ?: 0 }.thenBy { it.key })
        .mapIndexed { index, group -> group.key to index }.toMap()
    val preferred = preference.preferredGroup?.let(::chapterGroupKey)
    val replacements = mutableMapOf<String, SourceChapter>()
    val editions = mutableMapOf<String, List<SourceChapter>>()
    buckets.forEach { (key, versions) ->
        if (key == null || versions.size < 2) return@forEach
        val releaseCredits = versions.map { credits[it.url].orEmpty().map { group -> group.key }.sorted() }
        // Repeated numbers from the same credited team may be parts/volume resets, not alternative translations.
        if (releaseCredits.distinct().size != releaseCredits.size) return@forEach
        val chosen = versions.minWith(compareBy<SourceChapter> {
            if (preferred != null && credits[it.url].orEmpty().any { group -> group.key == preferred }) 0 else 1
        }.thenBy { credits[it.url].orEmpty().minOfOrNull { group -> groupRank[group.key] ?: Int.MAX_VALUE } ?: Int.MAX_VALUE }
            .thenBy { it.url })
        versions.forEach { replacements[it.url] = chosen; editions[it.url] = versions }
    }
    val selected = chapters.map { replacements[it.url] ?: it }.distinctBy { it.sourceId to it.url }
    return ChapterGroupSelection(selected, groups, editions)
}

internal fun TankobunUiState.isChapterRead(chapter: SourceChapter): Boolean =
    chapterGroupSelection.versionsOf(chapter).any { chapterProgress[it.url]?.completed == true }
