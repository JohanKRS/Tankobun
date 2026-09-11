package com.tankobun.app

import android.app.Instrumentation
import com.tankobun.app.extensions.ExtensionApkValidator
import com.tankobun.app.extensions.ExtensionDataSource
import com.tankobun.core.database.SourceBindingEntity
import com.tankobun.core.extensions.ExtensionIndexEntry
import com.tankobun.core.extensions.ExtensionPackageStore
import com.tankobun.core.extensions.SourcePreferenceStore
import eu.kanade.tachiyomi.source.model.FilterList
import java.io.File

/** Original, content-free signed APK fixtures. No internet or source content. */
internal suspend fun Instrumentation.checkPrivateExtensions(afterSystemRemoval: Boolean) {
    val app = targetContext.applicationContext as TankobunApplication
    val packages = ExtensionPackageStore(targetContext)
    val data = ExtensionDataSource(app.container)
    val host = app.container.sourceHost
    val a = "eu.kanade.tachiyomi.extension.en.preferencesqa"
    val b = "eu.kanade.tachiyomi.extension.en.updatesqa"
    val files = File(targetContext.filesDir, "update-qa")
    fun entry(pkg: String, version: Int) = ExtensionIndexEntry(pkg, pkg, "", "en", version, "$version.0")
    fun descriptor(pkg: String) = app.container.extensionScanner.installedExtensions().first { it.packageName == pkg }
    fun sources(pkg: String): List<eu.kanade.tachiyomi.source.Source> {
        app.container.extensionTrustStore.untrustedExtension(descriptor(pkg))?.let { candidate ->
            check(host.loadSources(pkg).isEmpty()) { "Untrusted private code executed" }
            check(app.container.extensionTrustStore.approve(candidate))
        }
        return host.loadSources(pkg)
    }
    val settings = SourcePreferenceStore(targetContext).preferences(987654401L)
    val binding = SourceBindingEntity(-9876501, 987654401L, a, "fixture-book", "Paper QA", null, 1234)
    val progress = com.tankobun.core.database.ReadingProgressEntity(binding.mediaId, "fixture-chapter", 2f, 5, 17, 12,
        com.tankobun.core.model.ReaderMode.PAGED, false, 1234)
    if (!afterSystemRemoval) {
        packages.uninstallPrivate(a)
        packages.uninstallPrivate(b)
        host.clearCache()
        check(packages.systemPackage(a) != null)
        val old = sources(a)
        check(old.size == 2)
        settings.edit().putString("quality", "standard").commit()
        app.container.database.sourceBindingDao().upsertBinding(binding)
        app.container.database.progressDao().upsertProgress(progress)
        data.migrateExtension(a)
        check(packages.isPrivate(a))
        check(packages.systemPackage(a) != null) { "Migration removed another Android app" }
        check(!data.usesAndroidInstaller(a))
        check(descriptor(a).isPrivateExtension && descriptor(a).hasSystemCopy)
        val migrated = sources(a)
        check(migrated.map { it.id } == old.map { it.id })
        check(migrated.first() !== old.first()) { "Migration did not invalidate the old APK path" }
        check(migrated.first().getSearchManga(1, "qa", FilterList()).mangas.single().description == "standard")
        check(packages.icon(a) != null)
        val path = packages.packageInfo(a)!!.applicationInfo!!.sourceDir
        check(path.startsWith(targetContext.noBackupFilesDir.absolutePath) && !File(path).canWrite())
        val validator = ExtensionApkValidator(targetContext)
        val upgrade = File(files, "preferencesqa.apk")
        validator.validate(upgrade, entry(a, 2))
        packages.installPrivate(upgrade, a, 2)
        check(packages.packageInfo(a)!!.longVersionCode == 2L)
        check(packages.packageInfo(a)!!.applicationInfo!!.sourceDir != path)
        check(sources(a).map { it.id } == old.map { it.id })
        check(settings.getString("quality", "") == "standard")
        check(runCatching { packages.installPrivate(File(packages.systemPackage(a)!!.applicationInfo!!.sourceDir), a, 1) }.isFailure)
        check(runCatching { validator.validate(File(files, "corrupted.apk"), entry(a, 2)) }.isFailure) { "Tampered signed APK accepted" }
        check(runCatching { packages.installPrivate(File(files, "corrupted.apk"), a, 2) }.isFailure)
        check(runCatching { validator.validate(File(files, "wrong-signer.apk"), entry(a, 2)) }.isFailure) { "Different signer accepted" }
        check(runCatching { packages.installPrivate(File(files, "wrong-signer.apk"), a, 2) }.isFailure)
        check(packages.packageInfo(a)!!.longVersionCode == 2L) { "Invalid update replaced working version" }
        check(packages.systemPackage(b) == null)
        check(!data.usesAndroidInstaller(b))
        val newApk = File(files, "updatesqa.apk")
        validator.validate(newApk, entry(b, 2))
        packages.installPrivate(newApk, b, 2)
        check(sources(b).size == 2)
        check(packages.systemPackage(b) == null) { "Private install registered an Android app" }
        check(packages.uninstallPrivate(b))
        host.clearCache(b)
        check(packages.packageInfo(b) == null && host.loadSources(b).isEmpty())
        check(settings.getString("quality", "") == "standard")
        check(app.container.database.sourceBindingDao().bindingForMedia(binding.mediaId) == binding)
        check(app.container.database.progressDao().latestProgress(binding.mediaId) == progress)
        println("PASS: private APK install/update/remove, Android migration, stable source IDs/settings/binding, trust gate, immutable read-only code, invalid APK/signer/downgrade rejected")
    } else {
        check(packages.systemPackage(a) == null)
        check(packages.isPrivate(a) && !descriptor(a).hasSystemCopy)
        check(sources(a).size == 2)
        check(app.container.database.sourceBindingDao().bindingForMedia(binding.mediaId) == binding)
        check(app.container.database.progressDao().latestProgress(binding.mediaId) == progress)
        // The original full preferences contract now runs on an APK absent from PackageManager.
        checkApkSourcePreferences(a)
        check(packages.uninstallPrivate(a))
        host.clearCache(a)
        check(host.loadSources(a).isEmpty())
        check(settings.contains("quality"))
        check(app.container.database.sourceBindingDao().bindingForMedia(binding.mediaId) == binding)
        check(app.container.database.progressDao().latestProgress(binding.mediaId) == progress)
        println("PASS: migrated APK works after Android removal; native settings/backup/restore; internal removal retains source settings and binding")
    }
}
