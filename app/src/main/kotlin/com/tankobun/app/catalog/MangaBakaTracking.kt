package com.tankobun.app.catalog

import androidx.room.withTransaction
import com.tankobun.app.AppContainer
import com.tankobun.core.database.*
import com.tankobun.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

internal class MangaBakaTracking(private val container: AppContainer) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var flushJob: Job? = null
    private val dao get() = container.database.catalogDao()

    suspend fun changed(entry: AnilistListEntry, progressOnly: Boolean = false, scoreFormat: AnilistScoreFormat = container.settingsStore.anilistScoreFormat()) {
        val account = container.tokenStore.mangaBakaAccount() ?: return
        val payload = JSONObject().put("progress_chapter", entry.progress)
            .put("state", when(entry.status) { MediaStatus.CURRENT -> "reading"; MediaStatus.COMPLETED -> "completed"; MediaStatus.DROPPED -> "dropped"; MediaStatus.PAUSED -> "paused"; MediaStatus.REPEATING -> "rereading"; else -> "plan_to_read" })
        if (!progressOnly) {
            payload.put("rating", entry.score?.let { it * scoreMultiplier(scoreFormat) } ?: JSONObject.NULL)
                .put("note", entry.notes ?: JSONObject.NULL).put("is_private", entry.private)
        }
        // A newer progress-only change must retain fields from a pending full edit.
        container.database.withTransaction {
            val pending = if (progressOnly) dao.pending(account).firstOrNull { it.mediaId == entry.mediaId } else null
            val merged = if (progressOnly && pending != null && pending.payloadJson != "delete") JSONObject(pending.payloadJson) else JSONObject()
            payload.keys().forEach { merged.put(it, payload.get(it)) }
            dao.queue(MangaBakaMutationEntity(account, entry.mediaId, merged.toString(), UUID.randomUUID().toString(), 0, 0L))
        }
        schedule()
    }

    suspend fun deleted(mediaId: Int) {
        val account = container.tokenStore.mangaBakaAccount() ?: return
        dao.queue(MangaBakaMutationEntity(account, mediaId, "delete", UUID.randomUUID().toString(), 0, 0L))
        schedule()
    }

    @Synchronized fun schedule() {
        if (flushJob?.isActive == true) return
        flushJob = scope.launch {
            delay(800L)
            do {
                flush()
                val account = container.tokenStore.mangaBakaAccount() ?: break
                val remaining = dao.pending(account).any { it.retryAtEpochMillis <= System.currentTimeMillis() }
            } while (remaining)
        }
    }

    suspend fun flush() = mutex.withLock {
        val token = container.tokenStore.mangaBakaToken() ?: return@withLock
        val account = container.tokenStore.mangaBakaAccount() ?: return@withLock
        for (mutation in dao.pending(account).filter { it.retryAtEpochMillis <= System.currentTimeMillis() }.take(50)) {
            ensureSession(token, account)
            try {
                val local = dao.byLocalId(mutation.mediaId)
                val mb = local?.mangaBakaId ?: container.catalogIdentity.anilistId(mutation.mediaId)?.let { container.mangaBakaRepository.findByAniList(it)?.mangaBakaId }
                    ?: error("No confirmed MangaBaka match")
                ensureSession(token, account)
                if (mutation.payloadJson == "delete") container.mangaBakaRepository.deleteTracking(token, mb)
                else container.mangaBakaRepository.updateTracking(token, mb, mutation.payloadJson)
                dao.acknowledge(account, mutation.mediaId, mutation.revision)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                dao.retry(account, mutation.mediaId, mutation.revision, System.currentTimeMillis() + minOf(6 * 60 * 60_000L, 30_000L * (1L shl mutation.attempts.coerceAtMost(10))))
            }
        }
    }

    suspend fun sync(): Int {
        flush()
        return mutex.withLock {
            val token = container.tokenStore.mangaBakaToken() ?: return@withLock 0
            val account = container.tokenStore.mangaBakaAccount() ?: return@withLock 0
            var page = 1
            var imported = 0
            do {
                val remote = container.mangaBakaRepository.library(token, page)
                container.database.withTransaction {
                    ensureSession(token, account)
                    val pending = dao.pending(account).mapTo(hashSetOf()) { it.mediaId }
                    for ((media, remoteEntry) in remote.entries) {
                        val existing = container.database.listEntryDao().cachedEntry(media.id)
                        if (media.id in pending) continue
                        // AniList remains authoritative for entries already linked to its account.
                        if (existing != null && media.anilistId != null) continue
                        val entry = remoteEntry.copy(
                            score = remoteEntry.score?.div(scoreMultiplier(container.settingsStore.anilistScoreFormat())),
                            customLists = existing?.customLists.orEmpty(),
                        )
                        container.database.mediaDao().upsertMedia(media.toEntity(System.currentTimeMillis()))
                        container.database.listEntryDao().upsertEntry(entry.toEntity(System.currentTimeMillis()))
                        imported++
                    }
                }
                check(page < 1000 || !remote.hasNextPage) { "Library pagination limit" }
                page++
            } while (remote.hasNextPage)
            imported
        }
    }

    private fun ensureSession(token: String, account: String) {
        if (container.tokenStore.mangaBakaToken() != token || container.tokenStore.mangaBakaAccount() != account) throw CancellationException("MangaBaka account changed")
    }
}

internal fun scoreMultiplier(format: AnilistScoreFormat) = when(format) {
    AnilistScoreFormat.POINT_100 -> 1.0
    AnilistScoreFormat.POINT_10, AnilistScoreFormat.POINT_10_DECIMAL -> 10.0
    AnilistScoreFormat.POINT_5 -> 20.0
    AnilistScoreFormat.POINT_3 -> 100.0 / 3.0
}
