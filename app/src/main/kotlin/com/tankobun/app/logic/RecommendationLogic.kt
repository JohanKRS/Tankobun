package com.tankobun.app.logic

import com.tankobun.core.model.AnilistRecommendation

internal const val RECOMMENDATIONS_PAGE_SIZE = 18

internal fun List<AnilistRecommendation>.recommendationPageCount(): Int =
    if (isEmpty()) 0 else ((size - 1) / RECOMMENDATIONS_PAGE_SIZE) + 1

