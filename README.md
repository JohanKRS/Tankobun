<p align="center">
  <img src="docs/app-icon.png" width="112" alt="Tankobun app icon">
</p>

# Tankobun

Tankobun is an Android manga reader built around an AniList-first flow. It is meant to feel like a personal reading shelf: sign in with AniList, browse or open a manga, choose a source, read in paged or webtoon mode, and keep progress synced without fuss.

## What It Does

- AniList login, library sync, tracking edits, custom lists, scoring, and queued progress sync.
- Library and browse views with configurable layouts, cover framing, filtering, and tablet-friendly navigation.
- Source selection through user-installed Tachiyomi-compatible extensions.
- Paged and webtoon reader modes with local caching, downloads, and respectful download throttling.
- Quick Actions drawer for sync, AniList tracking, continue reading, and downloads.
- MyAnimeList-format XML backups for AniList lists, with in-app restore and scheduled backups.

## Sources And Content

Tankobun does not host, upload, provide, or bundle manga/chapter content. It also does not ship with an extension repository URL. If you want source extensions, add a repository index URL in Settings and install the extensions yourself.

Source compatibility follows the extension format used by Tachiyomi and the broader Tachiyomi/Mihon ecosystem. Credit belongs to the extension creators and maintainers who build and maintain those source integrations. Please respect source terms, creators, publishers, and local laws.

## AniList

Tankobun is not affiliated with AniList. AniList data is accessed through the user-authorized AniList API, and any AniList names, marks, or services belong to AniList and their owners.

The backup feature exports manga lists as MyAnimeList-compatible XML because AniList supports importing that format. Backups can be restored from AniList's web import page or directly inside Tankobun.

## Setup

1. Install Android Studio with Android 16 / API 36 support.
2. Copy `local.properties.example` to `local.properties`.
3. Set `anilistClientId` to your AniList application client id.
4. Use `tankobun://auth/anilist` as the AniList redirect URL.
5. Build the `app` module.

## Project Notes

This is a personal/private APK project. It is not affiliated with AniList, Tachiyomi, Mihon, Komikku, extension authors, or content providers. The repository intentionally avoids bundling source APKs, extension repositories, or content.
