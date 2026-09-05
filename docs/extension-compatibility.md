# Extension compatibility

Tankobun hosts user-installed extensions through a shared compatibility layer. Changes here must address API contracts and execution behavior, without source-specific branches, bundled source packages, catalogue addresses, or content fixtures from external sites.

## Supported execution behavior

- The host invokes the suspend API once. Legacy implementations are bridged through cancellable Rx subscriptions. A failed operation or an empty result does not trigger a second call through another API.
- HTTP error responses are rejected before default parsers run. Extensions retain their request builders, clients, interceptors, request methods, bodies, headers, and explicit cache policies.
- Default request helpers allow HTTP caching with a ten-minute maximum age, as expected by the extension API. Explicit reload policies still apply; server cache directives remain authoritative.
- Extension-defined request intervals round up. Network throttling applies to real network exchanges, including redirects, so valid cached responses do not wait for a network quota. Waiting calls check for cancellation.
- Server backoff handles both delay seconds and HTTP dates. Exhausted quota reset times and large numeric values cannot wrap into immediate retries. Image retries remain bounded and avoid multiplying download-worker retries.
- The host no longer imposes separate eight-second page-list and twenty-five-second image deadlines. Connect/read timeouts and extension-specific timeouts still apply. Automatic multi-source matching retains its bounded search budget and can report a timeout independently of extension compatibility.
- A package is loaded once at a time, retaining shared client/limiter state. Updates of the same manga are serialized; unrelated manga can proceed independently. Signer approval still gates the host's entrypoints.
- Opaque manga/chapter JSON metadata retains nested objects, arrays, numbers, booleans, and nulls. It survives source searches, selected bindings, chapter caching, and personal library backups. Recommendation sharing does not include this metadata.
- Combined updates preserve returned manga metadata as well as chapters. Existing chapter metadata is supplied to the extension. The legacy preparation hook runs only for new chapters.
- Reader order follows the returned page list. The original extension index, page URL, and image key are retained when building an image request. Image requests are constructed at fetch time, avoiding repeated URL/signature transformations. Local page URIs are read through Android's content resolver; HTTP addresses in the URI field still use the extension's transport.
- Downloads record whether their index is a list position. Ambiguous legacy partial downloads are fetched again instead of reusing an image at the wrong position; obsolete page entries are pruned only after successful fetching.
- HTML pagination selectors may be absent. URL helpers preserve encoded identifiers and resolve relative and protocol-relative paths. Historical Tankobun default IDs remain stable; the explicit ID-generation helper follows the public API contract.
- Runtime compression includes Brotli and Zstandard. The self-hosted source marker is available without disabling configured HTTP limits.

## Regression checks

Tests use original synthetic sources and loopback HTTP servers. No test requires an extension repository or a source website. Relevant suites include `SourceContractTest`, `NetworkTrafficTest`, `SourceModelBridgeTest`, `SourcePageBridgeTest`, `SourceUrlsTest`, `SourceMetadataTest`, and `ServerBackoffTest`.

Run the project's debug unit tests, the JVM module tests, debug/release builds, release lint, and `git diff --check`. Database schema changes also require migration verification on Android with populated previous-version records and a reopen check.

User-authorized external packages may be inspected outside the checkout for additional binary/runtime checks. Their files, addresses, icons, and content must not be committed or packaged. Loading a source and constructing its client is separate from verifying a live website flow: authentication, site changes, remote failures, and extension bugs can still prevent an operation from succeeding. Do not claim that every extension or website was tested based on a sample.

## Functional and performance checks

Use a separate application ID for testing on a personal device. Keep its database, preferences, file-provider authority, and storage separate; disable production update handling and external import/authentication intents in that temporary build. Never clear or replace the user's normal installation to prepare a test.

Exercise both legacy and suspend implementations against original local fixtures:

1. Search, obtain details, and enumerate the complete chapter list. Include whole-volume entries and volumes with an internal table of contents.
2. Load every page of the selected fixtures. Decode the images and verify their identity, dimensions, and returned order, including one-based source indexes and chapter URL fragments.
3. Read the same pages again through the app's reader cache. Count actual server requests, not just host API invocations; warm reads must not download the same images again.
4. Resume a partially stored download through the production download runner. Verify completeness and that already valid files were not rewritten.
5. Navigate the rendered reader in paged and webtoon modes, cross a chapter boundary, and reopen the app to verify the saved reading position. Verify downloaded reading with the test origin unavailable.
6. Record cache retrieval times separately from decoding and frame rendering. Capture frame statistics and memory around a defined interaction sequence, recording the device, Android version, build variant, renderer, and run count.

Repeatable fixtures should use a measured local response delay and record request timestamps. Keep live-origin checks small, spaced, and explicitly bounded, with no automatic crawl of the entire catalogue. Emulator software rendering is unsuitable for judging device smoothness; a physical device with an optimized build is preferable. A small test sample cannot establish universal website availability or rule out memory leaks.
