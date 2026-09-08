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
    private val packages = ExtensionPackageStore(context)
    private val preferences = context.applicationContext.getSharedPreferences("extension_trust", Context.MODE_PRIVATE)

    fun isTrusted(packageInfo: PackageInfo): Boolean {
        if (!packageInfo.isExtensionPackage()) return false
        val key = extensionTrustKey(packageInfo.packageName, packageInfo.extensionSignerFingerprints()) ?: return false
        return preferences.getString(packageInfo.packageName, null) == key
    }

    fun untrustedExtension(descriptor: SourceDescriptor): UntrustedExtension? {
        val pkg = installedPackage(descriptor.packageName) ?: return null
        if (isTrusted(pkg)) return null
        return UntrustedExtension(descriptor, pkg.extensionSignerFingerprints())
    }

    fun approve(candidate: UntrustedExtension): Boolean {
        val pkg = installedPackage(candidate.descriptor.packageName) ?: return false
        if (!pkg.isExtensionPackage() || pkg.extensionSignerFingerprints() != candidate.signerFingerprints) return false
        val key = extensionTrustKey(pkg.packageName, candidate.signerFingerprints) ?: return false
        // A synchronous commit makes the explicit decision durable before constructors run.
        return preferences.edit().putString(pkg.packageName, key).commit()
    }

    private fun installedPackage(packageName: String): PackageInfo? = packages.packageInfo(packageName)
}

internal const val EXTENSION_PACKAGE_FLAGS = PackageManager.GET_META_DATA or
    PackageManager.GET_CONFIGURATIONS or PackageManager.GET_SIGNING_CERTIFICATES

internal fun PackageInfo.isExtensionPackage(): Boolean =
    isSupportedExtensionPackageName(packageName) &&
        reqFeatures.orEmpty().any { it.name == "tachiyomi.extension" || it.name == NOVEL_EXTENSION_FEATURE }

internal fun PackageInfo.extensionSignerFingerprints(): Set<String> = signingInfo?.apkContentsSigners.orEmpty()
    .mapTo(sortedSetOf()) { signer ->
        MessageDigest.getInstance("SHA-256").digest(signer.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

internal fun PackageInfo.extensionSignerHistory(): Set<String> = signingInfo?.let { info ->
    (if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory).orEmpty()
        .mapTo(sortedSetOf()) { signer -> MessageDigest.getInstance("SHA-256").digest(signer.toByteArray()).joinToString("") { "%02x".format(it) } }
}.orEmpty()

internal fun extensionTrustKey(packageName: String, fingerprints: Set<String>): String? {
    if (fingerprints.isEmpty() || fingerprints.any { !it.matches(Regex("[a-f0-9]{64}")) }) return null
    return "$packageName:${fingerprints.sorted().joinToString(":")}"
}
