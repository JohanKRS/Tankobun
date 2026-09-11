package com.tankobun.core.extensions

import org.junit.Assert.*
import org.junit.Test

class RepositoryTrustStoreTest {
    private val saved = mutableMapOf<String, String>()
    private val store = RepositoryTrustStore(saved::get) { saved.putAll(it); true }
    private val oldUrl = "https://repo.test/index.min.json"
    private val newUrl = "https://repo.test/index.pb"
    private val key = "a".repeat(64)
    private fun result(signingKey: String?) = ExtensionIndexResult(emptyList(), newUrl, signingKey)
    private fun entry(signingKey: String?) = ExtensionIndexEntry("Example", "pkg.example", "example.apk", "en", 2, "2", repositorySigningKey = signingKey, repositoryUrl = newUrl)

    @Test fun preservesIdentityAcrossAliasMigrationAndEmptyIndexes() {
        store.checkAndRemember(oldUrl, result(key))
        assertEquals(key, saved[oldUrl]); assertEquals(key, saved[newUrl])
        store.checkAndRemember(newUrl, result(key.uppercase()))
        store.verifyEntry(entry(key))
    }

    @Test fun changingOrRemovingAKeyBlocksStaleEntriesUntilReviewed() {
        for (replacement in listOf("b".repeat(64), null)) {
            saved.clear(); store.checkAndRemember(oldUrl, result(key))
            val error = assertThrows(RepositoryIdentityChangedException::class.java) { store.checkAndRemember(newUrl, result(replacement)) }
            assertThrows(IllegalStateException::class.java) { store.verifyEntry(entry(key)) }
            assertTrue(store.approve(error.change))
            store.checkAndRemember(newUrl, result(replacement))
            store.verifyEntry(entry(replacement))
            assertThrows(IllegalStateException::class.java) { store.verifyEntry(entry(key)) }
        }
    }

    @Test fun unsignedFirstUseStillRequiresReviewBeforeAddingAKey() {
        store.checkAndRemember(oldUrl, result(null))
        store.verifyEntry(entry(null))
        assertThrows(RepositoryIdentityChangedException::class.java) { store.checkAndRemember(oldUrl, result(key)) }
    }

    @Test fun newAliasCannotOverridePreviouslyPinnedResolvedIdentity() {
        store.checkAndRemember(oldUrl, result(key))
        assertThrows(RepositoryIdentityChangedException::class.java) {
            store.checkAndRemember("https://other.test/repo.json", result("b".repeat(64)))
        }
    }

    @Test fun unrefreshedBackupEntriesCannotInstall() {
        assertThrows(IllegalStateException::class.java) { store.verifyEntry(entry(null)) }
    }
}
