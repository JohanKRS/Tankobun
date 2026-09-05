# Tablet validation — 2026-09-05

Device: SM-X910, Android 16. Testing used a separate application ID, private library and preferences, with production updating and authentication disabled. The normal installation retained its package identity, version and installation timestamp. No personal library database or credentials were read or changed.

## Controlled extension flows

Both a legacy implementation and a modern suspend implementation passed search, details, complete chapter lists, whole-volume entries and volumes with an internal table of contents. Together they decoded 24 original fixture pages, repeated 24 cache reads and resumed partial downloads without rewriting the two valid files in each job.

With a fixed 40 ms response delay, median byte retrieval times were 180/4 ms and 186.5/5 ms for cold/warm reads. These measurements exclude decoding and drawing.

The optimized release build was measured over one sequence of 20 page changes in each reader mode:

| Mode | Rendered frames | Janky frames | p50 | p95 | p99 | Additional origin requests |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Paged | 898 | 20 (2.23%) | 6 ms | 7 ms | 24 ms | 0 |
| Webtoon | 896 | 21 (2.34%) | 6 ms | 7 ms | 20 ms | 0 |

Reopening retained Webtoon mode and page 2 of 6 while the local origin was unavailable. These are observations from one device and sequence, not a before/after benchmark or a claim that all devices and content behave identically.

## User-configured repository and live sample

The repository configured only in the test installation loaded 1,394 extension entries. Existing extension packages on the tablet were reused without updating them. The checkout and distributed application contain no repository configuration, extension packages or external content from these tests.

Two installed extensions passed bounded live samples. One returned 1,192 chapters, a 57-page chapter and a valid decoded image; its first three pages were also opened in the app's rendered reader in Webtoon and paged modes. The other returned two chapters, a 143-page chapter and a valid decoded image. Each successful probe recorded five HTTP 200 exchanges in its extension client, with an added 1.5-second spacing. Client counts do not include internal WebView traffic. The full remote chapters were not downloaded.

An earlier attempt on the second extension's much larger title reached the probe's 50-second deadline. The extension itself allows 120 seconds for its WebView stage. The smaller sample passed with a 150-second probe deadline. The large-title attempt remains inconclusive; no site-specific app patch was introduced.

## AniList refresh

Fourteen assertions passed against the actual Android database and ViewModel with GraphQL responses intercepted locally: additions, edits, removals, missing-metadata batching, a warm one-query refresh, delayed pending edits and deletions, concurrent edits, service failures, incomplete responses, account switching, complete empty lists, retained downloads, foreground coalescing, persisted cooldown and disabling the setting. A synthetic test credential was used; no real account mutations were issued.

A separate public API contract check received a 403 response stating that the service was temporarily disabled for stability issues. Live account reconciliation was therefore not verified. The cache-preservation behavior for that failure was verified locally.

The new setting and incomplete-response message have translations in all four supported locales. Final validation passed 228 distinct unit tests, debug/release builds, release lint and whitespace checks. Normal build outputs were restored to the production application ID after producing the separate test APK.
