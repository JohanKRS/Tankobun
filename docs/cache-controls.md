# Cache controls

Downloads settings offers independent storage budgets for browsing images and automatic reader pages, plus retention for navigation metadata. Presets combine these values:

| Profile | Images | Navigation data retention | Reader pages | Pages ahead |
| --- | --- | --- | --- | --- |
| Compact | 128 MiB | 7 days | 256 MiB | 2 |
| Balanced (default) | 512 MiB | 30 days | 2 GiB | 6 |
| Extensive | 2 GiB | 90 days | 8 GiB | 12 |

Users can independently choose 32 MiB–8 GiB for images, 7–365 days for navigation records, 128 MiB–32 GiB for reader pages, and disable speculative page prefetch. Limits describe maximum storage, not reservations or a request to download the library. Increasing disk limits does not increase the bitmap memory budget.

The default skips speculative page fetches on metered or unknown networks. Visible reader content still loads normally, including the visible/precomposed Webtoon window. Prefetch also retains up to two pages behind the reading direction and up to two at an adjacent chapter boundary. It uses the existing shared source clients, rate limits, and backoff behavior.

## Covers and navigation

Coil images share one disk cache with a configurable budget. Both normal URL images and source-specific thumbnail fetchers participate. Resizing waits for active readers and editors to finish, then reopens and trims through Coil's APIs. Existing images remain available within the new limit; no open journals are deleted.

Normal HTTP images now use Coil's Cache-Control strategy, including conditional revalidation where the server supports it. Source fetchers that expose only byte arrays retain covers for seven days before fetching again, with stale fallback on fetch failure and no fallback on cancellation. These fetches are deduplicated. Enlarging the cache never extends freshness.

Titles, descriptions, authors/staff, tags, search results, recommendations, source search candidates, and unused source chapter records live in Room. Retention runs at app startup and when preferences change. It uses fetch timestamps independently of existing freshness TTLs. Whole search/recommendation result sets are retained together to avoid incomplete cached pages.

Metadata needed by list entries, source bindings, reading progress, download jobs/pages, or queued sync mutations is protected. Bound or read/downloaded chapters are also protected. Navigation can be cleared separately; clearing all cache applies the same protections. Producers save related metadata and library/search/recommendation rows in one transaction so cleanup cannot remove data halfway through an insertion. SQLite can reuse released pages; record removal does not necessarily shrink the database file immediately. The UI reports navigation records separately from cache file sizes.

API HTTP responses and source HTTP responses each retain their separate, fixed 128 MiB limits. They continue to follow server headers. Users can inspect and clear these caches; the storage profiles do not replace extension-specific transport or throttle behavior.

## Reader pages

The page store lazily indexes files once per process, tracks reads in memory, persists chapter recency, evicts least-used pages across titles, and reserves at least 256 MiB of free storage when writing. It trims at startup and when the user changes the limit. A page larger than the budget or insufficient free storage skips caching without stopping reading. Cache clearing invalidates pending page writes; network caches are cleared through their owners.

Page byte freshness is seven days from writing, independent of the storage profile. Reads do not extend it. Expired images are fetched again when needed; an existing copy can be used when fetching fails. Cancellation and explicit retries do not silently return stale bytes. Page manifests are still obtained from the extension when opening a chapter so retained files do not keep expired signed page addresses alive. Keys include page identity; legacy cache files are reclaimed within the same storage budget as the user reads.

Downloads have separate files and remain available offline. Automatic eviction and cache clearing do not remove them.

All preferences are persisted and included in settings backups. Older backups retain current values for absent fields. UI copy is present in English, Brazilian Portuguese, Spanish, and Simplified Chinese.
