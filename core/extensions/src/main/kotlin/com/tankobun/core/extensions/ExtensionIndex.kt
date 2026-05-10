package com.tankobun.core.extensions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtensionIndexEntry(
    val name: String,
    @SerialName("pkg") val packageName: String,
    @SerialName("apk") val apkName: String,
    val lang: String,
    @SerialName("code") val versionCode: Int,
    @SerialName("version") val versionName: String,
    @SerialName("nsfw") val nsfwFlag: Int = 0,
    val sources: List<ExtensionIndexSource> = emptyList(),
) {
    val isNsfw: Boolean get() = nsfwFlag == 1
}

@Serializable
data class ExtensionIndexSource(
    val name: String,
    val lang: String? = null,
    val id: Long? = null,
)
