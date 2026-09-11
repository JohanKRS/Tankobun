<p align="center">
  <img src="docs/app-icon.png" width="148" alt="Tankobun app icon">
</p>

<h1 align="center">Tankobun</h1>

<p align="center">
  <strong>A personal Android manga shelf for reading, tracking, and AniList-compatible organization.</strong>
</p>

<p align="center">
  Tankobun helps you keep a local or AniList-synced manga library, reading progress, sources, downloads, and backups in one beautiful app.
</p>

<p align="center">
  <img src="docs/mockups/v4/phone-home-studio.webp" width="34%" alt="Tankobun light Home on a phone with original fictional manga titles and artwork">
  <img src="docs/mockups/v4/tablet-home-studio.webp" width="60%" alt="Tankobun light adaptive Home on a tablet with original fictional manga titles and artwork">
</p>

<p align="center">
  <img src="docs/mockups/v4/phone-plum-home.webp" width="46%" alt="Tankobun dark Plum Home on a phone with an original fictional manga catalog">
  <img src="docs/mockups/v4/tablet-koi-home-portrait.webp" width="46%" alt="Tankobun dark Koi Home on a portrait tablet with a different original fictional manga catalog">
</p>

<p align="center">
  <img src="docs/mockups/v4/internal-library.webp" width="30%" alt="Tankobun library on a phone with original fictional manga titles and artwork">
  <img src="docs/mockups/v4/internal-browse.webp" width="30%" alt="Tankobun discovery screen on a phone with original fictional manga titles and artwork">
  <img src="docs/mockups/v4/internal-detail.webp" width="30%" alt="Tankobun manga details on a phone for an original fictional title">
</p>

<p align="center">
  <sub>All titles, characters, covers, and artwork shown in these previews are fictional material created for this project.</sub>
</p>

## What Tankobun Is

Tankobun is an Android reader and tracking client built around an AniList-compatible workflow, with an adaptive Home dashboard, responsive phone/tablet layouts, and a customizable visual system.

It is designed as a personal reading shelf: use a local library or sign in with AniList, browse manga metadata, open a manga entry, choose a user-installed source when available, read in paged or webtoon mode, and keep your progress organized.

Local library mode does not require an account. Choose AniList (the default), MangaBaka, or Combined for Home, Browse, and search. Priority modes use the other catalog for complementary search results and fallback; Combined interleaves provider rankings and merges confirmed linked works. Entries keep AniList-compatible statuses, scoring, notes, progress, and custom lists. Titles without an AniList match remain usable locally.

Tankobun is not a content service, content host, extension repository, or manga source.

## What Tankobun Does

- Local manga library with AniList and MangaBaka catalog entries.
- Choice of navigation catalog, enriched metadata, and MangaBaka Mix discovery in the existing reading flow.
- Unified genre/tag filters, searchable tag categories, multiple formats/statuses/countries, and publication year ranges in Explore and the library.
- Optional MangaBaka tracking through a personal access token.
- AniList login and user-authorized library sync.
- Adaptive Home with catalog highlights, Continue Reading, and genre discovery.
- Profile dashboard with reading activity, library statistics, genre insights, and achievements.
- Fourteen color palettes with independent Defined or Rounded component shapes.
- Manga list browsing, status management, scoring, custom lists, and progress updates.
- Reader interface with paged and webtoon modes for manga, plus scrolling or page-turn reading for novels, with adjustable typography and themes.
- A shared library, catalog search, and details page for manga and novels. Source matching follows the work’s format.
- Multiple user-managed extension repositories, including compatible manga/NovelSource APKs and LNReader JavaScript plugins.
- Update all pending extension updates from the source manager, with sequential installation, progress and a stop control. Updates to APKs still installed in Android use its installer confirmation.
- Local reading state, caching, and optional offline storage for user-selected sources where permitted by the source and applicable law.
- Source selection through extensions installed by the user.
- Complete native Tankobun JSON backups for local and synced libraries, including both catalog identities.
- Additional MyAnimeList-compatible XML export for matched AniList titles.
- In-app restore tools for supported backup files.
- Library batch selection for sharing recommendations, changing status, editing custom lists, and removing manga.
- `.tankobun-recs` recommendation files for sharing selected manga metadata with friends, with import preview and optional import into a named custom list.

## Sources, Extensions, and Content

Tankobun does not host, upload, index, provide, sell, bundle, or distribute manga, chapters, scanlations, extensions, source APKs, or extension repository URLs.

Tankobun does not include a default extension repository. It does not recommend source repositories, source websites, or places to obtain manga content.

