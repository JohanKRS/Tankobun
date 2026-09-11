package com.tankobun.app

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import com.tankobun.app.backup.AppSettingsBackupDataSource
import com.tankobun.app.reader.ReaderDataSource
import com.tankobun.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File

internal suspend fun Instrumentation.checkNovelReader() {
    val container = (targetContext.applicationContext as TankobunApplication).container
    val store = container.settingsStore
    val previous = store.novelReaderPreferences()
    store.saveNovelReaderPreferences(NovelReaderPreferences(continuousReading = true))
    val activity = startActivitySync(Intent(targetContext, NovelReaderQaActivity::class.java).putExtra("contract", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as NovelReaderQaActivity
    val vm = activity.model
    suspend fun ready() = withTimeout(10_000) {
        while (vm.state.value.readerPages.isEmpty() || vm.state.value.readerNextSegment == null || vm.state.value.readerPreviousSegment == null) delay(25)
    }
    val reader = ReaderDataSource(container)
    try {
        ready()
        check(vm.state.value.activeChapter!!.chapterNumber == 2f)
        check(vm.state.value.allInstalledSources.isEmpty())
        val second = vm.state.value.activeChapter!!
        val third = vm.state.value.readerNextSegment!!.chapter
        runOnMainSync { vm.setNovelReaderPosition(second.url, 3, 37); vm.persistReaderProgress() }
        withTimeout(5_000) { while (reader.cachedProgressForChapter(199199, second.url)?.pageScrollOffset != 37) delay(25) }
        runOnMainSync { vm.setNovelReaderPosition(third.url, 2, 16) }
        ready()
        check(vm.state.value.activeChapter!!.url == third.url)
        check(vm.state.value.readerPreviousSegment!!.chapter.url == second.url)
        check(vm.state.value.readerNextSegment!!.chapter.chapterNumber == 4f)
        withTimeout(5_000) { while (reader.cachedProgressForChapter(199199, second.url)?.completed != true) delay(25) }
        runOnMainSync { vm.setNovelReaderPosition(second.url, 4, 29) }
        ready()
        check(vm.state.value.activeChapter!!.url == second.url)
        check(vm.state.value.currentPageIndex == 4 && vm.state.value.currentPageScrollOffset == 29)
        check(vm.state.value.readerPreviousSegment!!.chapter.chapterNumber == 1f)
        val preferences = NovelReaderPreferences(readingMode = NovelReadingMode.PAGED, continuousReading = false, maxTextWidth = 540, landscapeTwoPages = false, fontSize = 26, theme = NovelTheme.SEPIA)
        runOnMainSync { vm.setNovelReaderPreferences(preferences); vm.persistReaderProgress() }
        withTimeout(5_000) { while (reader.cachedProgressForChapter(199199, second.url)?.pageScrollOffset != 29) delay(25) }
        runOnMainSync { vm.closeReader(); vm.openChapter(second) }
        withTimeout(5_000) { while (vm.state.value.readerPages.isEmpty()) delay(25) }
        check(vm.state.value.currentPageIndex == 4 && vm.state.value.currentPageScrollOffset == 29)
        check(vm.state.value.readerNextSegment == null && vm.state.value.readerPreviousSegment == null)
        check(SettingsStore(targetContext).novelReaderPreferences() == preferences)
        val file = File(targetContext.cacheDir, "novel-reader-preferences.json")
        val backup = AppSettingsBackupDataSource(container)
        backup.saveBackup(Uri.fromFile(file), vm.state.value)
        store.saveNovelReaderPreferences(NovelReaderPreferences())
        backup.restoreBackup(Uri.fromFile(file))
        check(SettingsStore(targetContext).novelReaderPreferences() == preferences)
        file.delete()
    } finally {
        store.saveNovelReaderPreferences(previous)
        runOnMainSync { vm.closeReader(); activity.finish() }
    }
}
