package com.tankobun.core.anilist

import com.tankobun.core.model.AnilistScoreFormat

data class AnilistViewer(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    val scoreFormat: AnilistScoreFormat,
    val mangaCustomLists: List<String>,
)
