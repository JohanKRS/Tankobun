# APK source preferences QA

`PreferenceSourceFactory.java` is an original, content-free two-source APK fixture.
It declares the community `ConfigurableSource` contract and exercises native
switches, lists, multiple selections, edit/password fields, sliders, callbacks,
validation, dependencies and nested screens. It is never part of the app artifact.

Build the app and Android tests with the separate `.novelqa` application ID, using
Android Studio's JBR and the project's Gradle wrapper. Build the fixture with
`python3 tools/apk-preferences-qa/build.py`; the script uses the existing SDK,
Gradle-cached libraries and Android debug key, and writes only under `/tmp`.

After installing the three test APKs on an emulator, run:

```sh
adb shell am instrument -w \
  -e apkPreferences eu.kanade.tachiyomi.extension.en.preferencesqa \
  com.tankobun.app.novelqa.test/com.tankobun.app.NovelCompatibilityInstrumentation
```

This checks the real dynamically loaded APK, native preference dialogs, callback
validation, password masking, source-instance refresh, isolation between source
IDs, backup/restore and restoration of an unsaved field after activity recreation.
The existing LNReader/cache/download/progress tests run in the same invocation.

For visual inspection of the actual settings button and screen, launch the
`com.tankobun.app.ApkPreferencesQaActivity` activity in the QA app after the test.
Remove both QA APKs and the fixture, reset any display overrides, and stop the
emulator when finished.

## Batch updates and responsive rows

The builder also accepts `--version`, `--package` and `--output`. Build versions
1 and 2 of the default package and of `eu.kanade.tachiyomi.extension.en.updatesqa`.
Install both version-1 APKs. Copy the version-2 files into the QA app's private
`files/update-qa/preferencesqa.apk` and `files/update-qa/updatesqa.apk` directories.
Allow the QA app to request APK installation, then launch
`com.tankobun.app.ExtensionUpdatesQaActivity`.

This debug-only activity uses the real source manager and MainViewModel with
original APK/JS fixtures supplied by an in-memory HTTP interceptor. It lists two
APK updates, one LNReader update, and an uninstalled entry that the batch must
leave alone. Confirm the first native installation, cancel the second, then
restart the remaining updates and rotate the device while the installer is open.
Check installed versions and the final counts. No new download may start before
the preceding installer finishes. Fixtures never enter the release APK.

Inspect Installed and Repository tabs in phone and tablet widths: installed
sources have identity above controls on phones and one row on tablets; repository
entries always use one row with only install or uninstall. Reset display changes,
uninstall both external APK fixtures and the QA app, and stop the emulator afterward.

## Private APKs and Android migration

Install only the default version-1 fixture in Android. Place its version-2 APK
and the second package's version-2 APK under `files/update-qa/` as above. Also
provide `corrupted.apk` (modify a signed APK without re-signing it) and
`wrong-signer.apk` (the same package signed with a separate disposable QA key).
Run the instrumentation with `-e privateExtensions before-removal`, uninstall
the default fixture from Android, then run with
`-e privateExtensions after-removal`.

The first pass copies the shared APK into private storage, updates it, rejects
corruption/signature changes/downgrades, and installs/removes a private-only APK.
The second pass verifies loading after Android removal, native settings and
backup/restore. Both assert stable source IDs, preferences, library bindings and
reading progress; executable files must be immutable and excluded from backup.

For UI verification, reinstall the version-1 fixture and open
`ExtensionUpdatesQaActivity`: copy from Installed, use its phone button to open
Android app info, uninstall the Android copy and return. Confirm the source still
works, then update all. Install and remove the second fixture from Repository;
private APK operations must not launch the Android package installer/uninstaller.
Use original fixtures only; none are distributed with Tankobun.
