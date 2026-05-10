package com.tankobun.core.sync

import com.tankobun.core.model.MediaStatus
import com.tankobun.core.model.SyncMutation
import com.tankobun.core.model.SyncMutationType
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class SyncMutationFactory {
    fun saveMediaListEntry(
        mediaId: Int,
        status: MediaStatus? = null,
        progress: Int? = null,
        score: Double? = null,
        notes: String? = null,
        private: Boolean? = null,
        customLists: List<String>? = null,
        nowMillis: Long,
    ): SyncMutation {
        val payload = buildJsonObject {
            if (status != null) put("status", status.name) else put("status", JsonNull)
            if (progress != null) put("progress", progress) else put("progress", JsonNull)
            if (score != null) put("score", score) else put("score", JsonNull)
            if (notes != null) put("notes", notes) else put("notes", JsonNull)
            if (private != null) put("private", private) else put("private", JsonNull)
            if (customLists != null) {
                put("customLists", buildJsonArray { customLists.forEach { add(it) } })
            } else {
                put("customLists", JsonNull)
            }
        }
        return SyncMutation(
            id = UUID.randomUUID().toString(),
            type = SyncMutationType.SAVE_MEDIA_LIST_ENTRY,
            mediaId = mediaId,
            payloadJson = payload.toString(),
            attempts = 0,
            nextAttemptAtEpochMillis = nowMillis,
            createdAtEpochMillis = nowMillis,
        )
    }
}
