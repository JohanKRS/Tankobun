package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistScoreFormat
import com.tankobun.core.model.AnilistMangaStats
import com.tankobun.core.model.AnilistTitleLanguage

data class AnilistViewer(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    val bannerImageUrl: String?,
    val mangaStats: AnilistMangaStats?,
    val scoreFormat: AnilistScoreFormat,
    val titleLanguage: AnilistTitleLanguage,
    val mangaCustomLists: List<String>,
)
