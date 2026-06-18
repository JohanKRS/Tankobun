package com.tankobun.app.logic

import com.tankobun.app.LibraryMode

internal const val CURRENT_ONBOARDING_VERSION = 3

internal fun initialLibraryModeForStartup(
    storedMode: LibraryMode,
    hasAccessToken: Boolean,
    onboardingVersion: Int,
): LibraryMode =
    if (hasAccessToken && onboardingVersion < CURRENT_ONBOARDING_VERSION) {
        LibraryMode.ANILIST
    } else {
        storedMode
    }

internal fun shouldShowOnboarding(onboardingVersion: Int): Boolean =
    onboardingVersion < CURRENT_ONBOARDING_VERSION
