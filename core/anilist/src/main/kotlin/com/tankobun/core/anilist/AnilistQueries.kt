package com.tankobun.core.anilist

object AnilistQueries {
    const val MediaGenres = """
        query MediaGenres {
          GenreCollection
        }
    """

    val HomeTrending = """
        query HomeTrending(${'$'}isAdult: Boolean) {
          trending: Page(page: 1, perPage: 5) {
            media(type: MANGA, isAdult: ${'$'}isAdult, sort: TRENDING_DESC) {
              ...HomeTrendingMedia
            }
          }
        }
    """ + HOME_TRENDING_MEDIA_FRAGMENT

    fun homeInitial(genres: List<String>, perPage: Int): String =
        """
            query HomeInitial(${'$'}isAdult: Boolean) {
              trending: Page(page: 1, perPage: 5) {
                media(type: MANGA, isAdult: ${'$'}isAdult, sort: TRENDING_DESC) {
                  ...HomeTrendingMedia
                }
              }
              ${homeGenrePageFields(genres, pages = 1..1, perPage = perPage)}
            }
            $HOME_TRENDING_MEDIA_FRAGMENT
        """.trimIndent()

    fun homeGenreCandidates(genres: List<String>, pages: IntRange, perPage: Int): String {
        return """
            query HomeGenreCandidates(${'$'}isAdult: Boolean) {
              ${homeGenrePageFields(genres, pages, perPage)}
            }
        """.trimIndent()
    }

    private fun homeGenrePageFields(genres: List<String>, pages: IntRange, perPage: Int): String =
        genres.flatMapIndexed { index, genre ->
            val safeGenre = genre.replace("\\", "\\\\").replace("\"", "\\\"")
            pages.map { page ->
                """
                  genre${index}Page$page: Page(page: $page, perPage: $perPage) {
                    media(type: MANGA, genre: "$safeGenre", isAdult: ${'$'}isAdult, sort: TRENDING_DESC) {
                      id
                      title { romaji english native userPreferred }
                      coverImage { extraLarge large color }
                      bannerImage
                      popularity
                      genres
                      isAdult
                    }
                  }
                """.trimIndent()
            }
        }.joinToString("\n")

    private const val HOME_TRENDING_MEDIA_FRAGMENT = """

        fragment HomeTrendingMedia on Media {
          id
          idMal
          title { romaji english native userPreferred }
          description(asHtml: false)
          coverImage { extraLarge large color }
          bannerImage
          characters(sort: [FAVOURITES_DESC], page: 1, perPage: 12) {
            edges { role node { image { large } } }
          }
          staff(page: 1, perPage: 8) {
            edges { role node { name { userPreferred } } }
          }
          chapters
          volumes
          format
          countryOfOrigin
          status
          averageScore
          popularity
          startDate { year }
          endDate { year }
          siteUrl
          genres
          synonyms
          isAdult
          updatedAt
        }
    """

    fun homeMainCharacters(mediaIds: List<Int>): String {
        val mediaFields = mediaIds.mapIndexed { index, mediaId ->
            """
              media$index: Media(id: $mediaId, type: MANGA) {
              id
              characters(role: MAIN, sort: [FAVOURITES_DESC], page: 1, perPage: 1) {
                nodes { image { large } }
              }
              }
            """.trimIndent()
        }.joinToString("\n")
        return """
            query HomeMainCharacters {
              $mediaFields
            }
        """.trimIndent()
    }

    const val Viewer = """
        query Viewer {
          Viewer {
            id
            name
            avatar { large }
            bannerImage
            statistics {
              manga {
                count
                chaptersRead
                volumesRead
                meanScore
                genres {
                  genre
                  count
                  chaptersRead
                }
                tags {
                  tag { name }
                  count
                  chaptersRead
                }
                formats {
                  format
                  count
                  chaptersRead
                }
                statuses {
                  status
                  count
                  chaptersRead
                }
              }
            }
            options {
              titleLanguage
            }
            mediaListOptions {
              scoreFormat
              mangaList {
                customLists
              }
            }
          }
        }
    """

