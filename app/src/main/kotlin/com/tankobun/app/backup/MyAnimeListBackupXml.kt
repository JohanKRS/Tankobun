package com.tankobun.app.backup

import com.tankobun.app.logic.normalizedCustomLists
import com.tankobun.app.state.LibraryItem
import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.MediaStatus
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt

internal data class BackupRestoreEntry(
    val mediaId: Int?,
    val idMal: Int?,
    val status: MediaStatus,
    val progress: Int?,
    val score: Double?,
    val notes: String?,
    val private: Boolean?,
    val customLists: List<String>,
)

internal fun buildMyAnimeListBackupXml(
    items: List<LibraryItem>,
    viewerName: String?,
    scoreFormat: AnilistScoreFormat,
): String {
    val sortedItems = items.sortedWith(
        compareBy<LibraryItem> { it.entry.status.malSortOrder() }
            .thenBy { it.media.title.userPreferred.lowercase(Locale.ROOT) },
    )
    val counts = sortedItems
        .groupingBy { it.entry.status.toMyAnimeListStatus() }
        .eachCount()
    return buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8" ?>""")
        appendLine()
        appendLine("\t<!--")
        appendLine("\t Created by Tankobun AniList backup")
        appendLine("\t MyAnimeList-compatible manga XML for AniList import")
        appendLine("\t AniList-only metadata is kept in XML comments where MAL has no matching field")
        appendLine("\t-->")
        appendLine()
        appendLine("\t<myanimelist>")
        appendLine()
        appendLine("\t\t<myinfo>")
        appendLine("\t\t\t<user_id>0</user_id>")
        appendLine("\t\t\t<user_name>${viewerName.orEmpty().xmlEscaped()}</user_name>")
        appendLine("\t\t\t<user_export_type>2</user_export_type>")
        appendLine("\t\t\t<user_total_manga>${sortedItems.size}</user_total_manga>")
        appendLine("\t\t\t<user_total_reading>${counts.getOrDefault("Reading", 0)}</user_total_reading>")
        appendLine("\t\t\t<user_total_completed>${counts.getOrDefault("Completed", 0)}</user_total_completed>")
        appendLine("\t\t\t<user_total_onhold>${counts.getOrDefault("On-Hold", 0)}</user_total_onhold>")
        appendLine("\t\t\t<user_total_dropped>${counts.getOrDefault("Dropped", 0)}</user_total_dropped>")
        appendLine("\t\t\t<user_total_plantoread>${counts.getOrDefault("Plan to Read", 0)}</user_total_plantoread>")
        appendLine("\t\t</myinfo>")
        appendLine()
        sortedItems.forEach { item ->
            appendLine()
            appendLine("\t\t<!-- ${item.toAniListBackupComment()} -->")
            appendLine("\t\t<manga>")
            appendLine("\t\t\t<manga_mangadb_id>${item.media.idMal ?: 0}</manga_mangadb_id>")
            appendLine("\t\t\t<manga_title>${item.media.title.userPreferred.cdata()}</manga_title>")
            appendLine("\t\t\t<manga_volumes>${item.media.volumes ?: 0}</manga_volumes>")
            appendLine("\t\t\t<manga_chapters>${item.media.chapters ?: 0}</manga_chapters>")
            appendLine("\t\t\t<my_id>${item.entry.id}</my_id>")
            appendLine("\t\t\t<my_read_volumes>${item.readVolumesForBackup()}</my_read_volumes>")
            appendLine("\t\t\t<my_read_chapters>${item.entry.progress.coerceAtLeast(0)}</my_read_chapters>")
            appendLine("\t\t\t<my_start_date>0000-00-00</my_start_date>")
            appendLine("\t\t\t<my_finish_date>0000-00-00</my_finish_date>")
            appendLine("\t\t\t<my_scanalation_group><![CDATA[]]></my_scanalation_group>")
            appendLine("\t\t\t<my_score>${item.entry.score.toMyAnimeListScore(scoreFormat)}</my_score>")
            appendLine("\t\t\t<my_storage></my_storage>")
            appendLine("\t\t\t<my_retail_volumes>0</my_retail_volumes>")
            appendLine("\t\t\t<my_status>${item.entry.status.toMyAnimeListStatus()}</my_status>")
            appendLine("\t\t\t<my_comments>${item.entry.notes.orEmpty().cdata()}</my_comments>")
            appendLine("\t\t\t<my_times_read>${if (item.entry.status == MediaStatus.REPEATING) 1 else 0}</my_times_read>")
            appendLine("\t\t\t<my_tags>${item.entry.customLists.joinToString(", ").cdata()}</my_tags>")
            appendLine("\t\t\t<my_priority>Low</my_priority>")
            appendLine("\t\t\t<my_reread_value></my_reread_value>")
            appendLine("\t\t\t<my_rereading>${if (item.entry.status == MediaStatus.REPEATING) "YES" else "NO"}</my_rereading>")
            appendLine("\t\t\t<my_discuss>YES</my_discuss>")
            appendLine("\t\t\t<my_sns>default</my_sns>")
            appendLine("\t\t\t<update_on_import>1</update_on_import>")
            appendLine("\t\t</manga>")
        }
        appendLine()
        appendLine("\t</myanimelist>")
    }
}

