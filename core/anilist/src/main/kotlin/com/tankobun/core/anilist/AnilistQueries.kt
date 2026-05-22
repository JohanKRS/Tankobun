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
              mangaList {
                customLists
              }
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
              format
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
              format
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
          ${'$'}scoreFormat: ScoreFormat
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
            score(format: ${'$'}scoreFormat)
            notes
            private
            customLists(asArray: true)
            updatedAt
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
}
