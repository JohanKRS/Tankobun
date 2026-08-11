package com.tankobun.app.extensions

import com.tankobun.core.extensions.ExtensionIndexEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtensionApkValidatorTest {
    @Test
    fun acceptsMatchingSignedExtensionFromRepository() {
        assertNull(
            extensionApkValidationFailure(
                expected = entry(repositorySigningKey = SIGNER),
                downloaded = identity(),
            ),
        )
    }

    @Test
    fun acceptsLegacyRepositoryAndPreservesInstalledSignerContinuity() {
        assertNull(
            extensionApkValidationFailure(
                expected = entry(repositorySigningKey = null),
                downloaded = identity(signerFingerprints = setOf(SIGNER, ROTATED_SIGNER)),
                installedSignerFingerprints = setOf(SIGNER),
            ),
        )
    }

    @Test
    fun rejectsArchiveWhosePackageDoesNotMatchIndex() {
        assertEquals(
            ExtensionApkValidationFailure.PACKAGE_MISMATCH,
            extensionApkValidationFailure(
                expected = entry(),
                downloaded = identity(packageName = "eu.kanade.tachiyomi.extension.en.other"),
            ),
        )
    }

    @Test
    fun rejectsIndexEntryOutsideExtensionPackageNamespace() {
        assertEquals(
            ExtensionApkValidationFailure.INVALID_INDEX_ENTRY,
            extensionIndexEntryValidationFailure(
                entry().copy(packageName = "com.example.unrelated"),
            ),
        )
    }

    @Test
    fun rejectsIndexEntryWithoutPositiveVersionCode() {
        assertEquals(
            ExtensionApkValidationFailure.INVALID_INDEX_ENTRY,
            extensionIndexEntryValidationFailure(entry().copy(versionCode = 0)),
        )
    }

    @Test
    fun rejectsArchiveWhoseVersionDoesNotMatchIndex() {
        assertEquals(
            ExtensionApkValidationFailure.VERSION_MISMATCH,
            extensionApkValidationFailure(
                expected = entry(),
                downloaded = identity(versionCode = 8L),
            ),
        )
    }

    @Test
    fun rejectsOrdinaryApkEvenWhenPackageAndVersionMatch() {
        assertEquals(
            ExtensionApkValidationFailure.NOT_EXTENSION,
            extensionApkValidationFailure(
                expected = entry(),
                downloaded = identity(hasExtensionFeature = false),
            ),
        )
    }

    @Test
    fun rejectsUnsignedArchive() {
        assertEquals(
            ExtensionApkValidationFailure.UNSIGNED,
            extensionApkValidationFailure(
                expected = entry(),
                downloaded = identity(signerFingerprints = emptySet()),
            ),
        )
    }

    @Test
    fun rejectsArchiveNotSignedByRepositoryKey() {
        assertEquals(
            ExtensionApkValidationFailure.REPOSITORY_SIGNATURE_MISMATCH,
            extensionApkValidationFailure(
                expected = entry(repositorySigningKey = SIGNER),
                downloaded = identity(signerFingerprints = setOf(ROTATED_SIGNER)),
            ),
        )
    }

    @Test
    fun rejectsSignerChangeWhenUpdatingLegacyExtension() {
        assertEquals(
            ExtensionApkValidationFailure.INSTALLED_SIGNATURE_MISMATCH,
            extensionApkValidationFailure(
                expected = entry(repositorySigningKey = null),
                downloaded = identity(signerFingerprints = setOf(ROTATED_SIGNER)),
                installedSignerFingerprints = setOf(SIGNER),
            ),
        )
    }

    @Test
    fun normalizesStandardSha256FingerprintFormats() {
        val colonSeparated = SIGNER.chunked(2).joinToString(":").uppercase()

        assertEquals(SIGNER, normalizeExtensionSigningFingerprint("sha256:$colonSeparated"))
        assertEquals(null, normalizeExtensionSigningFingerprint("not-a-fingerprint"))
    }

    private fun entry(repositorySigningKey: String? = SIGNER): ExtensionIndexEntry =
        ExtensionIndexEntry(
            name = "Fixture",
            packageName = PACKAGE_NAME,
            apkName = "fixture.apk",
            lang = "en",
            versionCode = 9,
            versionName = "1.4.9",
            repositorySigningKey = repositorySigningKey,
        )

    private fun identity(
        packageName: String = PACKAGE_NAME,
        versionCode: Long = 9L,
        hasExtensionFeature: Boolean = true,
        signerFingerprints: Set<String> = setOf(SIGNER),
    ): ExtensionApkIdentity =
        ExtensionApkIdentity(
            packageName = packageName,
            versionCode = versionCode,
            hasExtensionFeature = hasExtensionFeature,
            signerFingerprints = signerFingerprints,
        )

    private companion object {
        const val PACKAGE_NAME = "eu.kanade.tachiyomi.extension.en.fixture"
        const val SIGNER = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        const val ROTATED_SIGNER = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    }
}
