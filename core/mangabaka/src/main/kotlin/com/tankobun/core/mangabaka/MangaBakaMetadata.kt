package com.tankobun.core.mangabaka

import com.tankobun.core.model.AnilistMedia
import kotlinx.serialization.json.*

/** Only structured, confirmed counts are accepted; guesses and prose are not totals. */
internal fun AnilistMedia.withMangaBakaSourceMetadata(full: JsonObject): AnilistMedia {
    if (full.number("id")?.toInt() != mangaBakaId) return this
    val sources = full.obj("source")
    val kitsu = sources.obj("kitsu").obj("response")
    val shikimori = sources.obj("shikimori").obj("response")
    val updates = sources.obj("manga_updates").obj("response")
    val rawAniList = sources.obj("anilist").obj("response")
    val aniList = sequenceOf(rawAniList, rawAniList.obj("Media"), rawAniList.obj("data").obj("Media"))
        .firstOrNull { anilistId != null && it.number("id")?.toInt() == anilistId } ?: JsonObject(emptyMap())
    fun JsonObject.count(key: String) = number(key)?.toInt()?.takeIf { it > 0 }
    fun JsonObject.year(key: String) = text(key)?.take(4)?.toIntOrNull()?.takeIf { it in 1..9999 }
    val creatorGroups = listOf(
        kitsu.obj("staff").array("nodes").mapNotNull { (it as? JsonObject)?.obj("person")?.text("name") },
        updates.array("authors").mapNotNull { (it as? JsonObject)?.text("name") },
        shikimori.array("personRoles").mapNotNull { (it as? JsonObject)?.obj("person")?.text("name") },
    )
    val kitsuBanner = kitsu.obj("bannerImage")
    val banner = (kitsuBanner.array("views").mapNotNull { it as? JsonObject } + kitsuBanner.obj("original"))
        .filter { (it.number("width") ?: 0.0) >= (it.number("height") ?: Double.MAX_VALUE) * 1.5 }
        .sortedBy { it.number("width") ?: Double.MAX_VALUE }
        .firstOrNull { (it.number("width") ?: 0.0) >= 1000 }?.text("url").imageUrl()
    val characters = aniList.obj("characters").array("edges").mapNotNull { edge ->
        (edge as? JsonObject)?.takeIf { it.text("role") == "MAIN" }?.obj("node")?.obj("image")?.text("large").imageUrl()
    }.distinct().take(12)
    return copy(
        staff = staff.ifEmpty { creatorGroups.firstOrNull { it.isNotEmpty() }.orEmpty().distinct() },
        startDateYear = startDateYear ?: aniList.obj("startDate").count("year") ?: kitsu.year("startDate") ?: shikimori.obj("airedOn").count("year") ?: updates.year("year"),
        endDateYear = endDateYear ?: aniList.obj("endDate").count("year") ?: kitsu.year("endDate") ?: shikimori.obj("releasedOn").count("year"),
        chapters = chapters ?: aniList.count("chapters") ?: kitsu.count("chapterCount") ?: shikimori.count("chapters"),
        volumes = volumes ?: aniList.count("volumes") ?: kitsu.count("volumeCount") ?: shikimori.count("volumes"),
        description = description ?: aniList.text("description") ?: kitsu.text("description") ?: updates.text("description"),
        bannerImage = bannerImage ?: aniList.text("bannerImage").imageUrl() ?: banner,
        mainCharacterImage = mainCharacterImage ?: characters.firstOrNull(),
        characterImages = characterImages.ifEmpty { characters },
    )
}