    const val SearchManga = """
        query SearchManga(${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}search: String!, ${'$'}isAdult: Boolean) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo { hasNextPage currentPage lastPage }
            media(type: MANGA, search: ${'$'}search, isAdult: ${'$'}isAdult, sort: SEARCH_MATCH) {
              id
              idMal
              title { romaji english native userPreferred }
              description(asHtml: false)
              coverImage { extraLarge large color }
              bannerImage
              chapters
              volumes
              format
              countryOfOrigin
              status
              averageScore
              popularity
              startDate { year }
              endDate { year }
              siteUrl
              genres
              synonyms
              isAdult
              updatedAt
            }
          }
        }
    """

    const val SearchFallbackManga = """
        query SearchFallbackManga(${'$'}isAdult: Boolean) {
          popular: Page(page: 1, perPage: 25) {
            media(type: MANGA, sort: POPULARITY_DESC, isAdult: ${'$'}isAdult) {
              ...SearchFallbackMedia
            }
          }
          trending: Page(page: 1, perPage: 25) {
            media(type: MANGA, sort: TRENDING_DESC, isAdult: ${'$'}isAdult) {
              ...SearchFallbackMedia
            }
          }
        }

        fragment SearchFallbackMedia on Media {
              id
              idMal
              title { romaji english native userPreferred }
              description(asHtml: false)
              coverImage { extraLarge large color }
              bannerImage
              chapters
              volumes
              format
              countryOfOrigin
              status
              averageScore
              popularity
              startDate { year }
              endDate { year }
              siteUrl
              genres
              synonyms
              isAdult
              updatedAt
        }
    """

    const val BrowseLanding = """
        query BrowseLanding(${'$'}perPage: Int!, ${'$'}isAdult: Boolean) {
          trending: Page(page: 1, perPage: ${'$'}perPage) {
            media(type: MANGA, isAdult: ${'$'}isAdult, sort: TRENDING_DESC) {
              ...BrowseLandingMedia
            }
          }
          popular: Page(page: 1, perPage: ${'$'}perPage) {
            media(type: MANGA, isAdult: ${'$'}isAdult, sort: POPULARITY_DESC) {
              ...BrowseLandingMedia
            }
          }
          popularManhwa: Page(page: 1, perPage: ${'$'}perPage) {
            media(type: MANGA, countryOfOrigin: KR, isAdult: ${'$'}isAdult, sort: POPULARITY_DESC) {
              ...BrowseLandingMedia
            }
          }
          topManga: Page(page: 1, perPage: ${'$'}perPage) {
            media(type: MANGA, isAdult: ${'$'}isAdult, sort: SCORE_DESC) {
              ...BrowseLandingMedia
            }
          }
        }

        fragment BrowseLandingMedia on Media {
          id
          idMal
          title { romaji english native userPreferred }
          description(asHtml: false)
          coverImage { extraLarge large color }
          bannerImage
          chapters
          volumes
          format
          countryOfOrigin
          status
          averageScore
          popularity
          startDate { year }
          endDate { year }
          siteUrl
          genres
          synonyms
          isAdult
          updatedAt
        }
    """

    const val MangaById = """
        query MangaById(${'$'}id: Int!) {
          Media(id: ${'$'}id, type: MANGA) {
            id
            idMal
            title { romaji english native userPreferred }
            description(asHtml: false)
            coverImage { extraLarge large color }
            bannerImage
            chapters
            volumes
            format
            countryOfOrigin
            status
            averageScore
            popularity
            startDate { year }
            endDate { year }
            siteUrl
            genres
            synonyms
            isAdult
            updatedAt
          }
        }
    """

