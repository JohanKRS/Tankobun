package com.tankobun.core.anilist

class IncompleteAniListLibraryException(cause: Throwable? = null) :
    IllegalArgumentException("AniList returned an incomplete library", cause)
