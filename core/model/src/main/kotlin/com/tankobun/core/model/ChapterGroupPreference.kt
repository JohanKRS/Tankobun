package com.tankobun.core.model

@kotlinx.serialization.Serializable
data class ChapterGroupPreference(
    val oneVersionPerChapter: Boolean = false,
    val preferredGroup: String? = null,
)
