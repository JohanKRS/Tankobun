package com.tankobun.app

import android.net.Uri
import androidx.core.content.FileProvider
import com.tankobun.app.sharing.RECOMMENDATION_SHARE_EXTENSION
import com.tankobun.app.sharing.RECOMMENDATION_SHARE_MIME_TYPE

class TankobunFileProvider : FileProvider() {
    override fun getType(uri: Uri): String? {
        val extension = ".$RECOMMENDATION_SHARE_EXTENSION"
        return if (
            uri.path.orEmpty().endsWith(extension, ignoreCase = true) ||
            uri.lastPathSegment.orEmpty().endsWith(extension, ignoreCase = true)
        ) {
            RECOMMENDATION_SHARE_MIME_TYPE
        } else {
            super.getType(uri)
        }
    }
}
