# Notices

Tankobun includes extension compatibility behavior adapted from, or modeled after,
the Mihon/Tachiyomi-compatible Android extension host and network layer.

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
