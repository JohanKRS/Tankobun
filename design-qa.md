# Home hero hierarchy, fade, contrast, and padding QA

- Source sketch: `/var/folders/37/xbnjjyy16rzcnffjlyv6lwym0000gn/T/codex-clipboard-a6ef4dfc-1e5b-46bf-b52c-1da32a8e586a.png`
- Combined mockup/detail/Home comparison: `/tmp/tankobun-home-hierarchy-fade-comparison.png`
- Manga details title reference: `/tmp/tankobun-mobile-detail.png`
- Tablet landscape implementation: `/tmp/tankobun-tablet-mask.png`
- Mobile light-theme implementation: `/tmp/tankobun-mobile-final-light.png`
- Mobile dark-theme implementation: `/tmp/tankobun-mobile-shadow-dark.png`
- Mobile single-character fallback: `/tmp/tankobun-mobile-single-mask-fixed.png`
- Mobile Library padding hierarchy: `/tmp/tankobun-mobile-library-padding.txt`
- Mobile Browse padding hierarchy: `/tmp/tankobun-mobile-browse-padding.txt`
- Tablet viewport: Pixel Tablet, 2560 x 1600 landscape
- Mobile viewport: Pixel 9, 1080 x 2424 portrait
- State: live AniList data with a debug-only forced mosaic used for visual inspection; the force was removed before the final build

## Findings

No actionable P0, P1, or P2 findings remain.

- The no-banner tablet fallback forms one continuous image region with shallow slanted panel edges, matching the sketch without making the angles distracting.
- Portrait uses up to four panels; landscape expands to as many as seven panels.
- AniList character roles are preserved: main characters are prioritized, and the visual ordering places the highest-priority characters in the center before alternating toward the edges.
- The tablet mosaic occupies 72% of the hero width. Individual strip width is calculated from the available character count, including the small intentional overlap between angled edges.
- Mobile uses two narrower strips inside the rightmost 72% of the hero. The principal character is always on the right; a single available character keeps the existing fallback.
- The image region now uses a real alpha mask: it starts fully transparent at its leading edge and reaches full opacity across the first 12% of the image. This removes the straight image cut while keeping the requested 10–15% transition compact.
- The single-character mobile fallback constrains its image region to the same right-aligned area as the visible artwork, so its alpha mask starts at the actual image edge instead of fading an empty part of the hero.
- Hero title, author, and description use a zero-offset, softly blurred shadow colored from the theme's banner/card background. The halo therefore blends into light and dark themes instead of introducing a fixed white or black outline.
- The Home title reuses the details page's auto-resizing manga-title component and expanded hierarchy. Its available height was increased so it is larger and more prominent without clipping long titles.
- The rank badge is slightly smaller, reducing competition with the title.
- Tablet text content uses a narrower 38% column, overlaps the image edge slightly, and allows up to four description lines without obscuring the central characters.
- Banner-backed tablet items remain unchanged.
- Home, Library, and Browse all resolve to the same 18 dp compact horizontal inset. Pixel 9 UI hierarchies place their leading content at x=47 px, so no padding adjustment was necessary; the small perceived difference comes from whitespace inside individual Tabler glyphs.
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
- The combined visual comparison confirms title hierarchy against the details page, a smooth 12% image transition, and readable theme-aware contrast in both light and dark themes.
- Android crash logs contained no Tankobun crash, migration failure, or ANR.

## Home heading grid and icon baseline QA — 2026-07-17

- Annotated reference: `/var/folders/37/xbnjjyy16rzcnffjlyv6lwym0000gn/T/codex-clipboard-8451d41d-8e37-4904-a167-3c5b80bf6f12.png`
- Pixel Tablet implementation in Portuguese: `/tmp/tankobun-tablet-heading-alignment-pt.png`
- UI hierarchy: `/tmp/tankobun-tablet-home-pt-ui-summary.txt`
- Viewport: Pixel Tablet, 2560 x 1600 landscape

No actionable P0, P1, P2, or P3 findings remain.

- The top chrome and Home section headings now reuse one leading-layout primitive, so icon size and icon-to-text spacing cannot diverge between them.
- `INÍCIO`, `EM ALTA`, and `EM ALTA POR GÊNERO` all begin at x=110 px in the captured UI hierarchy.
- Icons use an explicit optical alignment line tied to the text's first typographic baseline rather than vertical centering. The home and flame glyphs visibly finish on the baseline of the main letter body, while diacritics and descenders do not affect placement.
- The shared component preserves the existing typeface, size, color, trailing actions, responsive padding, and navigation behavior.
- Unit tests pass and the Android crash buffer is empty.

final result: passed
