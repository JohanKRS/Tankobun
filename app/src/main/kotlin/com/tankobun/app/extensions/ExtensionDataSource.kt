package com.tankobun.app.extensions

import com.tankobun.core.extensions.readingContentKind
import android.net.Uri
import androidx.core.content.FileProvider
import com.tankobun.app.AppContainer
import com.tankobun.core.network.TransferLimits
import com.tankobun.core.network.downloadCodeFile
import com.tankobun.app.logic.preferredVisibleSources
import com.tankobun.app.logic.visibleSources
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.extensions.ExtensionIndexResult
import com.tankobun.core.model.SourceDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class InstalledExtensionVersion(
    val versionCode: Int,
    val versionName: String,
)

internal data class InstalledSourceState(
    val allSources: List<SourceDescriptor>,
    val preferredSources: List<SourceDescriptor>,
    val untrustedExtensions: List<com.tankobun.core.extensions.UntrustedExtension>,
)

internal class ExtensionDataSource(
    private val container: AppContainer,
) {
    val repositoryTrust = com.tankobun.core.extensions.RepositoryTrustStore(container.application)
    private val extensionApkValidator = ExtensionApkValidator(container.application)
    private val packages = com.tankobun.core.extensions.ExtensionPackageStore(container.application)

    fun usesAndroidInstaller(packageName: String): Boolean =
        !packageName.startsWith(com.tankobun.core.extensions.novel.LNREADER_PACKAGE_PREFIX) &&
            !packages.isPrivate(packageName) && packages.systemPackage(packageName) != null

    suspend fun installPrivateExtension(apkUri: Uri, entry: ExtensionIndexEntry) = withContext(Dispatchers.IO) {
        repositoryTrust.verifyEntry(entry)
        // Downloaded APKs already passed repository/package/version/signature checks.
        val file = File(container.application.cacheDir, "extension_apks/${entry.packageName}-${entry.versionCode}.apk")
        check(FileProvider.getUriForFile(container.application, "${container.application.packageName}.fileprovider", file) == apkUri)
        extensionApkValidator.validate(file, entry)
        packages.installPrivate(file, entry.packageName, entry.versionCode)
    }

    suspend fun migrateExtension(packageName: String) = withContext(Dispatchers.IO) {
        val installed = packages.systemPackage(packageName) ?: error("Extension is not installed in Android")
        val archive = File(requireNotNull(installed.applicationInfo).sourceDir)
        val expected = ExtensionIndexEntry(packageName, packageName, "", "", installed.longVersionCode.toInt(), installed.versionName.orEmpty())
        extensionApkValidator.validate(archive, expected)
        packages.installPrivate(archive, packageName, expected.versionCode)
        check(packages.isPrivate(packageName)) { "Private extension could not be activated" }
    }

    suspend fun removePrivateExtension(packageName: String) = withContext(Dispatchers.IO) {
        check(packages.uninstallPrivate(packageName)) { "Extension could not be removed" }
    }

    suspend fun installedSourceState(
        preferredLanguages: Set<String>,
        disabledSourceKeys: Set<String>,
    ): InstalledSourceState = withContext(Dispatchers.IO) {
        val packages = container.extensionScanner.installedExtensions()
        val untrusted = packages.mapNotNull(container.extensionTrustStore::untrustedExtension)
        val untrustedPackages = untrusted.mapTo(mutableSetOf()) { it.descriptor.packageName }
        container.sourceHost.retainInstalledPackages(packages.map { it.packageName }.toSet())
        val discoveredSources = packages.filterNot { it.packageName in untrustedPackages }.flatMap { descriptor ->
            runCatching {
                container.sourceHost.loadSources(descriptor.packageName).map { source ->
                    descriptor.copy(
                        id = source.id,
                        name = source.name,
                        lang = source.lang,
                        contentKind = source.readingContentKind(),
                    )
                }
            }.getOrDefault(emptyList()).ifEmpty { listOf(descriptor) }
        }
        val allSources = discoveredSources.visibleSources()
        InstalledSourceState(
            allSources = allSources,
            untrustedExtensions = untrusted,
            preferredSources = allSources.preferredVisibleSources(
                preferredLanguages = preferredLanguages,
                disabledSourceKeys = disabledSourceKeys,
            ),
        )
    }

    suspend fun fetchExtensionIndex(repositoryUrl: String): ExtensionIndexResult {
        val fetchUrl = repositoryTrust.fetchUrl(repositoryUrl)
        return container.extensionRepository.fetchIndex(fetchUrl).also { result ->
            withContext(Dispatchers.IO) { repositoryTrust.checkAndRemember(fetchUrl, result) }
        }
    }

    fun extensionApkUrl(repositoryUrl: String, entry: ExtensionIndexEntry): String =
        container.extensionRepository.apkUrl(repositoryUrl, entry)

    fun extensionIconUrl(repositoryUrl: String, entry: ExtensionIndexEntry): String =
        container.extensionRepository.iconUrl(repositoryUrl, entry)

    fun installedExtensionVersion(packageName: String): InstalledExtensionVersion? =
        runCatching {
            if (packageName.startsWith(com.tankobun.core.extensions.novel.LNREADER_PACKAGE_PREFIX)) {
                return@runCatching com.tankobun.core.extensions.novel.LnReaderPluginStore(container.application).record(packageName)?.first
                    ?.let { InstalledExtensionVersion(it.versionCode, it.version) }
            }
            val packageInfo = packages.packageInfo(packageName) ?: return@runCatching null
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
            repositoryTrust.verifyEntry(entry)
            extensionApkValidator.validateIndexEntry(entry)
            val cacheDir = File(container.application.cacheDir, "extension_apks").also { it.mkdirs() }
            val safeName = "${entry.packageName}-${entry.versionCode}.apk"
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
            val apkFile = File(cacheDir, safeName)

            cacheDir.listFiles()
                ?.filter { it.name.startsWith(entry.packageName) && it.name != apkFile.name }
                ?.forEach { it.delete() }

            try {
                downloadCodeFile(container.okHttpClient, apkUrl, apkFile, TransferLimits.EXTENSION_APK_BYTES) {
                    extensionApkValidator.validate(it, entry)
                }

                FileProvider.getUriForFile(
                    container.application,
                    "${container.application.packageName}.fileprovider",
                    apkFile,
                )
            } catch (error: Throwable) {
                apkFile.delete()
                throw error
            }
        }
}
