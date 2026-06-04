package com.tankobun.app.logic

import com.tankobun.app.state.TankobunUiState
import kotlin.math.abs

internal fun TankobunUiState.withRecomputedTrackingDirty(): TankobunUiState =
    copy(
        trackingDirty = trackingSaveFailed || hasTrackingFormChanges(),
        trackingSaveFailed = false,
    )

private fun TankobunUiState.hasTrackingFormChanges(): Boolean {
    val entry = selectedListEntry ?: return false
    val progress = trackingProgress.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val score = trackingScore.toAniListScore(anilistScoreFormat)
    val notes = trackingNotes.trim().ifBlank { null }
    return trackingStatus != entry.status ||
        progress != entry.progress ||
        !score.sameAniListScore(entry.score) ||
        notes != entry.notes?.trim()?.ifBlank { null } ||
        trackingPrivate != entry.private ||
        trackingCustomLists.normalizedCustomLists() != entry.customLists.normalizedCustomLists()
}

private fun Double?.sameAniListScore(other: Double?): Boolean = when {
    this == null && other == null -> true
    this == null || other == null -> false
    else -> abs(this - other) < 0.0001
}

internal fun Iterable<String>.renamedCustomList(oldName: String, newName: String): List<String> =
    map { if (it.equals(oldName, ignoreCase = true)) newName else it }
        .normalizedCustomLists()

internal fun Iterable<String>.withoutCustomList(name: String): List<String> =
    filterNot { it.equals(name, ignoreCase = true) }
        .normalizedCustomLists()
