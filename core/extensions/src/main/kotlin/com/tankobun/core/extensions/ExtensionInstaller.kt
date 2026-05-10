package com.tankobun.core.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

class ExtensionInstaller(
    private val context: Context,
) {
    fun installApk(apkUri: Uri): Intent {
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun openUnknownAppSourcesSettings(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
