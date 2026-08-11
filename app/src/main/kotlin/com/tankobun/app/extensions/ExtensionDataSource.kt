package com.tankobun.app.extensions

import android.net.Uri
import androidx.core.content.FileProvider
import com.tankobun.app.AppContainer
import com.tankobun.app.logic.preferredVisibleSources
import com.tankobun.app.logic.visibleSources
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.extensions.ExtensionIndexResult
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

internal data class InstalledExtensionVersion(
    val versionCode: Int,
    val versionName: String,
)

internal data class InstalledSourceState(
    val allSources: List<SourceDescriptor>,
    val preferredSources: List<SourceDescriptor>,
)

internal class ExtensionDataSource(
    private val container: AppContainer,
) {
    private val extensionApkValidator = ExtensionApkValidator(container.application.packageManager)

    suspend fun installedSourceState(
        preferredLanguages: Set<String>,
        disabledSourceKeys: Set<String>,
    ): InstalledSourceState {
        val packages = container.extensionScanner.installedExtensions()
        container.sourceHost.retainInstalledPackages(packages.map { it.packageName }.toSet())
        val discoveredSources = packages.flatMap { descriptor ->
            runCatching {
                container.sourceHost.loadSources(descriptor.packageName).map { source ->
                    descriptor.copy(
                        id = source.id,
                        name = source.name,
                        lang = source.lang,
                    )
                }
            }.getOrDefault(emptyList()).ifEmpty { listOf(descriptor) }
        }
        val allSources = discoveredSources.visibleSources()
        return InstalledSourceState(
            allSources = allSources,
            preferredSources = allSources.preferredVisibleSources(
                preferredLanguages = preferredLanguages,
                disabledSourceKeys = disabledSourceKeys,
            ),
        )
    }

    suspend fun fetchExtensionIndex(repositoryUrl: String): ExtensionIndexResult =
        container.extensionRepository.fetchIndex(repositoryUrl)

    fun extensionApkUrl(repositoryUrl: String, entry: ExtensionIndexEntry): String =
        container.extensionRepository.apkUrl(repositoryUrl, entry)

    fun extensionIconUrl(repositoryUrl: String, entry: ExtensionIndexEntry): String =
        container.extensionRepository.iconUrl(repositoryUrl, entry)

    fun installedExtensionVersion(packageName: String): InstalledExtensionVersion? =
        runCatching {
            val packageInfo = container.application.packageManager.getPackageInfo(packageName, 0)
            InstalledExtensionVersion(
                versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                },
                versionName = packageInfo.versionName.orEmpty(),
            )
        }.getOrNull()

    suspend fun downloadExtensionApk(apkUrl: String, entry: ExtensionIndexEntry): Uri =
        withContext(Dispatchers.IO) {
            extensionApkValidator.validateIndexEntry(entry)
            val cacheDir = File(container.application.cacheDir, "extension_apks").also { it.mkdirs() }
            val safeName = "${entry.packageName}-${entry.versionCode}.apk"
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(cacheDir, safeName)
            val partialFile = File(cacheDir, "$safeName.part")

            cacheDir.listFiles()
                ?.filter { it.name.startsWith(entry.packageName) && it.name != apkFile.name }
                ?.forEach { it.delete() }

            try {
                val request = Request.Builder().url(apkUrl).build()
                container.okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("APK download failed: HTTP ${response.code}")
                    }
                    val body = response.body
                    partialFile.outputStream().use { output ->
                        body.byteStream().use { input -> input.copyTo(output) }
                    }
                }

                if (partialFile.length() <= 0L) {
                    error("APK download failed: empty file")
                }
                if (apkFile.exists()) apkFile.delete()
                check(partialFile.renameTo(apkFile)) { "APK download failed: could not finalize file" }

                extensionApkValidator.validate(apkFile, entry)

                FileProvider.getUriForFile(
                    container.application,
                    "${container.application.packageName}.fileprovider",
                    apkFile,
                )
            } catch (error: Throwable) {
                partialFile.delete()
                apkFile.delete()
                throw error
            }
        }
}
