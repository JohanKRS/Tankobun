package com.tankobun.core.anilist

import com.tankobun.core.model.*
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test

class AnilistCatalogIdentityTest {
    private val identity = object : CatalogIdentity {
        override suspend fun resolve(media: AnilistMedia) = if (media.id == 123) media.copy(id = -99, anilistId = 123, mangaBakaId = 99) else media
        override suspend fun localId(anilistId: Int) = if (anilistId == 123) -99 else anilistId
        override suspend fun anilistId(localId: Int) = when(localId) { -99 -> 123; else -> localId.takeIf { it > 0 } }
    }
    private fun repo(server: MockWebServer) = AnilistRepository(AnilistGraphQlClient(OkHttpClient(), RespectfulRateLimiter(0), endpoint = server.url("/graphql").toString()), identity)

    @Test fun remoteMutationsUseExternalIdAndReturnStableLocalId() = runTest {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse.Builder().body("""{"data":{"SaveMediaListEntry":{"id":900,"mediaId":123,"status":"CURRENT","progress":8}}}""").build())
            val result = repo(server).saveListEntry("test-token", -99, MediaStatus.CURRENT, 8, null, null, null, null)
            assertEquals(-99, result.mediaId)
            assertEquals(900, result.id)
            val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
            assertEquals(123, body["variables"]!!.jsonObject["mediaId"]!!.jsonPrimitive.int)
        }
    }

    @Test fun unmappedMangaBakaIdsNeverReachAniList() = runTest {
        MockWebServer().use { server ->
            server.start()
            val repo = repo(server)
            assertNull(repo.mangaById(-81))
            assertNull(repo.mediaListEntry(-81, "test-token"))
            assertTrue(repo.mangaByIds(listOf(-81)).isEmpty())
            assertTrue(repo.mediaRecommendations(-81, 1, accessToken = null).recommendations.isEmpty())
            assertEquals(0, server.requestCount)
        }
    }
}
