package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class AnilistLibrarySnapshotTest {
    private fun entry(id: Int) = """{"id":${id + 100},"mediaId":$id,"status":"CURRENT","progress":3,"updatedAt":1000}"""
    private fun collection(lists: String, next: Boolean = false) =
        Json.parseToJsonElement("""{"MediaListCollection":{"hasNextChunk":$next,"lists":$lists}}""").jsonObject

    @Test fun snapshotUsesOneAuthenticatedRequestWithoutMediaMetadata() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"data":${collection("[{\"entries\":[${entry(1)}]}]")}}""").build())
            val repository = AnilistRepository(AnilistGraphQlClient(OkHttpClient(), RespectfulRateLimiter(0), server.url("/").toString()))
            val snapshot = repository.mangaLibrarySnapshot("test-session", 7, AnilistScoreFormat.POINT_10)
            assertEquals(listOf(1), snapshot.map { it.mediaId })
            val request = server.takeRequest()
            assertEquals("Bearer test-session", request.headers["Authorization"])
            val body = requireNotNull(request.body).utf8()
            assertTrue(body.contains("MangaLibrarySnapshot"))
            assertFalse(body.contains("coverImage"))
            assertFalse(body.contains("description"))
            assertEquals(1, server.requestCount)
        }
    }

    @Test fun includesCustomListsAndDeduplicatesTheirEntries() {
        val data = collection("""[{"entries":[${entry(1)}]},{"entries":[${entry(1)},${entry(2)}]}]""")
        assertEquals(listOf(1, 2), AnilistJsonMapper.librarySnapshot(data).map { it.mediaId })
    }

    @Test fun completeEmptyLibraryIsValid() {
        assertTrue(AnilistJsonMapper.librarySnapshot(collection("[]")).isEmpty())
        val unchunked = Json.parseToJsonElement("""{"MediaListCollection":{"hasNextChunk":null,"lists":[]}}""").jsonObject
        assertTrue(AnilistJsonMapper.librarySnapshot(unchunked).isEmpty())
    }

    @Test fun incompleteResponsesCannotBecomeRemoteDeletions() {
        val invalid = listOf(
            "{}", """{"MediaListCollection":null}""",
            """{"MediaListCollection":{"hasNextChunk":false}}""",
            collection("[]", next = true).toString(),
            collection("[{}]").toString(),
            collection("""[{"entries":[{"id":1,"mediaId":2}]}]""").toString(),
        )
        invalid.forEach { raw ->
            assertThrows(IllegalArgumentException::class.java) {
                AnilistJsonMapper.librarySnapshot(Json.parseToJsonElement(raw).jsonObject)
            }
        }
    }

    @Test fun cappedCollectionsAreRejectedInsteadOfPruningTheCache() {
        val entries = (1..11_000).joinToString(",", transform = ::entry)
        assertThrows(IllegalArgumentException::class.java) {
            AnilistJsonMapper.librarySnapshot(collection("""[{"entries":[$entries]}]"""))
        }
    }
}