Any source extension used with Tankobun must be added and installed by the user. The user is solely responsible for choosing which extensions, repositories, websites, or services they use, and for making sure their use complies with applicable laws, site terms, publisher rights, and creator rights.

Before loading an installed APK extension, Tankobun asks the user to trust its package and current signing identity. Existing extensions also need this initial approval; updates signed by the same identity retain it. A changed signer requires a new review. Extensions execute inside Tankobun's process: this approval is not a sandbox or a guarantee that an extension is safe.

Extension indexes, descriptors, secondary lists, APKs and novel plugin assets must use HTTPS throughout the redirect chain. The app remembers the repository signing identity on its first successful fetch, including migrated index URLs; replacement or removal of that identity pauses installation until reviewed. This first-use record does not authenticate an unknown publisher. Existing package/signer approval remains required. These transport rules apply to code distribution independently of the reading websites used by extensions.

Downloads enforce actual byte limits: 8 MiB for repository responses, 32 MiB after expanding a gzip index, 32 MiB for shared source responses (before and after HTTP decompression) and encoded reader images, 64 MiB per extension APK and 128 MiB per app APK. Novel plugin code and chapter assets retain their 4 MiB and 1 MiB limits. Code and image transfers have total deadlines, remain cancellable while reading the body, and remove partial APKs on failure. Oversized inputs produce an error instead of a truncated file.

New extension APKs are stored privately in Tankobun, following the community private-extension approach also used by Mihon. They keep their original package and source IDs but do not become separate Android applications. Installation checks the archive identity, version and signer; updates are committed atomically using immutable, read-only APK paths. Private extensions can be removed inside the reader without `REQUEST_DELETE_PACKAGES`. This reduces the permissions needed; it does not guarantee any particular Google Play Protect classification.

Existing Android-installed extensions remain supported. In **Installed**, the migration action copies an extension into Tankobun without changing library links, reading progress or per-source settings. Once copied, the phone button opens Android app information so the user can uninstall the old Android copy. Other readers may depend on that shared copy. Tankobun does not uninstall Android applications itself. Private APK files are excluded from library/settings backups, which retain source identities and settings for reinstalling later; uninstalling Tankobun or clearing its data also removes its private extensions.

Extensions awaiting approval stay visible in the extension manager. A manga with a saved source shows a review action while keeping its source selection and cached chapter list; approving the extension restores the existing connection.


Novel support uses the community LNReader plugin contract and the NovelSource text API. There is no Tankobun-specific source repository format. The same source manager accepts user-entered repository indexes; installing an LNReader plugin stores that selected plugin privately in the app. Installing a plugin authorizes its code to run. Updates keep its source identity; removing a repository does not delete reading progress or installed sources. JavaScript plugins are not automatically installed by restoring a backup.

The novel reader supports vertical scrolling or screen-sized pages turned with side taps and horizontal swipes. Center taps show or hide all controls, matching the manga reader. Wide landscape screens can show two pages side by side. Optional continuous reading loads the previous and next chapters through the existing cache and offline downloads.

It also offers serif, sans-serif and monospace fonts, text size, line/paragraph spacing, margins, maximum text width, alignment, five color modes, text selection, text search, chapter selection and precise resume after reflow. Text and illustrations share the reader cache quota and download manager. Library backups retain catalog format, source identity, chapter and text position; settings backups retain typography, reading mode, landscape layout, continuous-reading preference and repository addresses. Downloaded reading content and plugin executables are not embedded in library/settings backups.

Each installed LNReader plugin has a settings button for its text, switch, select and checkbox options. Its website can be opened inside Tankobun for sign-in; cookies stay in Android's website store, while local/session storage snapshots are scoped to that plugin and the source's origin. Reader requests use the resulting session. Settings backups preserve switches and selection preferences; free-text fields and browser sessions stay on the device because they can contain credentials.

Compatible manga and NovelSource APKs also show a settings button when they implement `ConfigurableSource`. Tankobun hosts their native Android preference screen, retaining switches, lists, multiple selections, text/password fields, sliders, nested screens and extension-defined actions and validation. Changes are saved automatically in the community-standard per-source storage and refresh the loaded source instance. Settings backups include recognized switches, selections and sliders; text fields, credentials and arbitrary plugin storage remain on the device. Only installed, trusted APKs can open these settings.

