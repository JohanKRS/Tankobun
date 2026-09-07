package com.tankobun.core.mangabaka

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class MangaBakaMetadataTest {
    private fun base() = MangaBakaMapper.media(Json.parseToJsonElement("""{"id":5,"state":"active","source":{"anilist":{"id":null}},"titles":[{"language":"en","title":"Test"}],"content_rating":"safe"}""").jsonObject)!!
    @Test fun structuredSourcesFillAuthorDatesAndCounts() {
        val full = Json.parseToJsonElement("""{"id":5,"source":{"kitsu":{"response":{"staff":{"nodes":[{"person":{"name":"Fictional Author"}}]},"startDate":"2000-02-03","endDate":"2010-05-06","chapterCount":80,"volumeCount":10}}}}""").jsonObject
        val result = base().withMangaBakaSourceMetadata(full)
        assertEquals(listOf("Fictional Author"), result.staff)
        assertEquals(2000, result.startDateYear)
        assertEquals(2010, result.endDateYear)
        assertEquals(80, result.chapters)
        assertEquals(10, result.volumes)
    }
    @Test fun unknownAndGuessedCountsDoNotBecomeTotals() {
        val full = Json.parseToJsonElement("""{"id":5,"source":{"kitsu":{"response":{"chapterCountGuess":5000}},"shikimori":{"response":{"chapters":0,"volumes":0}},"manga_updates":{"response":{"status":"10 volumes ongoing","latest_chapter":20}}}}""").jsonObject
        val result = base().withMangaBakaSourceMetadata(full)
        assertNull(result.chapters)
        assertNull(result.volumes)
    }
    @Test fun existingMetadataAlwaysWinsAndOtherSeriesAreIgnored() {
        val original = base().copy(staff = listOf("Primary"), chapters = 12, volumes = 2, startDateYear = 2020)
        val full = Json.parseToJsonElement("""{"id":5,"source":{"kitsu":{"response":{"chapterCount":80,"volumeCount":10,"startDate":"2000-01-01"}}}}""").jsonObject
        assertEquals(original, original.withMangaBakaSourceMetadata(full))
        assertEquals(base(), base().withMangaBakaSourceMetadata(JsonObject(full + ("id" to JsonPrimitive(6)))))
    }
}
