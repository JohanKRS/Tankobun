package com.tankobun.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

class SecureTokenStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = securePreferencesOrRecover(appContext)

    fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun saveAccessToken(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val TAG = "SecureTokenStore"
        const val PREFS_NAME = "tankobun_tokens"
        const val FALLBACK_PREFS_NAME = "tankobun_tokens_fallback"
        const val KEY_ACCESS_TOKEN = "anilist.access.token"

        fun securePreferencesOrRecover(context: Context): SharedPreferences =
            runCatching { createSecurePreferences(context) }
                .recoverCatching { failure ->
                    Log.w(TAG, "Encrypted token storage could not be opened; resetting local auth storage.", failure)
                    resetSecurePreferences(context)
                    createSecurePreferences(context)
                }
                .getOrElse { failure ->
                    Log.e(TAG, "Encrypted token storage is unavailable; using a local fallback token store.", failure)
                    context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
                }

        fun createSecurePreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        fun resetSecurePreferences(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            context.deleteSharedPreferences(PREFS_NAME)
            runCatching {
                val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                    keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                }
            }.onFailure { failure ->
                Log.w(TAG, "Could not delete the stale encrypted preferences master key.", failure)
            }
        }
    }
}
