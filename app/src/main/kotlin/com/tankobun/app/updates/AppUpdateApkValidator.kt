package com.tankobun.app.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.tankobun.core.network.normalizedSha256
import java.io.File
import java.security.MessageDigest

internal data class AppApkIdentity(
    val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val currentSigners: Set<String>,
    val signerHistory: Set<String>,
    val multipleSigners: Boolean,
)

internal class AppUpdateApkValidator(private val context: Context) {
    fun validate(file: File, update: AppUpdateInfo) {
        @Suppress("DEPRECATION")
        val installed = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        @Suppress("DEPRECATION")
        val candidate = context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("Update APK is invalid or unsigned")
        validateAppUpdateIdentity(installed.identity(), candidate.identity(), update)
    }
}

internal fun validateAppUpdateIdentity(installed: AppApkIdentity, candidate: AppApkIdentity, update: AppUpdateInfo) {
    require(normalizedSha256(update.apkSha256) != null) { "Update SHA-256 is required" }
    require(candidate.packageName == installed.packageName) { "Update APK package mismatch" }
    require(candidate.versionCode > installed.versionCode) { "Update APK must have a newer version" }
    require(candidate.versionCode == update.versionCode.toLong() && candidate.versionName == update.versionName) { "Update APK version mismatch" }
    require(installed.currentSigners.isNotEmpty() && candidate.currentSigners.isNotEmpty()) { "Update APK is unsigned" }
    val continuous = if (installed.multipleSigners || candidate.multipleSigners) {
        installed.currentSigners == candidate.currentSigners
    } else {
        // Only forward rotation: the candidate must prove authorization by the
        // currently installed signer. Intersecting both histories permits rollback.
        candidate.signerHistory.containsAll(installed.currentSigners)
    }
    require(continuous) { "Update APK signature mismatch" }
}

private fun PackageInfo.identity(): AppApkIdentity {
    val info = signingInfo
    fun fingerprints(signatures: Array<android.content.pm.Signature>?): Set<String> = signatures.orEmpty().mapTo(mutableSetOf()) {
        MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
    }
    return AppApkIdentity(packageName, longVersionCode, versionName.orEmpty(), fingerprints(info?.apkContentsSigners),
        fingerprints(if (info?.hasMultipleSigners() == true) info.apkContentsSigners else info?.signingCertificateHistory),
        info?.hasMultipleSigners() == true)
}
