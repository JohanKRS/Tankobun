package com.tankobun.app.logic

import com.tankobun.core.model.AnilistScoreFormat
import java.util.Locale
import kotlin.math.roundToInt

internal fun String.filteredScoreInput(format: AnilistScoreFormat): String {
    val allowDecimal = format == AnilistScoreFormat.POINT_10_DECIMAL
    var hasDecimal = false
    return buildString {
        this@filteredScoreInput.forEach { char ->
            when {
                char.isDigit() -> append(char)
                allowDecimal && char == '.' && !hasDecimal -> {
                    append(char)
                    hasDecimal = true
                }
            }
        }
    }.take(if (allowDecimal) 4 else 3)
}

internal fun String.toAniListScore(format: AnilistScoreFormat): Double? {
    val value = trim().toDoubleOrNull() ?: return null
    return when (format) {
        AnilistScoreFormat.POINT_100 -> value.coerceIn(0.0, 100.0).roundToInt().toDouble()
        AnilistScoreFormat.POINT_10_DECIMAL -> (value.coerceIn(0.0, 10.0) * 10).roundToInt() / 10.0
        AnilistScoreFormat.POINT_10 -> value.coerceIn(0.0, 10.0).roundToInt().toDouble()
        AnilistScoreFormat.POINT_5 -> value.coerceIn(0.0, 5.0).roundToInt().toDouble()
        AnilistScoreFormat.POINT_3 -> value.coerceIn(0.0, 3.0).roundToInt().toDouble()
    }
}

internal fun Double?.formatTrackingScore(format: AnilistScoreFormat): String {
    val value = this ?: return ""
    return when (format) {
        AnilistScoreFormat.POINT_10_DECIMAL -> "%.1f".format(Locale.US, value)
        AnilistScoreFormat.POINT_100,
        AnilistScoreFormat.POINT_10,
        AnilistScoreFormat.POINT_5,
        AnilistScoreFormat.POINT_3 -> value.roundToInt().toString()
    }
}

