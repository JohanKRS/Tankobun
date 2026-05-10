package eu.kanade.tachiyomi.source.model

import android.net.Uri

data class MangasPage(
    val mangas: List<SManga>,
    val hasNextPage: Boolean,
)

class FilterList : ArrayList<Any> {
    constructor() : super()
    constructor(filters: List<Any>) : super(filters)
    constructor(vararg filters: Filter<*>) : super(filters.toList())
    constructor(vararg filters: Any) : super(filters.toList())
}

open class Filter<T>(
    val name: String,
    var state: T,
) {
    open class Header(name: String) : Filter<Unit>(name, Unit)
    open class Separator(name: String = "") : Filter<Unit>(name, Unit)
    open class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)
    open class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        fun isIgnored(): Boolean = state == STATE_IGNORE
        fun isIncluded(): Boolean = state == STATE_INCLUDE
        fun isExcluded(): Boolean = state == STATE_EXCLUDE

        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }
    open class Text(name: String, state: String = "") : Filter<String>(name, state)
    open class Select<V>(name: String, val values: Array<V>, state: Int = 0) : Filter<Int>(name, state)
    open class Group<V>(name: String, state: List<V>) : Filter<List<V>>(name, state)
    open class Sort(name: String, val values: Array<String>, state: Selection? = null) :
        Filter<Sort.Selection?>(name, state) {
        data class Selection(val index: Int, val ascending: Boolean)
    }
}

interface SManga {
    var url: String
    var title: String
    var altTitles: List<String>
    var artist: String?
    var author: String?
    var description: String?
    var genre: String?
    var genres: List<String>
    var status: Int
    var contentRating: ContentRating
    var score: Int?
    var thumbnail_url: String?
    var banner: String?
    var readingMode: ReadingMode?
    var update_strategy: UpdateStrategy
    var memo: Map<String, String>
    var initialized: Boolean

    fun copyFrom(other: SManga)
    fun setUrlWithoutDomain(url: String)

    enum class ContentRating {
        SAFE,
        SUGGESTIVE,
        ADULT,
    }

    enum class ReadingMode {
        RIGHT_TO_LEFT,
        LEFT_TO_RIGHT,
        LONG_STRIP,
    }

    companion object {
        fun create(): SManga = SMangaImpl()

        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6
    }
}

private data class SMangaImpl(
    override var url: String = "",
    override var title: String = "",
    override var altTitles: List<String> = emptyList(),
    override var artist: String? = null,
    override var author: String? = null,
    override var description: String? = null,
    override var genre: String? = null,
    override var genres: List<String> = emptyList(),
    override var status: Int = SManga.UNKNOWN,
    override var contentRating: SManga.ContentRating = SManga.ContentRating.SAFE,
    override var score: Int? = null,
    override var thumbnail_url: String? = null,
    override var banner: String? = null,
    override var readingMode: SManga.ReadingMode? = null,
    override var update_strategy: UpdateStrategy = UpdateStrategy.ALWAYS_UPDATE,
    override var memo: Map<String, String> = emptyMap(),
    override var initialized: Boolean = false,
) : SManga {
    override fun copyFrom(other: SManga) {
        altTitles = other.altTitles
        author = other.author
        artist = other.artist
        description = other.description
        genre = other.genre
        genres = other.genres
        status = other.status
        contentRating = other.contentRating
        score = other.score
        thumbnail_url = other.thumbnail_url
        banner = other.banner
        readingMode = other.readingMode
        update_strategy = other.update_strategy
        memo = other.memo
        initialized = other.initialized
    }

    override fun setUrlWithoutDomain(url: String) {
        this.url = url.substringAfter("://").substringAfter('/')
        if (!this.url.startsWith("/")) {
            this.url = "/${this.url}"
        }
    }
}

interface SChapter {
    var url: String
    var name: String
    var volume: String?
    var date_upload: Long
    var chapter_number: Float
    var number: String?
    var scanlator: String?
    var scanlators: List<String>
    var note: String?
    var memo: Map<String, String>

    fun setUrlWithoutDomain(url: String)

    companion object {
        fun create(): SChapter = SChapterImpl()
    }
}

private data class SChapterImpl(
    override var url: String = "",
    override var name: String = "",
    override var volume: String? = null,
    override var date_upload: Long = 0L,
    override var chapter_number: Float = -1f,
    override var number: String? = null,
    override var scanlator: String? = null,
    override var scanlators: List<String> = emptyList(),
    override var note: String? = null,
    override var memo: Map<String, String> = emptyMap(),
) : SChapter {
    override fun setUrlWithoutDomain(url: String) {
        this.url = url.substringAfter("://").substringAfter('/')
        if (!this.url.startsWith("/")) {
            this.url = "/${this.url}"
        }
    }
}

data class Page @JvmOverloads constructor(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null,
    var uri: Uri? = null,
) {
}

data class SMangaUpdate(
    val manga: SManga,
    val chapters: List<SChapter>,
)

enum class UpdateStrategy {
    ALWAYS_UPDATE,
    ONLY_FETCH_ONCE,
}
