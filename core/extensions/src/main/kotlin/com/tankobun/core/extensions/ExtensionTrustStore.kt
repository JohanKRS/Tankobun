package com.tankobun.core.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.tankobun.core.model.SourceDescriptor
import java.security.MessageDigest

data class UntrustedExtension(
    val descriptor: SourceDescriptor,
    val signerFingerprints: Set<String>,
)

/** Approval belongs to a package and its current signing identity, never its label. */
class ExtensionTrustStore(context: Context) {
    private val packageManager = context.applicationContext.packageManager
    private val preferences = context.applicationContext.getSharedPreferences("extension_trust", Context.MODE_PRIVATE)

    fun isTrusted(packageInfo: PackageInfo): Boolean {
        if (!packageInfo.isExtensionPackage()) return false
        val key = extensionTrustKey(packageInfo.packageName, packageInfo.signerFingerprints()) ?: return false
        return preferences.getString(packageInfo.packageName, null) == key
    }

    fun untrustedExtension(descriptor: SourceDescriptor): UntrustedExtension? {
        val pkg = installedPackage(descriptor.packageName) ?: return null
        if (isTrusted(pkg)) return null
        return UntrustedExtension(descriptor, pkg.signerFingerprints())
    }

    fun approve(candidate: UntrustedExtension): Boolean {
        val pkg = installedPackage(candidate.descriptor.packageName) ?: return false
        if (!pkg.isExtensionPackage() || pkg.signerFingerprints() != candidate.signerFingerprints) return false
        val key = extensionTrustKey(pkg.packageName, candidate.signerFingerprints) ?: return false
        // A synchronous commit makes the explicit decision durable before constructors run.
        return preferences.edit().putString(pkg.packageName, key).commit()
    }

    @Suppress("DEPRECATION")
    private fun installedPackage(packageName: String): PackageInfo? = runCatching {
        packageManager.getPackageInfo(packageName, EXTENSION_PACKAGE_FLAGS)
    }.getOrNull()
}

internal const val EXTENSION_PACKAGE_FLAGS = PackageManager.GET_META_DATA or
    PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNING_CERTIFICATES

internal fun PackageInfo.isExtensionPackage(): Boolean =
    packageName.startsWith("${InstalledExtensionScanner.TACHIYOMI_EXTENSION_PREFIX}.") &&
        reqFeatures.orEmpty().any { it.name == "tachiyomi.extension" }

private fun PackageInfo.signerFingerprints(): Set<String> = signingInfo?.apkContentsSigners.orEmpty()
    .mapTo(sortedSetOf()) { signer ->
        MessageDigest.getInstance("SHA-256").digest(signer.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

internal fun extensionTrustKey(packageName: String, fingerprints: Set<String>): String? {
    if (fingerprints.isEmpty() || fingerprints.any { !it.matches(Regex("[a-f0-9]{64}")) }) return null
    return "$packageName:${fingerprints.sorted().joinToString(":")}"
}