    fun mangaByIds(count: Int): String {
        require(count > 0)
        val variables = (0 until count).joinToString(", ") { index ->
            "${'$'}id$index: Int!"
        }
        val mediaFields = (0 until count).joinToString("\n") { index ->
            """
              media$index: Media(id: ${'$'}id$index, type: MANGA) {
                ...BatchMangaMedia
              }
            """.trimIndent()
        }
        return """
            query MangaByIds($variables) {
              $mediaFields
            }

            fragment BatchMangaMedia on Media {
              id
              idMal
              title { romaji english native userPreferred }
              description(asHtml: false)
              coverImage { extraLarge large color }
              bannerImage
              chapters
              volumes
              format
              countryOfOrigin
              status
              averageScore
              popularity
              startDate { year }
              endDate { year }
              siteUrl
              genres
              synonyms
              isAdult
              updatedAt
            }
        """.trimIndent()
    }

    const val MangaByMalId = """
        query MangaByMalId(${'$'}idMal: Int!) {
          Media(idMal: ${'$'}idMal, type: MANGA) {
            id
            idMal
            title { romaji english native userPreferred }
            description(asHtml: false)
            coverImage { extraLarge large color }
            bannerImage
            chapters
            volumes
            format
            countryOfOrigin
            status
            averageScore
            popularity
            startDate { year }
            endDate { year }
            siteUrl
            genres
            synonyms
            isAdult
            updatedAt
          }
        }
    """

    const val MediaTags = """
        query MediaTags {
          MediaTagCollection {
            name
            category
            isAdult
          }
        }
    """

    const val BrowseManga = """
        query BrowseManga(
          ${'$'}page: Int!,
          ${'$'}perPage: Int!,
          ${'$'}search: String,
          ${'$'}genres: [String],
          ${'$'}tags: [String],
          ${'$'}format: MediaFormat,
          ${'$'}status: MediaStatus,
          ${'$'}countryOfOrigin: CountryCode,
          ${'$'}startDateGreater: FuzzyDateInt,
          ${'$'}startDateLesser: FuzzyDateInt,
          ${'$'}isAdult: Boolean,
          ${'$'}sort: [MediaSort]
        ) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            pageInfo { hasNextPage currentPage lastPage }
            media(
              type: MANGA,
              search: ${'$'}search,
              genre_in: ${'$'}genres,
              tag_in: ${'$'}tags,
              format: ${'$'}format,
              status: ${'$'}status,
              countryOfOrigin: ${'$'}countryOfOrigin,
              startDate_greater: ${'$'}startDateGreater,
              startDate_lesser: ${'$'}startDateLesser,
              isAdult: ${'$'}isAdult,
              sort: ${'$'}sort
            ) {
              id
              idMal
              title { romaji english native userPreferred }
              description(asHtml: false)
              coverImage { extraLarge large color }
              bannerImage
              chapters
              volumes
              format
              countryOfOrigin
              status
              averageScore
              popularity
              startDate { year }
              endDate { year }
              siteUrl
              genres
              synonyms
              isAdult
              updatedAt
            }
          }
        }
    """

    const val StaffManga = """
        query StaffManga(${'$'}search: String!, ${'$'}page: Int!, ${'$'}perPage: Int!, ${'$'}sort: [MediaSort]) {
          Staff(search: ${'$'}search) {
            staffMedia(type: MANGA, sort: ${'$'}sort, page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo { hasNextPage currentPage lastPage }
              edges {
                staffRole
                node {
                  id
                  idMal
                  title { romaji english native userPreferred }
                  description(asHtml: false)
                  coverImage { extraLarge large color }
                  bannerImage
                  chapters
                  volumes
                  format
                  countryOfOrigin
                  status
                  averageScore
                  popularity
                  startDate { year }
                  endDate { year }
                  siteUrl
                  genres
                  tags {
                    name
                    rank
                    isMediaSpoiler
                    isGeneralSpoiler
                  }
                  synonyms
                  isAdult
                  updatedAt
                }
              }
            }
          }
        }
    """