Custom chapter JavaScript and CSS are downloaded with the selected plugin and stored atomically with integrity hashes. Scripts run against an isolated chapter DOM before native text rendering, caching and downloading; a failing or unfinished script produces an error instead of silently saving incomplete text. Source settings, session changes and plugin updates invalidate the prepared chapter cache. A listed plugin is not a guarantee that its website is reachable or that every authentication flow works. Runtime dependency licenses are bundled in `core/extensions/src/main/assets/novel/LICENSES.txt`. Rebuild the software-only JavaScript runtime with `npm ci && npm test && npm run build` from `tools/novel-runtime`.

Tankobun is only a reader/tracking client. It does not grant permission to access, copy, download, or redistribute any third-party content.

Recommendation sharing files contain manga metadata and recommendation list names only. They do not include manga content, chapter URLs, source links, downloaded files, source repositories, source APKs, notes, scores, private flags, or tokens.

Personal library/settings backups are different from recommendation files: they can preserve source bindings, auxiliary extension metadata, and repository addresses configured by the user. They are intended for personal restoration and do not add built-in source recommendations.

Import reads are limited to 4 MiB for recommendation files and 32 MiB for backups. Cancelled recommendation previews do not persist file-provided metadata. Deferred AniList mutations are bound to their original login session; legacy or different-session rows are retained locally and are not automatically submitted with another login. Cross-account transfer remains an explicit library merge action.

## APK Releases

APK files, when published in this repository, are provided only as convenience builds of this project's source code.

The APK does not include manga content, source extensions, source repositories, or content feeds. Installing the APK does not provide access to any manga source by itself.

Tankobun is not distributed through Google Play. Android may show warnings when installing APKs from outside an app store. Install only if you understand and accept the risks of sideloading Android applications.

## App Updates

Tankobun can check for app updates from a static `updates.json` manifest hosted by this project, for example with GitHub Pages, with APK assets published through GitHub Releases.

The update manifest is only for official Tankobun app APK builds. It must not include manga content, source extensions, extension repository URLs, source recommendations, content feeds, or bypass/access guidance.

Release APK updates must keep the same application id and signing lineage as the installed build, and each new release must use a higher `versionCode`.

Before offering an APK to Android's installer, Tankobun requires a valid SHA-256, checks any declared byte size, and verifies the archive's package, version code/name and signing continuity against the installed app. Forward signing-key rotation must be verified by Android. Manifest and APK requests require HTTPS on every redirect.

For release builds, set `tankobunUpdateManifestUrl` in `local.properties` or `TANKOBUN_UPDATE_MANIFEST_URL` in the environment. The default points to:

```text
https://johankrs.github.io/Tankobun/updates.json
```

The app checks this manifest quietly at most once per day and also offers a manual check in Settings > About. Android still asks the user to confirm installation of downloaded APK updates.

The published manifest for official builds is stored as `docs/updates.json` and is served by GitHub Pages at the URL above.

## Support Policy

Bug reports and feature requests about the app itself are welcome.

Please do not open issues, discussions, pull requests, or support requests asking for:

- Extension repository URLs.
- Recommendations for manga sources.
- Help accessing specific manga websites.
- Help bypassing paywalls, subscriptions, login requirements, DRM, region restrictions, blocks, or other access controls.
- Help downloading, copying, or redistributing copyrighted content without permission.

Requests of that kind may be closed or removed without response.

## AniList

Tankobun uses AniList manga metadata for local and synced libraries. When a user chooses AniList sync, Tankobun uses the AniList API with user authorization.

