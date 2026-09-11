# Security regression fixtures

`build.py` uses the existing Android SDK, Android Studio JBR and Android debug key to build inert APKs in `/tmp/tankobun-security-qa/fixtures`. A temporary key and signing lineage are generated only in that directory. No fixture contains source content or executable code.

Build the separate QA application and instrumentation with `bash ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -PqaApplicationIdSuffix=.novelqa --max-workers=2`, using Android Studio's JBR as `JAVA_HOME`. Install both APKs on an emulator. Copy the ten named fixture APKs (`valid`, `rotated`, `wrong-package`, `downgrade`, `wrong-version`, `wrong-name`, `wrong-signer`, `unsigned`, `tampered`, `multiple-signers`) into that application's `files/security-fixtures` directory.

Run `adb -s SERIAL shell am instrument -w -e security true com.tankobun.app.novelqa.test/com.tankobun.app.NovelCompatibilityInstrumentation`. The test refuses the ordinary app package. It exercises secure-preference outage/recovery/provider revocation, plaintext absence, a blocked provider pipe, and actual Android archive/signing verification including forward key rotation. Run the same instrumentation with `-e repositories true` for repository addition, alias refresh, failed add, names, visibility and backup restoration.

`RepositoryQaActivity` also accepts `--ez identityChangeFixture true` to show the signing-identity review with fictional data. Unit tests in `core:network`, `core:extensions` and `app` cover TLS downgrade rejection, encoded and expanded limits, malformed gzip headers, cancellation, deadlines, partial cleanup, repository identity changes and app APK metadata checks. After emulator QA, shut down the emulator to release memory.