    const val MediaDetails = """
        query MediaDetails(${'$'}id: Int!, ${'$'}scoreFormat: ScoreFormat, ${'$'}recommendationsPage: Int!, ${'$'}recommendationsPerPage: Int!) {
          Media(id: ${'$'}id, type: MANGA) {
            id
            idMal
            title { romaji english native userPreferred }
            description(asHtml: false)
            coverImage { extraLarge large color }
            bannerImage
            chapters
            volumes
            format
            countryOfOrigin
            status
            averageScore
            popularity
            startDate { year }
            endDate { year }
            siteUrl
            genres
            synonyms
            isAdult
            updatedAt
            tags {
              name
              rank
              isMediaSpoiler
              isGeneralSpoiler
            }
            staff(sort: RELEVANCE, perPage: 5) {
              edges {
                role
                node {
                  name { userPreferred }
                }
              }
              nodes {
                name { userPreferred }
              }
            }
            mediaListEntry {
              id
              mediaId
              status
              progress
              score(format: ${'$'}scoreFormat)
              notes
              private
              customLists(asArray: true)
              hiddenFromStatusLists
              updatedAt
            }
            recommendations(sort: RATING_DESC, page: ${'$'}recommendationsPage, perPage: ${'$'}recommendationsPerPage) {
              pageInfo {
                currentPage
                hasNextPage
              }
              nodes {
                rating
                mediaRecommendation {
                  id
                  idMal
                  title { romaji english native userPreferred }
                  description(asHtml: false)
                  coverImage { extraLarge large color }
                  bannerImage
                  chapters
                  volumes
                  format
                  countryOfOrigin
                  status
                  averageScore
                  popularity
                  startDate { year }
                  endDate { year }
                  siteUrl
                  genres
                  tags {
                    name
                    rank
                    isMediaSpoiler
                    isGeneralSpoiler
                  }
                  synonyms
                  isAdult
                  updatedAt
                }
              }
            }
          }
        }
    """

    const val MediaRecommendations = """
        query MediaRecommendations(${'$'}id: Int!, ${'$'}page: Int!, ${'$'}perPage: Int!) {
          Media(id: ${'$'}id, type: MANGA) {
            recommendations(sort: RATING_DESC, page: ${'$'}page, perPage: ${'$'}perPage) {
              pageInfo {
                currentPage
                hasNextPage
              }
              nodes {
                rating
                mediaRecommendation {
                  id
                  idMal
                  title { romaji english native userPreferred }
                  description(asHtml: false)
                  coverImage { extraLarge large color }
                  bannerImage
                  chapters
                  volumes
                  format
                  countryOfOrigin
                  status
                  averageScore
                  popularity
                  startDate { year }
                  endDate { year }
                  siteUrl
                  genres
                  tags {
                    name
                    rank
                    isMediaSpoiler
                    isGeneralSpoiler
                  }
                  synonyms
                  isAdult
                  updatedAt
                }
              }
            }
          }
        }
    """

    const val MediaListEntry = """
        query MediaListEntry(${'$'}id: Int!, ${'$'}scoreFormat: ScoreFormat) {
          Media(id: ${'$'}id, type: MANGA) {
            mediaListEntry {
              id
              mediaId
              status
              progress
              score(format: ${'$'}scoreFormat)
              notes
              private
              customLists(asArray: true)
              hiddenFromStatusLists
              updatedAt
            }
          }
        }
    """

    const val MangaListCollectionByUserId = """
        query MangaListCollectionByUserId(${'$'}userId: Int!, ${'$'}scoreFormat: ScoreFormat) {
          MediaListCollection(userId: ${'$'}userId, type: MANGA) {
            lists {
              name
              status
              entries {
                id
                mediaId
                status
                progress
                score(format: ${'$'}scoreFormat)
                notes
                private
                customLists(asArray: true)
                hiddenFromStatusLists
                updatedAt
                media {
                  id
                  idMal
                  title { romaji english native userPreferred }
                  description(asHtml: false)
                  coverImage { extraLarge large color }
                  bannerImage
                  chapters
                  volumes
                  format
                  countryOfOrigin
                  status
                  averageScore
                  popularity
                  startDate { year }
                  endDate { year }
                  siteUrl
                  genres
                  tags {
                    name
                    rank
                    isMediaSpoiler
                    isGeneralSpoiler
                  }
                  synonyms
                  isAdult
                  updatedAt
                }
              }
            }
          }
        }
    """

