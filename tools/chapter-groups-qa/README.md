# Chapter translation-group checks

All fixture titles, groups and text are fictional. `ChapterGroupsQaActivity` is
debug-only and requires the isolated `.novelqa` application ID. No reading source
or fixture content is included in release builds.

Run the JVM contracts with `:app:testDebugUnitTest`,
`:core:extensions:testDebugUnitTest`, and `:core:database:testDebugUnitTest`.

For an emulator, build `:app:assembleDebug :app:assembleDebugAndroidTest` with
`-PqaApplicationIdSuffix=.novelqa`, then install both resulting APKs. Push
`core/database/schemas/com.tankobun.core.database.TankobunDatabase/15.json` to
`/data/local/tmp/tankobun-schema15.json` and run:

```sh
adb -s emulator-5554 shell am instrument -w \
  -e chapterGroups true \
  -e schema15 /data/local/tmp/tankobun-schema15.json \
  com.tankobun.app.novelqa.test/com.tankobun.app.NovelCompatibilityInstrumentation
```

The contract checks Room's actual 15-to-16 migration with a populated legacy row,
cached credit metadata, preferred-group fallback, offline next-chapter navigation,
read-state handling across editions, persisted preferences, and settings backup
restoration. It restores the prior group preferences afterward.

For visual checks, open `com.tankobun.app.ChapterGroupsQaActivity` in that isolated
app. Aurora supplies chapters 1 and 3; Brisa supplies 1, 2 and 3. Choosing Aurora
must display Aurora / Brisa / Aurora and count three chapters. Disabling grouping
must restore five releases. Check the small credit line, the Translations dialog,
and that the header stays on one line on a phone. Shut down the emulator afterward.
