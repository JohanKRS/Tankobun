package com.tankobun.app.logic

import kotlin.math.floor

/** Grid width, in dp, that the "covers per row" setting describes: a phone held upright. */
internal const val COVER_GRID_REFERENCE_WIDTH_DP = 400f

/** Narrowest cover, in dp, that the extra columns of a wider grid may shrink covers to. */
internal const val MIN_ADAPTIVE_COVER_WIDTH_DP = 136f

/**
 * Columns for a cover grid [availableWidthDp] wide with [spacingDp] between covers.
 *
 * [preferredColumns] is the user's "covers per row" for a phone-sized grid about
 * [COVER_GRID_REFERENCE_WIDTH_DP] wide, so phones show exactly that. Wider grids, such as tablets
 * and phones in landscape, add columns in proportion to their width while every cover stays at
 * least [MIN_ADAPTIVE_COVER_WIDTH_DP] wide, which turns the extra room into more covers instead of
 * a few huge ones. A grid never shows fewer columns than the user chose.
 */
internal fun adaptiveCoverColumns(
    preferredColumns: Int,
    availableWidthDp: Float,
    spacingDp: Float,
): Int {
    val preferred = preferredColumns.coerceAtLeast(1)
    if (availableWidthDp <= COVER_GRID_REFERENCE_WIDTH_DP) return preferred
    val proportional = floor(preferred * availableWidthDp / COVER_GRID_REFERENCE_WIDTH_DP).toInt()
    val widestFitting = floor((availableWidthDp + spacingDp) / (MIN_ADAPTIVE_COVER_WIDTH_DP + spacingDp)).toInt()
    return maxOf(preferred, minOf(proportional, widestFitting))
}
