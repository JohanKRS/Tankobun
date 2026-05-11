package com.tankobun.core.anilist

object AnilistQueries {
    const val Viewer = """
        query Viewer {
          Viewer {
            id
            name
            avatar { large }
            mediaListOptions {
              scoreFormat
            }
          }
        }
    """

    const val SearchManga = """
        query SearchManga(${'$'}page: Int!, ${'$'}search: String!) {
          Page(page: ${'$'}page, perPage: 20) {
            pageInfo { hasNextPage currentPage lastPage }
            media(type: MANGA, search: ${'$'}search, sort: SEARCH_MATCH) {
              id
              idMal
              title { romaji english native userPreferred }
              description(asHtml: false)
              coverImage { extraLarge large color }
              bannerImage
              chapters
              volumes
              status
              siteUrl
              genres
              synonyms
              isAdult
              updatedAt
            }
          }
        }
    """

    const val SearchFallbackMangaPage = """
        query SearchFallbackMangaPage(${'$'}page: Int!, ${'$'}sort: [MediaSort]) {
          Page(page: ${'$'}page, perPage: 50) {
            media(type: MANGA, sort: ${'$'}sort) {
              id
              idMal
              title { romaji english native userPreferred }
              description(asHtml: false)
              coverImage { extraLarge large color }
              bannerImage
              chapters
              volumes
              status
              siteUrl
              genres
              synonyms
              isAdult
              updatedAt
            }
          }
        }
    """

    const val MediaDetails = """
        query MediaDetails(${'$'}id: Int!) {
          Media(id: ${'$'}id, type: MANGA) {
            id
            idMal
            title { romaji english native userPreferred }
            description(asHtml: false)
            coverImage { extraLarge large color }
            bannerImage
            chapters
            volumes
            status
            siteUrl
            genres
            synonyms
            isAdult
            updatedAt
            mediaListEntry {
              id
              mediaId
              status
              progress
              score
              notes
              private
              customLists(asArray: true)
              updatedAt
            }
            recommendations(sort: RATING_DESC, perPage: 12) {
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
                  status
                  siteUrl
                  genres
                  synonyms
                  isAdult
                  updatedAt
                }
              }
            }
          }
        }
    """

    const val MangaListCollectionByUserId = """
        query MangaListCollectionByUserId(${'$'}userId: Int!) {
          MediaListCollection(userId: ${'$'}userId, type: MANGA) {
            lists {
              name
              status
              entries {
                id
                mediaId
                status
                progress
                score
                notes
                private
                customLists(asArray: true)
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
                  status
                  siteUrl
                  genres
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
        query MangaListCollectionByUserName(${'$'}userName: String!) {
          MediaListCollection(userName: ${'$'}userName, type: MANGA) {
            lists {
              name
              status
              entries {
                id
                mediaId
                status
                progress
                score
                notes
                private
                customLists(asArray: true)
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
                  status
                  siteUrl
                  genres
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
          ${'$'}customLists: [String]
        ) {
          SaveMediaListEntry(
            mediaId: ${'$'}mediaId,
            status: ${'$'}status,
            progress: ${'$'}progress,
            score: ${'$'}score,
            notes: ${'$'}notes,
            private: ${'$'}private,
            customLists: ${'$'}customLists
          ) {
            id
            mediaId
            status
            progress
            score
            notes
            private
            customLists(asArray: true)
            updatedAt
          }
        }
    """
}
