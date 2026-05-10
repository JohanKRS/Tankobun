# Tankobun

Tankobun is a greenfield Android manga reader built around an AniList-first flow:

1. Sign in or search with AniList.
2. Open a manga.
3. Pick the source for that manga from its detail page.
4. Read in paged or webtoon mode, with local caching, downloads, and queued progress sync.

This is a personal/private APK project. It does not affiliate with AniList, Tachiyomi,
Mihon, Komikku, extension authors, or content providers.

Tankobun does not ship with an extension repository URL. Add a repository index URL
inside Settings if you want to install source extensions.

## Setup

1. Install Android Studio with Android 16 / API 36 support.
2. Copy `local.properties.example` to `local.properties`.
3. Set `anilistClientId` to your AniList application client id.
4. Use `tankobun://auth/anilist` as the AniList redirect URL.
5. Build the `app` module.

The repository intentionally avoids bundling source APKs or content.
