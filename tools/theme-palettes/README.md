# Theme palettes

Tankobun's color themes are generated from the seeds in `themes.json` with
[Material Color Utilities](https://github.com/material-foundation/material-color-utilities).
Every theme gets a complete light and dark Material 3 color scheme, including the
`surfaceContainer` roles used by dialogs, menus, sheets and the navigation dock.

Each entry sets:

- `seed` and `primaryChroma`: the primary hue and how vivid it stays.
- `secondary`, `tertiary`, `neutral` with their chroma: the supporting hues. The neutral
  hue tints backgrounds and surfaces, which is what makes themes feel different beyond
  their accent color.
- `darkPrimaryTone` / `darkSecondaryTone` (optional): a brighter tone than the Material
  default (80) for dark mode, so vivid themes do not turn pastel at night.
- `monochrome`: uses the grayscale Material scheme.

Regenerate `app/src/main/kotlin/com/tankobun/app/TankobunPalettes.kt` after editing:

```sh
npm ci && npm run generate
```

The unit test `TankobunThemeCatalogTest` checks text and control contrast for every
generated scheme.
