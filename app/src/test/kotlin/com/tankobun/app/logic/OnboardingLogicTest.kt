package com.tankobun.app.logic

import com.tankobun.app.LibraryMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingLogicTest {
    @Test
    fun existingTokenBeforeCurrentVersionDefaultsToAniListMode() {
        assertEquals(
            LibraryMode.ANILIST,
            initialLibraryModeForStartup(
                storedMode = LibraryMode.LOCAL,
                hasAccessToken = true,
                onboardingVersion = 1,
            ),
        )
    }

    @Test
    fun completedCurrentVersionKeepsStoredLibraryModeEvenWithToken() {
        assertEquals(
            LibraryMode.LOCAL,
            initialLibraryModeForStartup(
                storedMode = LibraryMode.LOCAL,
                hasAccessToken = true,
                onboardingVersion = CURRENT_ONBOARDING_VERSION,
            ),
        )
    }

    @Test
    fun onboardingShowsOnlyBelowCurrentVersion() {
        assertTrue(shouldShowOnboarding(CURRENT_ONBOARDING_VERSION - 1))
        assertFalse(shouldShowOnboarding(CURRENT_ONBOARDING_VERSION))
    }

    @Test
    fun versionTwoUsersSeeOnboardingVersionThree() {
        assertTrue(shouldShowOnboarding(2))
        assertFalse(shouldShowOnboarding(3))
    }
}
