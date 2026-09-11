package com.tankobun.app.updates

import org.junit.Assert.assertThrows
import org.junit.Test

class AppUpdateApkValidatorTest {
    private val old = AppApkIdentity("com.tankobun.app", 49, "4.2.1", setOf("current"), setOf("past", "current"), false)
    private val update = AppUpdateInfo(50, "4.2.2", "https://cdn.test/a.apk", "a".repeat(64), null, null, null, false, emptyMap())
    private val candidate = old.copy(versionCode = 50, versionName = "4.2.2")

    @Test fun acceptsSameSignerAndForwardVerifiedRotation() {
        validateAppUpdateIdentity(old, candidate, update)
        validateAppUpdateIdentity(old, candidate.copy(currentSigners = setOf("next"), signerHistory = setOf("past", "current", "next")), update)
    }

    @Test fun rejectsWrongPackageVersionSignatureAndMissingHash() {
        listOf(candidate.copy(packageName = "other.app"), candidate.copy(versionCode = 49), candidate.copy(versionCode = 51),
            candidate.copy(versionName = "different"), candidate.copy(currentSigners = emptySet()),
            candidate.copy(currentSigners = setOf("other"), signerHistory = setOf("other")),
            candidate.copy(currentSigners = setOf("past"), signerHistory = setOf("past"))).forEach {
            assertThrows(IllegalArgumentException::class.java) { validateAppUpdateIdentity(old, it, update) }
        }
        assertThrows(IllegalArgumentException::class.java) { validateAppUpdateIdentity(old, candidate, update.copy(apkSha256 = null)) }
    }

    @Test fun multipleSignersRequireTheEntireExactSet() {
        val installed = old.copy(currentSigners = setOf("a", "b"), multipleSigners = true)
        validateAppUpdateIdentity(installed, candidate.copy(currentSigners = setOf("a", "b"), multipleSigners = true), update)
        assertThrows(IllegalArgumentException::class.java) {
            validateAppUpdateIdentity(installed, candidate.copy(currentSigners = setOf("a", "b", "c"), multipleSigners = true), update)
        }
    }
}
