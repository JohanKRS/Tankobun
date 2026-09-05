# Library refresh on opening

The AniList settings include a switch, enabled by default, to refresh library membership when the app enters the foreground. Local mode and signed-out sessions do not trigger it. The UI displays cached data immediately; an automatic refresh does not block navigation or show a success notification.

Automatic checks are coalesced and spaced by at least two minutes, including failed attempts and quick process restarts. The timestamp belongs to the current login session and is separate from reading progress timestamps. Manual synchronization remains available. All requests use the existing shared AniList client, including its rate-limit headers and server backoff.

The automatic request includes the complete entry membership and tracking fields, including custom lists. This detects remote additions, edits and removals without downloading every manga's metadata again. Missing metadata is fetched through the existing batched query. A warm refresh uses one collection query; after a cold start, the existing viewer refresh is shared with it. Manual synchronization also refreshes cached manga metadata and viewer statistics.

The database applies the snapshot atomically. Queued mutations, including delayed and unowned legacy rows, remain protected. Changes made locally while a request is in flight also take priority, including deletion tombstones. Switching account or library mode discards the response. Remote removals affect list membership and keep downloaded files, source bindings and reading history.

Missing collections, incomplete entries, GraphQL failures, further chunks and collections reaching the API's 11,000-entry cap cannot be used to infer deletions. A complete empty collection is valid and clears membership. Incomplete manual refreshes show a localized message; automatic failures keep the cache and wait for a later opening. The switch and the error message are available in English, Brazilian Portuguese, Spanish and Simplified Chinese. Settings backups retain the switch, without copying session cooldown state.

Regression coverage includes remote additions/removals, empty lists, pending changes, concurrent edits, custom-list duplicates, incomplete responses and cooldown boundaries. A separate Android test installation also exercises the real database and ViewModel with intercepted local GraphQL responses, including service errors, account switching and repeated foreground events. No real account mutations or source repository configuration are needed by the committed tests.
