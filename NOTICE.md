# Notices

Tankobun includes extension compatibility behavior adapted from, or modeled after,
the Mihon/Tachiyomi-compatible Android extension host and network layer.

Upstream projects:

- Mihon: https://github.com/mihonapp/mihon
- Tachiyomi: https://github.com/tachiyomiorg/tachiyomi
- Bebas Neue: https://fonts.google.com/specimen/Bebas+Neue

The compatibility work is based on Apache License 2.0 projects and APIs from
the Tachiyomi/Mihon extension ecosystem. The Apache License 2.0 text is included
at `docs/licenses/APACHE-2.0.txt`.

Bebas Neue is bundled for selected manga detail typography under the SIL Open
Font License 1.1. The license text is included at `docs/licenses/OFL-1.1.txt`.

Tankobun-specific changes include integration with Tankobun's extension scanner,
source host, dependency registry, browser-like user-agent handling, shared Android
WebView cookies, supported content-encoding handling, Cloudflare retry behavior,
and defensive exception wrapping for extension network calls.

Tankobun does not bundle extension APKs, manga sources, extension repository URLs,
or third-party content. User-installed extension packages may be used as runtime
compatibility test inputs, but this project does not link to or recommend any
extension repository.
