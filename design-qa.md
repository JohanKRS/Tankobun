# Home responsive design QA

- Annotated tablet source: `/Users/johan/Documents/tablet.png`
- Annotated phone source: `/var/folders/37/xbnjjyy16rzcnffjlyv6lwym0000gn/T/codex-clipboard-6bdc3ccd-b54b-4af3-9bbe-fb9eed4e0a35.png`
- Tablet implementation: `/tmp/tankobun-home-tablet-final-portrait.png`
- Tablet normalized comparison frame: `/tmp/tankobun-home-tablet-final-924.png`
- Phone implementation: `/tmp/tankobun-home-phone-final.png`
- Tablet viewport: Pixel Tablet, portrait, 1600 x 2560 (implementation normalized to 924 x 1467 for comparison)
- Phone viewport: Pixel 9, portrait, 1080 x 2424
- State: live AniList Home feed; active Charcoal Gold theme on phone and the emulator default light theme on tablet

## Findings

No actionable P0, P1, or P2 findings remain.

- Tablet hero: AniList banner is the primary visual. It occupies 72% of the card width, is center-cropped, and fades left beneath the 42%-wide content column. The fallback order is banner, cover, then main character; fallback covers use the same crop and placement as banners.
- Phone hero: image priority and vertical fit remain unchanged. Only the title renderer changed, now sharing the detail page's measured auto-resizing and line-breaking implementation.
- Carousel: the pager owns its horizontal content padding, so cards align with the section grid at rest while adjacent cards can reach and peek through the screen edges during horizontal scrolling.
- Continue Reading: header and card strip now use the same responsive horizontal padding as the other Home sections. Covers and progress data behavior are unchanged.
- Genre highlights: tablet renders two balanced columns. Rows have a fixed responsive height and their banner-first images fill the complete vertical image slot on both phone and tablet.
- Theme and icons: all colors still resolve through the active Tankobun theme and all generic interface/category icons remain Tabler icons.
- Accessibility and stability: titles remain readable for long live AniList names, primary actions stay inside their cards, and the UI hierarchy exposes section and media labels without horizontal overflow.

## Expected differences

- The annotated tablet screenshot contains saved Continue Reading data, while the clean QA emulator has no reading history. The section is conditional and therefore absent from the implementation capture; its padding was verified in code against the same edge-to-edge `LazyRow` pattern used by Browse recommendations.
- The source uses a dark theme and red annotation overlays; the normalized tablet capture uses the emulator's light theme to confirm that the implementation follows theme tokens rather than hard-coded mock colors.
- Live AniList trending results and images can differ between captures.

## Interaction verification

- Hero pager auto-advance and adb swipe both changed pages.
- A banner-backed item (One Piece) filled the tablet image region and faded into the content column.
- A no-banner item used its cover as the tablet fallback and filled the same region instead of leaving isolated character art.
- Phone retained the full-height character composition and single-column genre layout.
- Tablet used the two-column genre layout in both portrait and landscape widths.
- Android UI trees were inspected on Pixel 9 and Pixel Tablet; no Tankobun crash was present in logcat.

final result: passed
