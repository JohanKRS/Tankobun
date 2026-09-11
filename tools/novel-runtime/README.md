# Novel compatibility runtime

This directory builds the software libraries provided to user-installed LNReader
CommonJS plugins. It contains no reading sources, repository index or source code
from a reading website. Runtime assets and dependency notices are checked in so
Android builds do not require Node.

Rebuild with the project's Node environment:

```sh
npm ci
npm test
npm run build
```

The host uses native HTTP/cookies/cache and keeps storage scoped to the installed
plugin. Each call gets a disposable WebView, with direct navigation, file access
and WebView network requests blocked. The adapter supports the documented modules,
chapter-list pagination and image headers. Plugin-defined text, switch, select and
checkbox settings are available in the source manager. Their defaults are applied
before methods run, including settings read by class initializers.

Chapter scripts use the standard `#LNReader-chapter` DOM container and DOM load
events. Source-provided CSS is applied while processing content. Scripts and their
asynchronous work must finish before the document is sanitized for native reading;
network work uses the host's native request bridge. The resulting document is used
by both cache and offline downloads. User-selected fonts and themes remain native.

The separate, visible website browser has no native JavaScript interface. It
captures only main-frame storage from the source's origin, never from unrelated
login redirects. Plugin APIs receive the matching origin's local/session snapshot.
Switches and selection preferences are portable through settings backup; arbitrary
text fields, cookies and website sessions remain on the device.

## Android validation

From the project root, use Android Studio's bundled JBR and the Gradle wrapper:

```sh
bash ./gradlew :app:testDebugUnitTest :core:extensions:testDebugUnitTest \
  :core:reader:test :core:downloads:testDebugUnitTest :app:lintDebug \
  :app:assembleRelease --max-workers=2

bash ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
  -PqaApplicationIdSuffix=.novelqa --max-workers=2
```

Install the two debug APKs on a test emulator, then run:

```sh
adb shell am instrument -w \
  com.tankobun.app.novelqa.test/com.tankobun.app.NovelCompatibilityInstrumentation
adb shell am start -n \
  com.tankobun.app.novelqa/com.tankobun.app.NovelReaderQaActivity --ei block 9
adb shell am start -n \
  com.tankobun.app.novelqa/com.tankobun.app.NovelPluginQaActivity
```

The separate application ID protects the normal app's library. Device tests use
original fictional text to exercise the real runtime, source host, chapter order,
network bridge, persistent storage, chapter scripts, website storage isolation,
cache, download worker, offline reading, character position and plugin/settings
backup/restore. Installed NovelSource APKs are
also checked for initialization and content type. No third-party chapter needs
to be downloaded for these tests. The debug activity checks phone/tablet layout,
themes and restoration after changing typography or window size. The plugin QA
activity provides original settings and a local fictional sign-in page for
checking all four setting types and browser-session capture. Both activities are
excluded from release builds. Shut down test emulators when finished.
