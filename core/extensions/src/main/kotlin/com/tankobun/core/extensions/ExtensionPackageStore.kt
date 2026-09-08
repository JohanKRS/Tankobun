package com.tankobun.core.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/** Community APKs can live inside the reader, without becoming Android applications. */
class ExtensionPackageStore(context: Context) {
    private val manager = context.applicationContext.packageManager
    private val directory = File(context.applicationContext.noBackupFilesDir, "extension_apks")

    fun systemPackage(packageName: String): PackageInfo? = runCatching {
        @Suppress("DEPRECATION")
        manager.getPackageInfo(packageName, EXTENSION_PACKAGE_FLAGS).takeIf { it.isExtensionPackage() }
    }.getOrNull()

    fun packageInfo(packageName: String): PackageInfo? = synchronized(lock) {
        selectExtensionPackage(systemPackage(packageName), privatePackage(packageName))
    }

    fun installedPackages(): List<PackageInfo> = synchronized(lock) {
        @Suppress("DEPRECATION")
        val shared = manager.getInstalledPackages(EXTENSION_PACKAGE_FLAGS).filter { it.isExtensionPackage() }
        val private = directory.listFiles().orEmpty().filter { it.isDirectory && validExtensionStoragePackage(it.name) }
            .mapNotNull { privatePackage(it.name) }
        (shared.map { it.packageName } + private.map { it.packageName }).distinct().mapNotNull { name ->
            selectExtensionPackage(shared.firstOrNull { it.packageName == name }, private.firstOrNull { it.packageName == name })
        }
    }

    fun isPrivate(packageName: String): Boolean = packageInfo(packageName)?.applicationInfo?.sourceDir
        ?.startsWith(directory.absolutePath + File.separator) == true

    fun icon(packageName: String): Drawable? = packageInfo(packageName)?.applicationInfo?.loadIcon(manager)

    /** Caller validates repository identity and signature before storing. No extension code runs here. */
    fun installPrivate(apk: File, packageName: String, versionCode: Int) = synchronized(lock) {
        require(validExtensionStoragePackage(packageName))
        val archive = archiveInfo(apk) ?: error("Invalid extension archive")
        require(archive.packageName == packageName && archive.longVersionCode == versionCode.toLong())
        val current = packageInfo(packageName)
        require(current == null || archive.longVersionCode >= current.longVersionCode) { "Extension downgrade rejected" }
        val signatures = archive.extensionSignerHistory()
        require(signatures.isNotEmpty() && (current == null || signatures.containsAll(current.extensionSignerFingerprints()))) {
            "Extension signature mismatch"
        }
        val targetDir = File(directory, packageName).apply { mkdirs() }
        val temp = File.createTempFile("incoming-", ".tmp", targetDir)
        try {
            val hash = MessageDigest.getInstance("SHA-256")
            FileOutputStream(temp).use { output ->
                // Android 14 requires dynamically loaded code to be read-only before writing/loading.
                check(temp.setReadOnly())
                apk.inputStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        hash.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                }
                output.fd.sync()
            }
            val digest = hash.digest().joinToString("") { "%02x".format(it) }
            val target = File(targetDir, "$versionCode-$digest.apk")
            if (target.isFile) temp.delete() else check(temp.renameTo(target))
            val pointer = AtomicFile(File(targetDir, "active"))
            val output = pointer.startWrite()
            try {
                output.write(target.name.toByteArray())
                pointer.finishWrite(output)
            } catch (error: Throwable) { pointer.failWrite(output); throw error }
            // Immutable paths keep the current class loader safe while the new version is committed.
            // Keep the previous version until the next update, in case a reader still references it.
            val previous = current?.applicationInfo?.sourceDir
            targetDir.listFiles().orEmpty().filter { it.extension == "apk" && it != target && it.absolutePath != previous }
                .forEach { verifiedArchives.remove(it.absolutePath); it.delete() }
        } finally { temp.delete() }
    }

    fun uninstallPrivate(packageName: String): Boolean = synchronized(lock) {
        if (!validExtensionStoragePackage(packageName)) return false
        val target = File(directory, packageName)
        if (!target.exists()) return false
        verifiedArchives.keys.removeAll { it.startsWith(target.absolutePath + File.separator) }
        // Preferences, source bindings, reading history and downloaded chapters live elsewhere.
        target.deleteRecursively()
    }

    private fun privatePackage(packageName: String): PackageInfo? {
        if (!validExtensionStoragePackage(packageName)) return null
        return runCatching {
            val targetDir = File(directory, packageName)
            val filename = AtomicFile(File(targetDir, "active")).openRead().bufferedReader().use { it.readText() }
            require(filename.matches(Regex("[0-9]+-[a-f0-9]{64}\\.apk")))
            val apk = File(targetDir, filename)
            check(apk.isFile && !apk.canWrite())
            val stamp = apk.lastModified() to apk.length()
            verifiedArchives[apk.absolutePath]?.takeIf { it.stamp == stamp }?.let { return@runCatching it.info }
            archiveInfo(apk)?.takeIf { it.packageName == packageName }?.apply {
                lastUpdateTime = apk.lastModified()
                applicationInfo?.sourceDir = apk.absolutePath
                applicationInfo?.publicSourceDir = apk.absolutePath
                verifiedArchives[apk.absolutePath] = VerifiedArchive(stamp, this)
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(apk: File): PackageInfo? = manager.getPackageArchiveInfo(apk.absolutePath, EXTENSION_PACKAGE_FLAGS)
        ?.takeIf { it.isExtensionPackage() }

    private data class VerifiedArchive(val stamp: Pair<Long, Long>, val info: PackageInfo)
    companion object {
        private val lock = Any()
        private val verifiedArchives = mutableMapOf<String, VerifiedArchive>()
    }
}

internal fun validExtensionStoragePackage(name: String): Boolean =
    isSupportedExtensionPackageName(name) && name.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))

internal fun selectExtensionPackage(shared: PackageInfo?, private: PackageInfo?): PackageInfo? = when {
    shared == null -> private
    private == null -> shared
    preferPrivateExtension(shared.longVersionCode, private.longVersionCode, shared.extensionSignerFingerprints(), private.extensionSignerHistory()) -> private
    else -> shared
}

internal fun preferPrivateExtension(sharedVersion: Long, privateVersion: Long, sharedSigners: Set<String>, privateSignerHistory: Set<String>): Boolean =
    privateVersion >= sharedVersion && sharedSigners.isNotEmpty() && privateSignerHistory.containsAll(sharedSigners)
