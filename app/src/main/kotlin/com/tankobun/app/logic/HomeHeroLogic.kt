package com.tankobun.app.logic

import com.tankobun.core.model.AnilistMedia

internal fun AnilistMedia.mobileHeroCharacterImages(): List<String> {
    return (listOfNotNull(mainCharacterImage) + characterImages)
        .filter(String::isNotBlank)
        .distinct()
        .take(1)
}

internal fun AnilistMedia.tabletHeroCharacterImages(
    landscape: Boolean,
    screenWidthDp: Int,
): List<String> {
    if (!bannerImage.isNullOrBlank()) return emptyList()
    val maximumPanels = when {
        !landscape -> 4
        screenWidthDp >= 1_200 -> 7
        else -> 6
    }
    val images = (characterImages + listOfNotNull(mainCharacterImage))
        .filter(String::isNotBlank)
        .distinct()
        .take(maximumPanels)
    if (images.size < 2) return emptyList()

    val centerOutPositions = buildList {
        val centerLeft = (images.lastIndex) / 2
        add(centerLeft)
        for (distance in 1..images.size) {
            val right = centerLeft + distance
            if (right in images.indices) add(right)
            val left = centerLeft - distance
            if (left in images.indices) add(left)
        }
    }
    return MutableList(images.size) { "" }
        .also { arranged ->
            images.forEachIndexed { priority, image ->
                arranged[centerOutPositions[priority]] = image
            }
        }
}
