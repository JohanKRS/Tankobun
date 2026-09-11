# APK extension storage and Android removal

Research checked on 2026-09-08. Tankobun uses the community APK format; this
changes where the executable is stored, not the source IDs or repository format.

## Upstream findings

| Reader / revision | Android-installed APK | Private APK |
| --- | --- | --- |
| [Mihon aebf11a](https://github.com/mihonapp/mihon/blob/aebf11a74954fd82f81b8bce9f8ac4fc89fa6127/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionInstaller.kt#L171) | `ACTION_UNINSTALL_PACKAGE` and `REQUEST_DELETE_PACKAGES` in its manifest | Deletes its own extension file |
| [Tachiyomi v0.15.3, history retained in Mihon](https://github.com/mihonapp/mihon/blob/49991d38d97e21746dcc9e5f2c177bccb94c93b3/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionInstaller.kt#L214) | Same system uninstall action and permission | Private-file removal also present |
| [Aniyomi 4b5b90a](https://github.com/aniyomiorg/aniyomi/blob/4b5b90a3749b2c0504d4ffdd9416051d4730226c/app/src/main/java/eu/kanade/tachiyomi/extension/manga/util/MangaExtensionInstaller.kt#L221) | Same system uninstall action and permission | Private-file removal also present |

There is no permission-free system uninstall technique in those implementations.
Android's [PackageInstaller.uninstall API](https://developer.android.com/reference/android/content/pm/PackageInstaller#uninstall(java.lang.String,android.content.IntentSender))
also requires a deletion permission. Switching intents or APIs does not provide
a guarantee about Play Protect classification.

[Mihon's private extension loader](https://github.com/mihonapp/mihon/blob/aebf11a74954fd82f81b8bce9f8ac4fc89fa6127/app/src/main/java/eu/kanade/tachiyomi/extension/util/ExtensionLoader.kt#L27)
supports reading an APK from app-private storage without registering an Android
application. This is the approach applied here. No root, accessibility service,
hidden uninstaller, lowered target SDK, or disabled security scanning is involved.

## Tankobun behavior

- New APKs installed from the user-configured repository stay inside Tankobun.
  LNReader plugins already use app-private storage.
- Existing Android-installed extensions remain supported and keep their current
  update path until migrated. The **Installed** migration action copies the APK
  and selects its private copy, preserving its package, source IDs, preferences,
  source bindings and reading progress.
- When an Android copy remains, the phone action opens the official app-info
  page. The user decides whether to uninstall it; other readers may still use it.
  Private removal waits until the Android copy is removed to avoid a supposedly
  deleted extension reappearing through the shared installation.
- Package scanning, trust checks, loading, version checks, icons and update-all
  all resolve both storage locations. Equal versions with compatible signing
  identity prefer the explicitly migrated private copy; a newer Android version
  takes priority. A private APK cannot shadow a differently signed Android APK.
- Installation validates package, version, extension metadata and signing
  identity. APK paths include their digest; the active version is committed
  atomically. Old class loaders can finish on the preceding immutable path.
  Writable code is rejected, following [Android 14 DCL requirements](https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading).
- Parsed private APK metadata is cached by immutable path and file attributes,
  avoiding repeated APK parsing on source requests. Source execution still
  checks the current trust decision, including cached source instances.
- Executables live in `noBackupFilesDir`; library/settings backup retains
  identities and recognized preferences, not APKs. Repositories remain entirely
  user-configured. Removing Tankobun or clearing its data removes private APKs.

## Play Protect limit

The change does not add `REQUEST_DELETE_PACKAGES` and no longer needs it to
remove privately installed extensions. This addresses that permission requirement,
not every possible scanner verdict. [Google's guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)
distinguishes unknown-app scan prompts from harmful-app classifications; erroneous
harmful-app classifications can be appealed. An emulator functional test alone
cannot establish how every device or later scanner revision will classify an APK.

## Validation

`PrivateExtensionContract` uses original, signed, content-free fixtures for
private installation/update/removal, migration, stable source IDs, preferences,
library bindings, reading position, trust, read-only code and rejection of
corruption, a different signer and downgrades. A second pass runs after the
Android APK has been uninstalled and checks native preference UI, callbacks,
backup/restore, cache refresh and private removal. See the current artifact's
`dist/novel-reader/VALIDATION.md` for execution results and device evidence.
