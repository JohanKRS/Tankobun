package com.tankobun.app.state

import com.tankobun.core.extensions.UntrustedExtension
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.model.SourceManga
import com.tankobun.app.logic.withSourcePickerNoSources
import org.junit.Assert.*
import org.junit.Test

class ExtensionTrustStateTest {
    private val bound = SourceDescriptor(42, "Original source", "en", "fixture.extension", "1", 1, false, true)
    private val pending = UntrustedExtension(bound.copy(id = 99), setOf("a".repeat(64)))
    private val manga = SourceManga(bound.id, "original-manga", "Original title", null, null, null, null, null)
    private fun selected() = TankobunUiState(
        selectedSourceId = bound.id,
        selectedSourcePackageName = bound.packageName,
        selectedSourceManga = manga,
    )

    @Test fun reviewMatchesTheBoundPackageBeforeItsSourcesCanBeLoaded() {
        val state = selected().copy(untrustedExtensions = listOf(pending))
        assertEquals(pending, state.selectedSourceAwaitingTrust)
        assertNull(state.selectedSource)
        assertEquals(manga, state.selectedSourceManga)
    }

    @Test fun matchingIdInAnotherPackageCannotReplaceTheBoundSource() {
        val other = bound.copy(packageName = "another.extension")
        val state = selected().copy(allInstalledSources = listOf(other), installedSources = listOf(other))
        assertNull(state.selectedSource)
        assertNull(state.selectedSourceAwaitingTrust)
    }

    @Test fun approvalRestoresTheSourceWithoutChangingItsSavedSelection() {
        val pendingState = selected().copy(untrustedExtensions = listOf(pending))
        val approved = pendingState.copy(untrustedExtensions = emptyList(), allInstalledSources = listOf(bound))
        assertEquals(bound, approved.selectedSource)
        assertEquals(manga, approved.selectedSourceManga)
        assertNull(approved.selectedSourceAwaitingTrust)
    }

    @Test fun unrelatedPendingExtensionsDoNotBlockAnApprovedSource() {
        val state = selected().copy(
            allInstalledSources = listOf(bound),
            untrustedExtensions = listOf(pending.copy(descriptor = pending.descriptor.copy(packageName = "unrelated.extension"))),
        )
        assertNull(state.selectedSourceAwaitingTrust)
        assertEquals(bound, state.selectedSource)
    }

    @Test fun staleSourceInstancesRemainUnavailableUntilApproval() {
        val state = selected().copy(untrustedExtensions = listOf(pending), allInstalledSources = listOf(bound))
        assertNull(state.selectedSource)
    }

    @Test fun pendingReviewDoesNotShowTheMissingExtensionBannerInThePicker() {
        val state = selected().copy(untrustedExtensions = listOf(pending), sourcePickerOpen = true)
            .withSourcePickerNoSources()
        assertNull(state.sourcePickerMessage)
        assertTrue(state.sourcePickerOpen)
    }
}
