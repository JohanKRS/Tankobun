package com.tankobun.core.extensions

import com.tankobun.core.model.SourceManga
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SourceModelBridgeTest {
    private val manga = SourceManga(7, "/manga", "Fixture", null, null, null, null, null,
        """{"cursor":[1,"two"],"config":{"enabled":true},"nullable":null}""")

    @Test fun updatePreservesNestedMetadataAndPreparesOnlyNewChapters() = runTest {
        val previous = SChapter.create().apply { url = "/old"; name = "Old"; memo = manga.toSManga().memo }
            .toSourceChapter(7, "/manga")
        var calls = 0
        val prepared = mutableListOf<String>()
        val source = object : HttpSource() {
            override val name = "Fixture"
            override val lang = "en"
            override val baseUrl = "https://example.test"
            override suspend fun getMangaUpdate(manga: SManga, chapters: List<SChapter>, fetchDetails: Boolean, fetchChapters: Boolean): SMangaUpdate {
                calls++
                assertTrue(fetchDetails && fetchChapters)
                assertEquals(manga.memo, chapters.single().memo)
                manga.memo = Json.parseToJsonElement("""{"cursor":{"next":2}}""") as kotlinx.serialization.json.JsonObject
                return SMangaUpdate(manga, chapters + SChapter.create().apply { url = "/new"; name = "New"; memo = manga.memo })
            }
            override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
                prepared += chapter.url
                chapter.chapter_number = 2f
            }
        }
        val update = source.updateManga(manga, listOf(previous), true, true)
        assertEquals(1, calls)
        assertEquals(listOf("/new"), prepared)
        assertEquals("""{"cursor":{"next":2}}""", update.manga.memoJson)
        assertEquals(update.manga.toSManga().memo, update.chapters.last().toSChapter().memo)
        assertEquals(2f, update.chapters.last().chapterNumber)
    }

    @Test fun metadataRoundTripPreservesJsonValueTypes() {
        assertEquals(Json.parseToJsonElement(manga.memoJson!!), manga.toSManga().toSourceManga(7).toSManga().memo)
        assertTrue(manga.copy(memoJson = null).toSManga().memo.isEmpty())
        assertTrue(manga.copy(memoJson = "invalid").toSManga().memo.isEmpty())
    }

    @Test fun updatesOfTheSameMangaAreSerializedAndCancellationReleasesTheGate() = runTest {
        val source = object : HttpSource() {
            override val name = "Fixture"
            override val lang = "en"
            override val baseUrl = "https://example.test"
        }
        val gate = SourceUpdateGate()
        val entered = CompletableDeferred<Unit>()
        val first = launch { gate.run(source, "/manga") { entered.complete(Unit); awaitCancellation() } }
        entered.await()
        var secondEntered = false
        val second = async { gate.run(source, "/manga") { secondEntered = true } }
        yield()
        assertFalse(secondEntered)
        gate.run(source, "/other") { assertFalse(secondEntered) }
        first.cancelAndJoin()
        second.await()
        assertTrue(secondEntered)
        gate.run(source, "/manga") { assertTrue(secondEntered) }
    }
}