AniList [describes itself](https://docs.anilist.co/guide/introduction) as an anime and manga database, tracking, and social site. AniList is not a manga host, source, or chapter provider, and it does not host, upload, share, sell, or provide manga, chapters, or scanlations.

Tankobun is not affiliated with, endorsed by, sponsored by, or officially supported by AniList. AniList names, marks, data, and services belong to AniList and their respective owners.

To enable AniList login in your own build, create your own AniList API client and use:

```text
tankobun://auth/anilist
```

as the redirect URL.

## MangaBaka

[MangaBaka](https://mangabaka.org) can be the preferred navigation catalog or complement AniList without requiring another account. Its [API](https://mangabaka.org/data/api) provides search, metadata, similar works, and Mix suggestions. Optional tracking uses a personal token with `library.read` and `library.write`, created in [API & apps](https://mangabaka.org/my/settings/api-and-apps). Connect it in Settings → Catalogs and accounts.

The navigation preference is saved in settings backups and does not change tracking accounts, reading sources, downloads, or local library identity. Home and Browse caches are separated by preference; the Home carousel stays at five titles and Browse shelves at ten. Combined alternates unique works in each catalog's own ranking, preserves richer artwork on linked titles, and uses the available catalog if the other fails. It does not compare popularity scores between services. “For you” remains the existing personalized MangaBaka Mix shelf based on reading and library signals; it is independent of the Combined navigation mode.

Home automatically enriches artwork for its five carousel titles, including larger MangaBaka covers and available banners or character images. AniList artwork is fetched in one batch; MangaBaka fallback uses at most two concurrent requests. Artwork checks, including titles without banners, are cached for seven days, with a 30-minute retry delay after failures. Search thumbnails cannot overwrite a cached larger variant of the same MangaBaka cover.

Thanks to MangaBaka, its contributors, AniList, and the upstream metadata communities. Tankobun is independent and is not affiliated with or endorsed by these services.

## Extension Compatibility

Tankobun can work with user-installed extensions that follow a compatible extension format used by community manga reader ecosystems.

Compatibility does not mean affiliation, endorsement, or support from those projects, their maintainers, extension authors, source websites, publishers, or content providers.

All extension code, source integrations, service names, trademarks, and third-party content remain the property of their respective owners.

## Building

1. Install Android Studio with the required Android SDK version.
2. Copy `local.properties.example` to `local.properties`.
3. Add your own AniList client id:

```properties
anilistClientId=
```

4. For signed release builds, also provide your own release keystore values in `local.properties` or the matching `TANKOBUN_RELEASE_*` environment variables shown in `local.properties.example`.
5. Build the `app` module.

## Privacy

Tankobun is intended to run as a client-side Android app.

Depending on how you use it, the app may store local app data such as reading progress, cached files, downloads, backup files, preferences, and authentication data required for AniList login or optional MangaBaka tracking.

Tankobun does not operate a server controlled by this project for manga hosting, content indexing, analytics, tracking, advertising, or user profiling.

Public catalog searches and the title IDs used for Mix are sent to MangaBaka without requiring a MangaBaka account. If connected, subsequent tracking edits are sent using the personal token, which is stored securely on the device and excluded from backups. See the [MangaBaka privacy policy](https://mangabaka.org/about/privacy) and [terms](https://mangabaka.org/about/terms). Third-party services, image hosts, and user-installed extensions also have their own privacy policies and terms.

AniList and MangaBaka credentials are persisted only in encrypted storage. If Android's secure storage cannot be opened or recovered, account connection is unavailable and the local library remains usable. Legacy plaintext tokens are discarded. Disconnecting one provider clears only that provider's credentials, with a deferred revocation if the encrypted store is temporarily unavailable.

## Disclaimer

Tankobun is provided for personal and lawful use only.

The author does not host, provide, endorse, verify, or control third-party manga content, source websites, extension repositories, or user-installed extensions.

Users are responsible for how they configure and use the app.

## Name and Branding

The Tankobun name, icon, and project branding identify this project. Forks, modified builds, and redistributed APKs must not imply endorsement, affiliation, or official support from the original author.

If you distribute a modified version, please use a clearly different app name, package name, and icon.

## License

Tankobun code is licensed under the [MIT License](LICENCE.md). Catalog data, images, and third-party components retain their own rights and licenses; they are not relicensed under MIT. See [NOTICE.md](NOTICE.md) for attribution.

## Third-Party Notices

Some extension compatibility behavior is adapted from the Mihon/Tachiyomi-compatible network and extension host ecosystem under the Apache License 2.0. See `NOTICE.md` for attribution and license details.

### Catalog data and acknowledgements

Metadata is provided by [AniList](https://anilist.co) and [MangaBaka](https://mangabaka.org), including upstream data from [MangaUpdates](https://www.mangaupdates.com), [MyAnimeList](https://myanimelist.net), [Kitsu](https://kitsu.app), [Shikimori](https://shikimori.one), [Anime-Planet](https://www.anime-planet.com), and [Anime News Network](https://www.animenewsnetwork.com).

MangaBaka-original data uses [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/). Tankobun combines and formats metadata for display; adaptations of MangaBaka-original data retain that license. Third-party data and artwork remain subject to their owners and provider terms. The MangaBaka license does not grant additional redistribution rights to third-party data.

See the [data license](https://mangabaka.org/about/data-license), [noncommercial terms](https://mangabaka.org/about/data-license-noncommercial), and [API attribution guidance](https://mangabaka.org/data/api). Credits and links are available in Settings → About. Native backups and recommendation exports containing MangaBaka entries carry a data attribution notice. Queries and caching are limited to on-demand app use.
