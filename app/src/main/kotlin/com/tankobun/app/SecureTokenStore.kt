package com.tankobun.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

class SecureTokenStore internal constructor(
    context: Context,
    openSecure: (Context) -> SharedPreferences,
    resetSecure: (Context) -> Unit,
) {
    constructor(context: Context) : this(context, ::createSecurePreferences, ::resetSecurePreferences)

    private val appContext = context.applicationContext
    // Only revocation flags live here, never tokens or account data. A logout while
    // the keystore is unavailable must still take effect when it recovers.
    private val revocations = appContext.getSharedPreferences("tankobun_token_revocations", Context.MODE_PRIVATE)
    private val blockedProviders = mutableSetOf<String>()
    private val preferences: SharedPreferences? = runCatching { openSecure(appContext) }
        .recoverCatching {
            Log.w(TAG, "Encrypted token storage could not be opened; resetting local auth storage.")
            resetSecure(appContext)
            openSecure(appContext)
        }.getOrElse {
            Log.e(TAG, "Encrypted token storage is unavailable; credential persistence is disabled.")
            null
        }

    init {
        // Plaintext credentials are never read or migrated back into an active session.
        val legacy = appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        if (legacy.edit().clear().commit()) appContext.deleteSharedPreferences(FALLBACK_PREFS_NAME)
        listOf(ANILIST, MANGABAKA).filter { revocations.getBoolean(it, false) }.forEach(::clearProvider)
    }

    @Synchronized fun accessToken(): String? = read(ANILIST, KEY_ACCESS_TOKEN)
    @Synchronized fun mangaBakaToken(): String? = read(MANGABAKA, "mangabaka.token")
    @Synchronized fun mangaBakaAccount(): String? = read(MANGABAKA, "mangabaka.account")
    @Synchronized fun mangaBakaName(): String? = read(MANGABAKA, "mangabaka.name")

    @Synchronized fun saveAccessToken(token: String): Boolean = save(ANILIST, mapOf(KEY_ACCESS_TOKEN to token))
    @Synchronized fun saveMangaBaka(token: String, account: String, name: String): Boolean =
        save(MANGABAKA, mapOf("mangabaka.token" to token, "mangabaka.account" to account, "mangabaka.name" to name))

    @Synchronized fun clear() = clearProvider(ANILIST)
    @Synchronized fun clearMangaBaka() = clearProvider(MANGABAKA)

    private fun read(provider: String, key: String): String? {
        if (provider in blockedProviders || revocations.getBoolean(provider, false)) return null
        return runCatching { preferences?.getString(key, null) }.getOrElse {
            blockedProviders.add(provider)
            null
        }
    }

    private fun save(provider: String, values: Map<String, String>): Boolean {
        val secure = preferences ?: return false
        val saved = runCatching {
            val editor = secure.edit()
            values.forEach { (key, value) -> editor.putString(key, value) }
            editor.commit() && revocations.edit().remove(provider).commit()
        }.getOrDefault(false)
        if (saved) blockedProviders.remove(provider) else clearProvider(provider)
        return saved
    }

    private fun clearProvider(provider: String) {
        blockedProviders.add(provider)
        val revoked = revocations.edit().putBoolean(provider, true).commit()
        val keys = if (provider == ANILIST) listOf(KEY_ACCESS_TOKEN) else listOf("mangabaka.token", "mangabaka.account", "mangabaka.name")
        fun removeFrom(store: SharedPreferences): Boolean = store.edit().let { editor -> keys.forEach(editor::remove); editor.commit() }
        val legacyCleared = removeFrom(appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE))
        val secureCleared = preferences?.let { runCatching { removeFrom(it) }.getOrDefault(false) } == true
        if (legacyCleared && secureCleared) revocations.edit().remove(provider).commit()
        if (!revoked && !secureCleared) Log.e(TAG, "Could not persist credential revocation.")
    }

    private companion object {
        const val TAG = "SecureTokenStore"
        const val PREFS_NAME = "tankobun_tokens"
        const val FALLBACK_PREFS_NAME = "tankobun_tokens_fallback"
        const val KEY_ACCESS_TOKEN = "anilist.access.token"
        const val ANILIST = "anilist"
        const val MANGABAKA = "mangabaka"

        fun createSecurePreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            return EncryptedSharedPreferences.create(context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
        }

        fun resetSecurePreferences(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            context.deleteSharedPreferences(PREFS_NAME)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        }
    }
}
