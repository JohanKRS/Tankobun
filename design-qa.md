# Home character mosaic and mobile header QA

- Source sketch: `/var/folders/37/xbnjjyy16rzcnffjlyv6lwym0000gn/T/codex-clipboard-a6ef4dfc-1e5b-46bf-b52c-1da32a8e586a.png`
- Combined source/implementation comparison: `/tmp/tankobun-home-final-comparison.png`
- Tablet portrait implementation: `/tmp/tankobun-center-portrait.png`
- Tablet landscape implementation: `/tmp/tankobun-tablet-mosaic-balanced-narrow.png`
- Mobile Home header implementation: `/tmp/tankobun-mobile-header.png`
- Mobile Library header implementation: `/tmp/tankobun-mobile-library.png`
- Mobile two-character hero: `/tmp/tankobun-mobile-balanced-final.png`
- Portuguese genre labels and section spacing: `/tmp/tankobun-home-pt.png`
- Tablet viewport: Pixel Tablet, 1600 x 2560 portrait and 2560 x 1600 landscape
- Mobile viewport: Pixel 9, 1080 x 2424 portrait
- State: live AniList data with a debug-only forced mosaic used for visual inspection; the force was removed before the final build

## Findings

No actionable P0, P1, or P2 findings remain.

- The no-banner tablet fallback forms one continuous image region with shallow slanted panel edges, matching the sketch without making the angles distracting.
- Portrait uses up to four panels; landscape expands to as many as seven panels.
- AniList character roles are preserved: main characters are prioritized, and the visual ordering places the highest-priority characters in the center before alternating toward the edges.
- The tablet mosaic occupies 72% of the hero width. Individual strip width is calculated from the available character count, including the small intentional overlap between angled edges.
- Mobile uses two narrower strips inside the rightmost 72% of the hero. The principal character is always on the right; a single available character keeps the existing fallback.
- Horizontal and bottom fades were shortened and reduced in opacity. Their leading edge remains opaque enough to hide a hard image boundary, while character color and detail remain visible and title, author, and description retain usable contrast.
- Tablet text content uses a narrower 38% column, overlaps the image edge slightly, and allows up to five description lines to use the available vertical space without obscuring the central characters.
- Banner-backed tablet items remain unchanged.
- The shared top bar now uses the same 18 dp horizontal inset as page content on compact layouts. Home and Library screenshots confirm the leading page icon aligns with section cards and controls.
- Home genre labels now reuse Browse's localized labels; the Portuguese UI tree exposed `Ação`, `Aventura`, and `Comédia` in the genre list and localized hero chips.
- Home section spacing increased from 20 dp to 24 dp, adding a small amount of breathing room without changing the page rhythm.

## Expected differences

- The sketch is an intentionally loose layout concept rather than a pixel reference. The implementation keeps the current Tankobun typography, theme colors, controls, spacing, and hero dimensions.
- Live AniList trending data currently has banners or only one character on its no-banner tablet entries. A banner-backed entry with several characters was temporarily forced into the tablet mosaic path for QA, then the normal banner-priority guard was restored.

## Interaction and stability verification

- The hero pager auto-advanced and responded to adb swipes.
- The tablet was inspected in portrait and landscape using UI hierarchy dumps and screenshots.
- Mobile Home and Library were navigated using UI-tree-derived coordinates.
- Unit tests cover banner priority, cover fallback, portrait limits, landscape limits, center-first tablet placement, two-character mobile placement, and the one-character mobile fallback.
- Android crash logs contained no Tankobun crash, migration failure, or ANR.

final result: passed