    const val MangaListCollectionByUserName = """
        query MangaListCollectionByUserName(${'$'}userName: String!, ${'$'}scoreFormat: ScoreFormat) {
          MediaListCollection(userName: ${'$'}userName, type: MANGA) {
            lists {
              name
              status
              entries {
                id
                mediaId
                status
                progress
                score(format: ${'$'}scoreFormat)
                notes
                private
                customLists(asArray: true)
                hiddenFromStatusLists
                updatedAt
                media {
                  id
                  idMal
                  title { romaji english native userPreferred }
                  description(asHtml: false)
                  coverImage { extraLarge large color }
                  bannerImage
                  chapters
                  volumes
                  format
                  countryOfOrigin
                  status
                  averageScore
                  popularity
                  startDate { year }
                  endDate { year }
                  siteUrl
                  genres
                  tags {
                    name
                    rank
                    isMediaSpoiler
                    isGeneralSpoiler
                  }
                  synonyms
                  isAdult
                  updatedAt
                }
              }
            }
          }
        }
    """

    const val SaveMediaListEntry = """
        mutation SaveMediaListEntry(
          ${'$'}mediaId: Int!,
          ${'$'}status: MediaListStatus,
          ${'$'}progress: Int,
          ${'$'}score: Float,
          ${'$'}notes: String,
          ${'$'}private: Boolean,
          ${'$'}customLists: [String],
          ${'$'}hiddenFromStatusLists: Boolean,
          ${'$'}scoreFormat: ScoreFormat
        ) {
          SaveMediaListEntry(
            mediaId: ${'$'}mediaId,
            status: ${'$'}status,
            progress: ${'$'}progress,
            score: ${'$'}score,
            notes: ${'$'}notes,
            private: ${'$'}private,
            customLists: ${'$'}customLists,
            hiddenFromStatusLists: ${'$'}hiddenFromStatusLists
          ) {
            id
            mediaId
            status
            progress
            score(format: ${'$'}scoreFormat)
            notes
            private
            customLists(asArray: true)
            hiddenFromStatusLists
            updatedAt
          }
        }
    """

    const val DeleteMediaListEntry = """
        mutation DeleteMediaListEntry(${'$'}id: Int!) {
          DeleteMediaListEntry(id: ${'$'}id) {
            deleted
          }
        }
    """

    const val UpdateMangaCustomLists = """
        mutation UpdateMangaCustomLists(${'$'}customLists: [String]) {
          UpdateUser(mangaListOptions: { customLists: ${'$'}customLists }) {
            mediaListOptions {
              mangaList {
                customLists
              }
            }
          }
        }
    """

    const val UpdateUserPreferences = """
        mutation UpdateUserPreferences(${'$'}titleLanguage: UserTitleLanguage, ${'$'}scoreFormat: ScoreFormat) {
          UpdateUser(titleLanguage: ${'$'}titleLanguage, scoreFormat: ${'$'}scoreFormat) {
            id
            name
            avatar { large }
            bannerImage
            statistics {
              manga {
                count
                chaptersRead
                volumesRead
                meanScore
                genres {
                  genre
                  count
                  chaptersRead
                }
                tags {
                  tag { name }
                  count
                  chaptersRead
                }
                formats {
                  format
                  count
                  chaptersRead
                }
                statuses {
                  status
                  count
                  chaptersRead
                }
              }
            }
            options {
              titleLanguage
            }
            mediaListOptions {
              scoreFormat
              mangaList {
                customLists
              }
            }
          }
        }
    """
}
