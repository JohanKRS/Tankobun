package com.tankobun.core.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import com.tankobun.core.model.SourceDescriptor
import kotlin.math.absoluteValue

class InstalledExtensionScanner(
    private val context: Context,
) {
    fun installedExtensions(): List<SourceDescriptor> {
        val packageManager = context.packageManager
        @Suppress("DEPRECATION")
        return packageManager.getInstalledPackages(EXTENSION_PACKAGE_FLAGS)
            .filter { it.isExtensionPackage() }
            .map { pkg ->
                val appInfo = pkg.applicationInfo
                val label = appInfo?.loadLabel(packageManager)?.toString()
                val lang = appInfo?.metaData?.getString("tachiyomi.extension.lang")
                    ?: extensionLanguageFromPackage(pkg.packageName)
                SourceDescriptor(
                    id = stableSourceId(pkg.packageName, label.orEmpty(), lang),
                    name = label ?: pkg.packageName.substringAfterLast('.'),
                    lang = lang,
                    packageName = pkg.packageName,
                    versionName = pkg.versionName,
                    versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                        pkg.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        pkg.versionCode
                    },
                    isNsfw = appInfo?.metaData?.extensionBoolean("tachiyomi.extension.nsfw") ?: false,
                    installed = true,
                )
            }
            .sortedWith(compareBy<SourceDescriptor> { it.lang }.thenBy { it.name })
    }

    private fun stableSourceId(packageName: String, name: String, lang: String): Long {
        return "$packageName:$name:$lang".hashCode().toLong().absoluteValue
    }

    companion object {
        const val TACHIYOMI_EXTENSION_PREFIX = "eu.kanade.tachiyomi.extension"
    }
}

private fun Bundle.extensionBoolean(key: String): Boolean? =
    when (val value = get(key)) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", ignoreCase = true) || value == "1"
        else -> null
    }

internal fun extensionLanguageFromPackage(packageName: String): String {
    val extensionPrefix = "${InstalledExtensionScanner.TACHIYOMI_EXTENSION_PREFIX}."
    return packageName
        .takeIf { it.startsWith(extensionPrefix) }
        ?.removePrefix(extensionPrefix)
        ?.substringBefore('.')
        ?.takeIf { it.isNotBlank() }
        ?: packageName.substringAfterLast('.').take(2)
}
