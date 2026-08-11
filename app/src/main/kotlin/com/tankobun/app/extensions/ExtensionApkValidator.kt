package com.tankobun.app.extensions

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.tankobun.core.extensions.ExtensionIndexEntry
import java.io.File
import java.security.MessageDigest
import java.util.Locale

internal enum class ExtensionApkValidationFailure {
    INVALID_INDEX_ENTRY,
    INVALID_ARCHIVE,
    PACKAGE_MISMATCH,
    VERSION_MISMATCH,
    NOT_EXTENSION,
    UNSIGNED,
    INVALID_REPOSITORY_SIGNING_KEY,
    REPOSITORY_SIGNATURE_MISMATCH,
    INSTALLED_SIGNATURE_MISMATCH,
}

internal class ExtensionApkValidationException(
    val failure: ExtensionApkValidationFailure,
) : IllegalStateException("Extension package validation failed: ${failure.name.lowercase(Locale.ROOT)}")

internal data class ExtensionApkIdentity(
    val packageName: String,
    val versionCode: Long,
    val hasExtensionFeature: Boolean,
    val signerFingerprints: Set<String>,
)

internal class ExtensionApkValidator(
    private val packageManager: PackageManager,
) {
    fun validateIndexEntry(expected: ExtensionIndexEntry) {
        extensionIndexEntryValidationFailure(expected)?.let { failure ->
            throw ExtensionApkValidationException(failure)
        }
    }

    fun validate(apkFile: File, expected: ExtensionIndexEntry) {
        validateIndexEntry(expected)
        val downloadedPackage = packageManager.getPackageArchiveInfoCompat(apkFile)
            ?: throw ExtensionApkValidationException(ExtensionApkValidationFailure.INVALID_ARCHIVE)
        val downloadedIdentity = downloadedPackage.toExtensionApkIdentity()
        val installedSigners = packageManager.installedSignerFingerprints(expected.packageName)
        extensionApkValidationFailure(
            expected = expected,
            downloaded = downloadedIdentity,
            installedSignerFingerprints = installedSigners,
        )?.let { failure -> throw ExtensionApkValidationException(failure) }
    }
}

internal fun extensionIndexEntryValidationFailure(
    expected: ExtensionIndexEntry,
): ExtensionApkValidationFailure? =
    if (!expected.packageName.startsWith("$EXTENSION_PACKAGE_PREFIX.") || expected.versionCode <= 0) {
        ExtensionApkValidationFailure.INVALID_INDEX_ENTRY
    } else {
        null
    }

internal fun extensionApkValidationFailure(
    expected: ExtensionIndexEntry,
    downloaded: ExtensionApkIdentity,
    installedSignerFingerprints: Set<String>? = null,
): ExtensionApkValidationFailure? {
    extensionIndexEntryValidationFailure(expected)?.let { return it }
    if (downloaded.packageName != expected.packageName) {
        return ExtensionApkValidationFailure.PACKAGE_MISMATCH
    }
    if (downloaded.versionCode != expected.versionCode.toLong()) {
        return ExtensionApkValidationFailure.VERSION_MISMATCH
    }
    if (!downloaded.hasExtensionFeature) {
        return ExtensionApkValidationFailure.NOT_EXTENSION
    }
    if (downloaded.signerFingerprints.isEmpty()) {
        return ExtensionApkValidationFailure.UNSIGNED
    }

    expected.repositorySigningKey?.takeIf { it.isNotBlank() }?.let { signingKey ->
        val expectedFingerprint = normalizeExtensionSigningFingerprint(signingKey)
            ?: return ExtensionApkValidationFailure.INVALID_REPOSITORY_SIGNING_KEY
        if (expectedFingerprint !in downloaded.signerFingerprints) {
            return ExtensionApkValidationFailure.REPOSITORY_SIGNATURE_MISMATCH
        }
    }

    if (!installedSignerFingerprints.isNullOrEmpty() &&
        !downloaded.signerFingerprints.containsAll(installedSignerFingerprints)
    ) {
        return ExtensionApkValidationFailure.INSTALLED_SIGNATURE_MISMATCH
    }
    return null
}

internal fun normalizeExtensionSigningFingerprint(value: String): String? {
    val trimmed = value.trim()
    val withoutAlgorithm = if (trimmed.startsWith("sha256:", ignoreCase = true)) {
        trimmed.substringAfter(':')
    } else {
        trimmed
    }
    val normalized = withoutAlgorithm
        .replace(":", "")
        .lowercase(Locale.ROOT)
    return normalized.takeIf { it.length == SHA_256_HEX_LENGTH && it.all(Char::isHexDigit) }
}

private fun PackageManager.getPackageArchiveInfoCompat(apkFile: File): PackageInfo? {
    @Suppress("DEPRECATION")
    return getPackageArchiveInfo(apkFile.absolutePath, PACKAGE_INFO_FLAGS)
}

private fun PackageManager.installedSignerFingerprints(packageName: String): Set<String>? =
    try {
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signerFingerprints()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

private fun PackageInfo.toExtensionApkIdentity(): ExtensionApkIdentity =
    ExtensionApkIdentity(
        packageName = packageName,
        versionCode = longVersionCode,
        hasExtensionFeature = reqFeatures.orEmpty().any { feature -> feature.name == EXTENSION_FEATURE },
        signerFingerprints = signerFingerprints(),
    )

private fun PackageInfo.signerFingerprints(): Set<String> {
    val packageSigningInfo = signingInfo ?: return emptySet()
    val signatures = if (packageSigningInfo.hasMultipleSigners()) {
        packageSigningInfo.apkContentsSigners
    } else {
        packageSigningInfo.signingCertificateHistory
    }
    return signatures
        .orEmpty()
        .mapTo(linkedSetOf()) { signature -> signature.toByteArray().sha256Hex() }
}

private fun ByteArray.sha256Hex(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02x".format(Locale.ROOT, byte.toInt() and 0xFF) }

private fun Char.isHexDigit(): Boolean =
    this in '0'..'9' || this in 'a'..'f'

private const val EXTENSION_FEATURE = "tachiyomi.extension"
private const val EXTENSION_PACKAGE_PREFIX = "eu.kanade.tachiyomi.extension"
private const val SHA_256_HEX_LENGTH = 64

@Suppress("DEPRECATION")
private val PACKAGE_INFO_FLAGS =
    PackageManager.GET_CONFIGURATIONS or
        PackageManager.GET_META_DATA or
        PackageManager.GET_SIGNATURES or
        PackageManager.GET_SIGNING_CERTIFICATES
