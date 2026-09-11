package com.tankobun.core.extensions

import android.content.Context
import com.tankobun.core.network.UnsafeDistributionException
import com.tankobun.core.network.normalizedSha256
import com.tankobun.core.network.requireDistributionUrl

data class RepositoryIdentityChange(
    val repositoryUrl: String,
    val resolvedUrl: String,
    val previousIdentities: Map<String, String?>,
    val newIdentity: String,
) {
    val previousIdentity: String get() = previousIdentities.values.first { it != null && it != newIdentity }!!
}

class RepositoryIdentityChangedException(val change: RepositoryIdentityChange) :
    IllegalStateException("Repository signing identity changed; review required")

/** Trust survives URL migration, key removal, and removing/re-adding a repository. */
class RepositoryTrustStore internal constructor(
    private val read: (String) -> String?,
    private val write: (Map<String, String>) -> Boolean,
) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("extension_repository_trust", Context.MODE_PRIVATE).let { prefs ->
            { url: String -> prefs.getString(url, null) }
        },
        context.applicationContext.getSharedPreferences("extension_repository_trust", Context.MODE_PRIVATE).let { prefs ->
            { values: Map<String, String> -> prefs.edit().let { editor -> values.forEach { (url, identity) -> editor.putString(url, identity) }; editor.commit() } }
        },
    )

    @Synchronized fun checkAndRemember(requestedUrl: String, result: ExtensionIndexResult) {
        val aliases = listOf(requestedUrl, result.resolvedIndexUrl).map { requireDistributionUrl(it).toString() }.distinct()
        val identity = repositoryIdentity(result.repositorySigningKey)
        val previous = aliases.associateWith(read)
        if (previous.values.any { it != null && it != identity }) {
            check(write(aliases.associate { "blocked:$it" to identity })) { "Could not block changed repository" }
            throw RepositoryIdentityChangedException(RepositoryIdentityChange(aliases.first(), aliases.last(), previous, identity))
        }
        val fetchOrigin = if (aliases.size > 1) mapOf("fetch:${aliases.last()}" to aliases.first()) else emptyMap()
        check(write(aliases.associateWith { identity } + aliases.associate { "blocked:$it" to "" } + fetchOrigin)) { "Could not save repository identity" }
    }

    @Synchronized fun fetchUrl(repositoryUrl: String): String {
        val canonical = requireDistributionUrl(repositoryUrl).toString()
        // Refresh through the descriptor which supplied the key, even though the
        // visible list uses its resolved index URL for entry grouping.
        return read("fetch:$canonical")?.let { requireDistributionUrl(it).toString() } ?: canonical
    }

    @Synchronized fun approve(change: RepositoryIdentityChange): Boolean {
        if (change.previousIdentities.any { (url, identity) -> read(url) != identity }) return false
        return write(change.previousIdentities.keys.associateWith { change.newIdentity } + change.previousIdentities.keys.associate { "blocked:$it" to "" })
    }

    @Synchronized fun verifyEntry(entry: ExtensionIndexEntry) {
        val url = requireDistributionUrl(entry.repositoryUrl).toString()
        check(read("blocked:$url").isNullOrEmpty()) { "Repository identity review required before installing extensions" }
        check(read(url) == repositoryIdentity(entry.repositorySigningKey)) { "Refresh and review this repository before installing extensions" }
    }
}

internal fun repositoryIdentity(key: String?): String = key?.takeIf { it.isNotBlank() }?.let {
    val value = it.trim().let { text -> if (text.startsWith("sha256:", ignoreCase = true)) text.substringAfter(':') else text }
    normalizedSha256(value.replace(":", "")) ?: throw UnsafeDistributionException("Invalid repository signing identity")
} ?: "unsigned"
