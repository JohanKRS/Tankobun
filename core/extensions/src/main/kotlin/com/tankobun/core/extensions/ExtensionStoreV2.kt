@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tankobun.core.extensions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
internal data class ExtensionRepositoryDescriptor(
    @SerialName("index_v2") val indexV2: String? = null,
    val meta: Meta? = null,
) {
    @Serializable
    data class Meta(
        val signingKeyFingerprint: String? = null,
    )
}

@Serializable
internal data class ExtensionStoreV2(
    @ProtoNumber(1) val name: String = "",
    @ProtoNumber(2) val badgeLabel: String = "",
    @ProtoNumber(3) val signingKey: String = "",
    @ProtoNumber(4) val contact: Contact = Contact(),
    @ProtoNumber(101) val extensionList: ExtensionList? = null,
    @ProtoNumber(102) val extensionListUrl: String? = null,
) {
    @Serializable
    data class Contact(
        @ProtoNumber(1) val website: String = "",
        @ProtoNumber(2) val discord: String? = null,
    )

    @Serializable
    data class ExtensionList(
        @ProtoNumber(1) val extensions: List<Extension> = emptyList(),
    )

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String = "",
        @ProtoNumber(2) val packageName: String = "",
        @ProtoNumber(3) val resources: Resources = Resources(),
        @ProtoNumber(4) val extensionLib: String = "",
        @ProtoNumber(5) val versionCode: Long = 0L,
        @ProtoNumber(6) val versionName: String = "",
        @ProtoNumber(7) val contentWarning: ContentWarning = ContentWarning.UNSPECIFIED,
        @ProtoNumber(8) val sources: List<Source> = emptyList(),
    )

    @Serializable
    data class Resources(
        @ProtoNumber(1) val apkUrl: String = "",
        @ProtoNumber(2) val iconUrl: String = "",
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long = 0L,
        @ProtoNumber(2) val name: String = "",
        @ProtoNumber(3) val language: String = "",
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        @ProtoNumber(7) val message: String? = null,
    )

    @Serializable
    internal enum class ContentWarning {
        @ProtoNumber(0)
        @SerialName("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        @ProtoNumber(1)
        @SerialName("CONTENT_WARNING_SAFE")
        SAFE,

        @ProtoNumber(2)
        @SerialName("CONTENT_WARNING_MIXED")
        MIXED,

        @ProtoNumber(3)
        @SerialName("CONTENT_WARNING_NSFW")
        NSFW,
    }
}

internal fun ExtensionStoreV2.ExtensionList.toIndexEntries(
    repositorySigningKey: String? = null,
): List<ExtensionIndexEntry> =
    extensions.map { extension ->
        val languages = extension.sources
            .asSequence()
            .map { it.language }
            .filter { it.isNotBlank() }
            .toSet()
        ExtensionIndexEntry(
            name = extension.name,
            packageName = extension.packageName,
            apkName = extension.resources.apkUrl,
            lang = languages.singleOrNull() ?: "all",
            versionCode = extension.versionCode.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(),
            versionName = extension.versionName,
            nsfwFlag = if (extension.contentWarning >= ExtensionStoreV2.ContentWarning.MIXED) 1 else 0,
            sources = extension.sources.map { source ->
                ExtensionIndexSource(
                    name = source.name,
                    lang = source.language,
                    id = source.id,
                )
            },
            iconUrl = extension.resources.iconUrl.takeIf { it.isNotBlank() },
            repositorySigningKey = repositorySigningKey?.takeIf { it.isNotBlank() },
        )
    }
