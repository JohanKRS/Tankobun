# Notices

Tankobun includes extension compatibility behavior adapted from, or modeled after,
the Mihon/Tachiyomi-compatible Android extension host and network layer.

Private APK extension storage and migration follow the community architecture
documented in Mihon's ExtensionLoader and ExtensionInstaller. Tankobun retains
original APK identities and uses its own atomic storage and validation integration.
Reference: https://github.com/mihonapp/mihon/blob/aebf11a74954fd82f81b8bce9f8ac4fc89fa6127/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt

Upstream projects:

- Mihon: https://github.com/mihonapp/mihon
- Tachiyomi: https://github.com/tachiyomiorg/tachiyomi
- Bebas Neue: https://fonts.google.com/specimen/Bebas+Neue
- Tabler Icons: https://tabler.io/icons
- Compose Icons: https://github.com/DevSrSouza/compose-icons

The compatibility work is based on Apache License 2.0 projects and APIs from
the Tachiyomi/Mihon extension ecosystem. The Apache License 2.0 text is included
at `docs/licenses/APACHE-2.0.txt`.

Bebas Neue is bundled for selected manga detail typography under the SIL Open
Font License 1.1. The license text is included at `docs/licenses/OFL-1.1.txt`.

The interface iconography uses Tabler Icons through the Compose Icons adapter.
Both projects are MIT-licensed; their license texts are included at
`docs/licenses/TABLER-ICONS-MIT.txt` and `docs/licenses/COMPOSE-ICONS-MIT.txt`.

Every app build also packages this notice, Tankobun's MIT license, and the four
license texts above under `assets/licenses/`. Settings → About displays these
bundled copies without requiring a network connection. The build copies only
these named license files, not website assets, screenshots, or test fixtures.

Tankobun-specific changes include integration with Tankobun's extension scanner,
source host, dependency registry, browser-like user-agent handling, shared Android
WebView cookies, supported content-encoding handling, Cloudflare retry behavior,
and defensive exception wrapping for extension network calls.

Tankobun does not bundle extension APKs, manga sources, extension repository URLs,
or third-party content. User-installed extension packages may be used as runtime
compatibility test inputs, but this project does not link to or recommend any
extension repository.

## Catalog data and images

Thanks to MangaBaka, its contributors, AniList, and the upstream metadata communities.
Tankobun uses AniList as its primary catalog and MangaBaka for complementary
metadata, search, recommendations, Mix discovery, and optional tracking.

- [MangaBaka](https://mangabaka.org) · [API and attribution](https://mangabaka.org/data/api)
- [AniList](https://anilist.co) · [Terms](https://docs.anilist.co/guide/terms-of-use)
- [MangaUpdates](https://www.mangaupdates.com) · [Acceptable use](https://api.mangaupdates.com/#section/Acceptable-Use-Policy)
- [MyAnimeList](https://myanimelist.net) · [Terms](https://myanimelist.net/about/terms_of_use)
- [Kitsu](https://kitsu.app) · [Terms](https://kitsu.app/terms)
- [Shikimori](https://shikimori.one) · [API](https://shikimori.one/api/doc)
- [Anime-Planet](https://www.anime-planet.com) · [Terms](https://www.anime-planet.com/about/terms)
- [Anime News Network](https://www.animenewsnetwork.com)

MangaBaka-original data is licensed under
[Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-nc-sa/4.0/).
Tankobun combines and formats metadata for display; adaptations of MangaBaka-original
data retain CC BY-NC-SA 4.0. Data and artwork originating from other providers retain
their respective rights and terms. Neither this notice nor the app's MIT license
grants additional rights to third-party data or images.

See MangaBaka's [data license](https://mangabaka.org/about/data-license),
[noncommercial terms](https://mangabaka.org/about/data-license-noncommercial),
[service terms](https://mangabaka.org/about/terms), and
[privacy policy](https://mangabaka.org/about/privacy).

Tankobun is independent of these providers. Names, marks, data, and images belong
to their respective owners; attribution does not imply affiliation or endorsement.

## Novel reader compatibility

The independently implemented novel adapter follows LNReader's CommonJS plugin
contract and the NovelSource text-source API used by community readers. Thanks to
the LNReader and Tsundoku contributors for documenting these interfaces. No source
plugin code or source/repository index is bundled with Tankobun.

The JavaScript runtime bundles Cheerio, htmlparser2, dayjs, urlencode,
@noble/ciphers, protobufjs and their runtime dependencies. Their complete MIT,
ISC and BSD license notices are included in
`core/extensions/src/main/assets/novel/LICENSES.txt`, generated from the pinned
project-local dependency lockfile. The Apache-2.0 notice above also applies to the
Tachiyomi/NovelSource interface compatibility layer. Build tooling and test-only
fictional content do not supply reading sources.