internal fun parseMyAnimeListBackupXml(
    input: InputStream,
    scoreFormat: AnilistScoreFormat,
): List<BackupRestoreEntry> {
    val document = DocumentBuilderFactory.newInstance()
        .apply {
            isIgnoringComments = false
            isXIncludeAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
        }
        .newDocumentBuilder()
        .parse(input)
    val root = document.documentElement
    val entries = mutableListOf<BackupRestoreEntry>()
    var pendingAniListMediaId: Int? = null
    var pendingPrivate: Boolean? = null
    val children = root.childNodes
    for (index in 0 until children.length) {
        val node = children.item(index)
        when (node.nodeType) {
            Node.COMMENT_NODE -> {
                val comment = node.nodeValue.orEmpty()
                pendingAniListMediaId = Regex("""AniList media id:\s*(\d+)""")
                    .find(comment)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toIntOrNull()
                pendingPrivate = comment.contains("private: true", ignoreCase = true).takeIf { it }
            }
            Node.ELEMENT_NODE -> {
                val element = node as? Element ?: continue
                if (element.tagName != "manga") continue
                val idMal = element.childText("manga_mangadb_id")?.toIntOrNull()?.takeIf { it > 0 }
                val customLists = element.childText("my_tags")
                    .orEmpty()
                    .split(',')
                    .normalizedCustomLists()
                entries += BackupRestoreEntry(
                    mediaId = pendingAniListMediaId,
                    idMal = idMal,
                    status = element.childText("my_status").toMediaStatusFromMyAnimeList(),
                    progress = element.childText("my_read_chapters")?.toIntOrNull()?.coerceAtLeast(0),
                    score = element.childText("my_score")
                        ?.toIntOrNull()
                        ?.toAniListScoreFromMyAnimeList(scoreFormat),
                    notes = element.childText("my_comments")?.trim()?.ifBlank { null },
                    private = pendingPrivate,
                    customLists = customLists,
                )
                pendingAniListMediaId = null
                pendingPrivate = null
            }
        }
    }
    return entries
}

internal fun Element.childText(tagName: String): String? {
    val nodes = getElementsByTagName(tagName)
    if (nodes.length == 0) return null
    return nodes.item(0)?.textContent?.trim()
}

internal fun LibraryItem.toAniListBackupComment(): String =
    buildList {
        add("AniList media id: ${media.id}")
        add("AniList list entry id: ${entry.id}")
        media.siteUrl?.takeIf { it.isNotBlank() }?.let { add("AniList URL: $it") }
        if (media.idMal == null) add("No MAL id; this entry may need manual matching on import")
        if (entry.private) add("private: true")
        if (entry.customLists.isNotEmpty()) add("custom lists: ${entry.customLists.joinToString(", ")}")
        media.format?.takeIf { it.isNotBlank() }?.let { add("format: $it") }
    }.joinToString("; ").xmlCommentEscaped()

internal fun LibraryItem.readVolumesForBackup(): Int {
    val volumes = media.volumes ?: 0
    return if (entry.status == MediaStatus.COMPLETED) volumes.coerceAtLeast(0) else 0
}

internal fun MediaStatus.toMyAnimeListStatus(): String =
    when (this) {
        MediaStatus.CURRENT,
        MediaStatus.REPEATING -> "Reading"
        MediaStatus.PLANNING,
        MediaStatus.UNKNOWN -> "Plan to Read"
        MediaStatus.COMPLETED -> "Completed"
        MediaStatus.DROPPED -> "Dropped"
        MediaStatus.PAUSED -> "On-Hold"
    }

internal fun String?.toMediaStatusFromMyAnimeList(): MediaStatus =
    when (this?.trim()?.lowercase(Locale.ROOT)) {
        "reading" -> MediaStatus.CURRENT
        "completed" -> MediaStatus.COMPLETED
        "on-hold", "on hold" -> MediaStatus.PAUSED
        "dropped" -> MediaStatus.DROPPED
        "plan to read", "plantoread" -> MediaStatus.PLANNING
        else -> MediaStatus.PLANNING
    }

internal fun MediaStatus.malSortOrder(): Int =
    when (this) {
        MediaStatus.CURRENT -> 0
        MediaStatus.REPEATING -> 1
        MediaStatus.COMPLETED -> 2
        MediaStatus.PAUSED -> 3
        MediaStatus.DROPPED -> 4
        MediaStatus.PLANNING -> 5
        MediaStatus.UNKNOWN -> 6
    }

internal fun Int.toAniListScoreFromMyAnimeList(format: AnilistScoreFormat): Double? {
    val value = takeIf { it > 0 }?.coerceIn(0, 10) ?: return null
    return when (format) {
        AnilistScoreFormat.POINT_100 -> (value * 10).toDouble()
        AnilistScoreFormat.POINT_10_DECIMAL,
        AnilistScoreFormat.POINT_10 -> value.toDouble()
        AnilistScoreFormat.POINT_5 -> (value / 2.0).roundToInt().coerceIn(0, 5).toDouble()
        AnilistScoreFormat.POINT_3 -> (value * 3.0 / 10.0).roundToInt().coerceIn(0, 3).toDouble()
    }
}

internal fun Double?.toMyAnimeListScore(format: AnilistScoreFormat): Int {
    val value = this ?: return 0
    val score = when (format) {
        AnilistScoreFormat.POINT_100 -> value / 10.0
        AnilistScoreFormat.POINT_10_DECIMAL,
        AnilistScoreFormat.POINT_10 -> value
        AnilistScoreFormat.POINT_5 -> value * 2.0
        AnilistScoreFormat.POINT_3 -> value * (10.0 / 3.0)
    }
    return score.roundToInt().coerceIn(0, 10)
}

internal fun String.cdata(): String =
    "<![CDATA[${replace("]]>", "]]]]><![CDATA[>")}]]>"

internal fun String.xmlEscaped(): String =
    replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

internal fun String.xmlCommentEscaped(): String =
    replace("--", "- -")
        .replace("<", "(")
        .replace(">", ")")

