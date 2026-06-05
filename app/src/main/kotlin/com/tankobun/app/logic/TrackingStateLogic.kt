package com.tankobun.app.logic

import com.tankobun.app.state.LibraryItem
import com.tankobun.app.state.TankobunUiState
import com.tankobun.core.model.AnilistListEntry
import com.tankobun.core.model.AnilistMedia
import java.util.Locale
import kotlin.math.abs

internal fun TankobunUiState.withRecomputedTrackingDirty(): TankobunUiState =
    copy(
        trackingDirty = trackingSaveFailed || hasTrackingFormChanges(),
        trackingSaveFailed = false,
    )

internal fun TankobunUiState.withTrackingCustomListSelected(
    name: String,
    selected: Boolean,
): TankobunUiState =
    copy(
        trackingCustomLists = if (selected) {
            trackingCustomLists + name
        } else {
            trackingCustomLists - name
        },
    ).withRecomputedTrackingDirty()

internal fun TankobunUiState.withAddedTrackingCustomList(name: String): TankobunUiState =
    copy(
        trackingCustomLists = trackingCustomLists + name,
    ).withRecomputedTrackingDirty()

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

internal fun TankobunUiState.withRenamedAniListCustomList(
    customLists: List<String>,
    updatedEntries: Map<Int, AnilistListEntry>,
    oldName: String,
    newName: String,
): TankobunUiState =
    withUpdatedAniListCustomListEntries(
        customLists = customLists,
        updatedEntries = updatedEntries,
        transformEntry = { entry ->
            entry.copy(customLists = entry.customLists.renamedCustomList(oldName, newName))
        },
        transformSelectedLists = { lists ->
            lists.renamedCustomList(oldName, newName).toSet()
        },
        message = "Custom list renamed",
    )

internal fun TankobunUiState.withDeletedAniListCustomList(
    customLists: List<String>,
    updatedEntries: Map<Int, AnilistListEntry>,
    name: String,
): TankobunUiState =
    withUpdatedAniListCustomListEntries(
        customLists = customLists,
        updatedEntries = updatedEntries,
        transformEntry = { entry ->
            entry.copy(customLists = entry.customLists.withoutCustomList(name))
        },
        transformSelectedLists = { lists ->
            lists.withoutCustomList(name).toSet()
        },
        message = "Custom list deleted",
    )

internal fun TankobunUiState.withTrackingCustomListSaveResult(
    media: AnilistMedia,
    entry: AnilistListEntry,
    knownCustomLists: List<String>,
): TankobunUiState {
    val nextItems = libraryItems.withLibraryEntry(media, entry)
    return copy(
        anilistCustomLists = knownCustomLists,
        library = nextItems.map { item -> item.media },
        libraryItems = nextItems,
        selectedListEntry = entry,
        trackingStatus = entry.status,
        trackingProgress = entry.progress.toString(),
        trackingScore = entry.score.formatTrackingScore(anilistScoreFormat),
        trackingNotes = entry.notes.orEmpty(),
        trackingPrivate = entry.private,
        trackingCustomLists = entry.customLists.toSet(),
    )
}

internal fun TankobunUiState.withTrackingSaveStarted(
    media: AnilistMedia,
    entry: AnilistListEntry,
    knownCustomLists: List<String>,
    autoSave: Boolean,
): TankobunUiState {
    val selected = selectedMedia?.id == media.id
    val nextItems = libraryItems.withLibraryEntry(media, entry)
    return copy(
        anilistCustomLists = knownCustomLists,
        library = nextItems.map { item -> item.media },
        libraryItems = nextItems,
        selectedListEntry = if (selected) entry else selectedListEntry,
        trackingDirty = if (selected) false else trackingDirty,
        trackingSaveInProgress = if (selected) true else trackingSaveInProgress,
        trackingSaveFailed = if (selected) false else trackingSaveFailed,
        busy = if (autoSave) busy else true,
        message = null,
    )
}

internal fun TankobunUiState.withTrackingSaveResult(
    media: AnilistMedia,
    entry: AnilistListEntry,
    knownCustomLists: List<String>,
    autoSave: Boolean,
): TankobunUiState {
    val selected = selectedMedia?.id == media.id
    val preserveEditedForm = selected && trackingDirty
    val nextItems = libraryItems.withLibraryEntry(media, entry)
    return copy(
        anilistCustomLists = knownCustomLists,
        library = nextItems.map { item -> item.media },
        libraryItems = nextItems,
        selectedListEntry = if (selected) entry else selectedListEntry,
        trackingStatus = if (!selected || preserveEditedForm) trackingStatus else entry.status,
        trackingProgress = if (!selected || preserveEditedForm) trackingProgress else entry.progress.toString(),
        trackingScore = if (!selected || preserveEditedForm) trackingScore else entry.score.formatTrackingScore(anilistScoreFormat),
        trackingNotes = if (!selected || preserveEditedForm) trackingNotes else entry.notes.orEmpty(),
        trackingPrivate = if (!selected || preserveEditedForm) trackingPrivate else entry.private,
        trackingCustomLists = if (!selected || preserveEditedForm) trackingCustomLists else entry.customLists.toSet(),
        trackingDirty = if (selected) preserveEditedForm else trackingDirty,
        trackingSaveInProgress = if (selected) false else trackingSaveInProgress,
        trackingSaveFailed = if (selected) false else trackingSaveFailed,
        busy = if (autoSave) busy else false,
        message = if (autoSave) message else "AniList tracking saved",
    )
}

internal fun TankobunUiState.withTrackingSaveFailure(
    mediaId: Int,
    autoSave: Boolean,
    message: String,
): TankobunUiState {
    val selected = selectedMedia?.id == mediaId
    return copy(
        trackingDirty = if (selected) true else trackingDirty,
        trackingSaveInProgress = if (selected) false else trackingSaveInProgress,
        trackingSaveFailed = if (selected) true else trackingSaveFailed,
        busy = if (autoSave) busy else false,
        message = message,
    )
}

private fun TankobunUiState.withUpdatedAniListCustomListEntries(
    customLists: List<String>,
    updatedEntries: Map<Int, AnilistListEntry>,
    transformEntry: (AnilistListEntry) -> AnilistListEntry,
    transformSelectedLists: (Set<String>) -> Set<String>,
    message: String,
): TankobunUiState {
    val nextItems = libraryItems.map { item ->
        val entry = updatedEntries[item.media.id] ?: transformEntry(item.entry)
        item.copy(entry = entry)
    }.sortedByTitle()
    return copy(
        anilistCustomLists = customLists,
        libraryItems = nextItems,
        library = nextItems.map { item -> item.media },
        selectedListEntry = selectedListEntry?.let { entry ->
            updatedEntries[entry.mediaId] ?: transformEntry(entry)
        },
        trackingCustomLists = transformSelectedLists(trackingCustomLists),
        busy = false,
        message = message,
    )
}

private fun List<LibraryItem>.withLibraryEntry(
    media: AnilistMedia,
    entry: AnilistListEntry,
): List<LibraryItem> =
    (filterNot { item -> item.media.id == media.id } + LibraryItem(media, entry)).sortedByTitle()

private fun List<LibraryItem>.sortedByTitle(): List<LibraryItem> =
    sortedBy { item -> item.media.title.userPreferred.lowercase(Locale.ROOT) }
