# Website showcase assets

The localized screenshots in `en`, `pt`, `es` and `zh` were captured from Tankobun's actual 4.2 UI, at source commit `0f7d616`, using an isolated Android emulator installation. Application UI code was not edited for the captures. No account, source service or external extension repository was used.

Only demonstration records were populated: eight fictional titles, existing project-created covers from `docs/mockups/fake-covers`, local reading progress, and an original comic page. The covers are not third-party catalog assets. `art/original-page.webp` is a compressed copy of the original image generated for this website: a bicycle courier delivering a sapling in an invented coastal town. It contains no branded characters or copied comic panels. These assets are website-only and are not bundled into the APK.

The first fictional title is “Além do Horizonte”. The main device is a tablet in portrait orientation; the secondary device is a phone in the dark theme, with a different highlighted title. The tablet Home includes sixteen category entries and three visible Continue Reading titles, matching the application's existing three-item limit. The library capture shows the planned-reading category to feature additional covers. The tracking screenshot demonstrates the actual AniList-compatible controls in local mode; it does not depict a connected AniList account.

An initial demonstration mistakenly reused a portrait cover as a banner. Removing that incorrect banner value restored the existing leading-edge fade in the captures. No rendering code or released APK was changed.

Images were resized and encoded as WebP. Website frames and decorative backgrounds use CSS. All four feature screenshots are shown in full: library, manga details, reader and tracking. Clicking is an optional additional enlargement. Controls were not painted over or assembled into invented app screens.

The app icon is an optimized copy of `docs/app-icon.png`. The website reuses the existing local Bebas Neue font and links to the project's license notices.

## Design comparison

The accepted image concept guided the composition, with these checks against the rendered page:

- Cream background, orange actions and mint phone stage retained.
- Existing icon and condensed Tankobun wordmark retained in the header.
- Single, large three-line title retained; no paired slogan added.
- After user feedback, a portrait tablet became the primary device, alongside a smaller Plum phone.
- Four complete feature screenshots: organization, details, reading and AniList. Cropping was removed following user feedback.
- Release notes placed in the footer, below the product presentation.
- Deliberate differences: actual navigation, reader controls and tracking fields replace invented controls; screenshots can be enlarged; short feature descriptions and content/licensing notices remain available.
- Responsive layout stacks the hero and feature columns on small screens. Chinese uses the system font for readable native glyphs.
