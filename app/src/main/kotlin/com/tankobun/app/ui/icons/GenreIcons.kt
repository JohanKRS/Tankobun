package com.tankobun.app.ui.icons

import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

internal fun genreIcon(name: String): ImageVector =
    when (name.trim().lowercase(Locale.ROOT).replace('_', ' ').replace('-', ' ')) {
        "action" -> TankobunIcons.GenreAction
        "adventure" -> TankobunIcons.GenreAdventure
        "comedy" -> TankobunIcons.GenreComedy
        "drama" -> TankobunIcons.GenreDrama
        "ecchi" -> TankobunIcons.GenreEcchi
        "fantasy" -> TankobunIcons.GenreFantasy
        "hentai" -> TankobunIcons.GenreHentai
        "horror" -> TankobunIcons.GenreHorror
        "mahou shoujo" -> TankobunIcons.GenreMahouShoujo
        "mecha" -> TankobunIcons.GenreMecha
        "music" -> TankobunIcons.GenreMusic
        "mystery" -> TankobunIcons.GenreMystery
        "psychological" -> TankobunIcons.GenrePsychological
        "romance" -> TankobunIcons.GenreRomance
        "sci fi", "science fiction" -> TankobunIcons.GenreSciFi
        "slice of life" -> TankobunIcons.GenreSliceOfLife
        "sports" -> TankobunIcons.GenreSports
        "supernatural" -> TankobunIcons.GenreSupernatural
        "thriller" -> TankobunIcons.GenreThriller
        else -> TankobunIcons.Category
    }
