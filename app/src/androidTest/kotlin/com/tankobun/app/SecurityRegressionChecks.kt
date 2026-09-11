package com.tankobun.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tankobun.app.updates.AppUpdateApkValidator
import com.tankobun.app.updates.AppUpdateInfo
import com.tankobun.core.network.sha256
import com.tankobun.core.network.readBytesCancellable
import java.io.File

/** Isolated app only; exercises the real Android keystore and archive signature parser. */
internal fun checkSecurityRegressions(context: Context): String {
    check(context.packageName.endsWith(".novelqa"))
    val secureName = "tankobun_tokens"
    val legacyName = "tankobun_tokens_fallback"
    val markersName = "tankobun_token_revocations"
    val anilistKey = "anilist.access.token"
    val mangaKey = "mangabaka.token"
    val aniToken = "fictional-anilist-security-regression-token"
    val mangaToken = "fictional-mangabaka-security-regression-token"
    fun openSecure(ctx: Context): SharedPreferences {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(ctx, secureName, key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
    }
    fun legacy() = context.applicationContext.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
    fun assertNoPlaintext() {
        File(context.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty().filter { it.isFile }.forEach { file ->
            val contents = file.readText()
            check(!contents.contains(aniToken) && !contents.contains(mangaToken)) { "Plaintext credential in ${file.name}" }
        }
    }
    listOf(secureName, legacyName, markersName).forEach(context::deleteSharedPreferences)
    try {
        var attempts = 0
        var resets = 0
        legacy().edit().putString(anilistKey, aniToken).putString(mangaKey, mangaToken).commit()
        val unavailable = SecureTokenStore(context, { attempts++; error("Simulated keystore failure") }, { resets++ })
        check(attempts == 2 && resets == 1)
        check(!unavailable.saveAccessToken(aniToken))
        check(!unavailable.saveMangaBaka(mangaToken, "fiction", "Fiction"))
        check(unavailable.accessToken() == null && unavailable.mangaBakaToken() == null)
        assertNoPlaintext()
        val recreationFailure = SecureTokenStore(context, { error("Simulated key failure") }, { error("Simulated reset failure") })
        check(!recreationFailure.saveAccessToken(aniToken))
        assertNoPlaintext()

        val secure = SecureTokenStore(context)
        check(secure.saveAccessToken(aniToken) && secure.saveMangaBaka(mangaToken, "fiction", "Fiction"))
        assertNoPlaintext()
        // Recovery purges the inactive legacy store without discarding secure sessions.
        legacy().edit().putString(anilistKey, aniToken).putString(mangaKey, mangaToken).commit()
        val recovered = SecureTokenStore(context)
        check(recovered.accessToken() == aniToken && recovered.mangaBakaToken() == mangaToken)
        assertNoPlaintext()

        legacy().edit().putString(anilistKey, aniToken).putString(mangaKey, mangaToken).commit()
        recovered.clear()
        check(recovered.accessToken() == null && recovered.mangaBakaToken() == mangaToken)
        check(!legacy().contains(anilistKey) && legacy().getString(mangaKey, null) == mangaToken) {
            "Provider isolation failed: AniList present=${legacy().contains(anilistKey)}, MangaBaka preserved=${legacy().getString(mangaKey, null) == mangaToken}"
        }
        check(!openSecure(context).contains(anilistKey) && openSecure(context).getString(mangaKey, null) == mangaToken)
        recovered.clearMangaBaka()
        assertNoPlaintext()

        check(recovered.saveAccessToken(aniToken) && recovered.saveMangaBaka(mangaToken, "fiction", "Fiction"))
        // Leave the real secure data intact while simulating an outage; logout must
        // suppress and clear only AniList once the keystore is available again.
        val outage = SecureTokenStore(context, { error("Simulated outage") }, {})
        outage.clear()
        val afterOutage = SecureTokenStore(context)
        check(afterOutage.accessToken() == null && afterOutage.mangaBakaToken() == mangaToken)
        check(!openSecure(context).contains(anilistKey))
        afterOutage.clearMangaBaka()
        check(afterOutage.saveAccessToken(aniToken) && afterOutage.saveMangaBaka(mangaToken, "fiction", "Fiction"))
        val mangaOutage = SecureTokenStore(context, { error("Simulated outage") }, {})
        mangaOutage.clearMangaBaka()
        val finalStore = SecureTokenStore(context)
        check(finalStore.accessToken() == aniToken && finalStore.mangaBakaToken() == null)
        check(!openSecure(context).contains(mangaKey))
        finalStore.clear()
        assertNoPlaintext()

        val pipe = android.os.ParcelFileDescriptor.createPipe()
        val pipeInput = android.os.ParcelFileDescriptor.AutoCloseInputStream(pipe[0])
        try {
            val started = android.os.SystemClock.elapsedRealtime()
            val failure = runCatching {
                kotlinx.coroutines.runBlocking { pipeInput.readBytesCancellable(16, 150) }
            }
            check(failure.exceptionOrNull() is kotlinx.coroutines.TimeoutCancellationException)
            check(android.os.SystemClock.elapsedRealtime() - started < 2_000) { "Provider pipe was not cancelled promptly" }
            check(!pipe[0].fileDescriptor.valid()) { "Provider pipe was not closed" }
        } finally { pipeInput.close(); pipe[1].close() }

        val directory = File(context.filesDir, "security-fixtures")
        val validator = AppUpdateApkValidator(context)
        fun update(file: File) = AppUpdateInfo(50, "4.2.2", "https://example.invalid/update.apk", file.sha256(), null, null, file.length(), false, emptyMap())
        for (name in listOf("valid", "rotated")) {
            val file = File(directory, "$name.apk")
            check(file.isFile) { "Missing fixture: $name" }
            validator.validate(file, update(file))
        }
        for (name in listOf("wrong-package", "downgrade", "wrong-version", "wrong-name", "wrong-signer", "unsigned", "tampered", "multiple-signers")) {
            val file = File(directory, "$name.apk")
            check(file.isFile) { "Missing fixture: $name" }
            check(runCatching { validator.validate(file, update(file)) }.isFailure) { "Invalid archive accepted: $name" }
        }
        return "PASS: secure-store open/reset failures, plaintext absence, recovery, provider-isolated logout and deferred revocation; bounded/cancellable provider pipe; real APK package/version/signature/integrity rejection; valid same-signer and forward-rotation APKs"
    } finally {
        listOf(secureName, legacyName, markersName).forEach(context::deleteSharedPreferences)
    }
}
