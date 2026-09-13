package com.tankobun.app

import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.room.migration.Migration
import com.tankobun.app.backup.AppSettingsBackupDataSource
import com.tankobun.app.logic.*
import com.tankobun.core.database.DatabaseFactory
import com.tankobun.core.database.TankobunDatabase
import com.tankobun.core.database.toEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.model.*
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

internal suspend fun Instrumentation.checkChapterGroups(schema15: String) {
    checkChapterGroupMigration(schema15)
    val container = (targetContext.applicationContext as TankobunApplication).container
    val store = container.settingsStore
    val saved = store.chapterGroupPreferences()
    store.saveChapterGroupPreferences(emptyMap())
    val activity = startActivitySync(Intent(targetContext, ChapterGroupsQaActivity::class.java)
        .putExtra("contract", true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ChapterGroupsQaActivity
    val vm = activity.model
    suspend fun await(condition: () -> Boolean) = withTimeout(10_000) { while (!condition()) delay(25) }
    try {
        await { vm.state.value.sourceChapters.size == 5 }
        check(vm.state.value.readingChapters.size == 5)
        runOnMainSync { vm.setChapterGroupPreference(ChapterGroupPreference(true, "Equipe Aurora")) }
        val selected = vm.state.value.readingChapters.readingOrder()
        check(selected.map { it.scanlator } == listOf("Equipe Aurora", "Equipe Brisa", "Equipe Aurora"))
        check(SettingsStore(targetContext).chapterGroupPreferences() == vm.state.value.chapterGroupPreferences)
        check(container.database.chapterDao().cachedChapters(992, "paper").size == 5)
        runOnMainSync { vm.openChapter(selected[0]) }
        await { vm.state.value.readerPages.isNotEmpty() }
        runOnMainSync { vm.openNextChapter() }
        await { vm.state.value.activeChapter?.url == selected[1].url && vm.state.value.readerPages.isNotEmpty() }
        runOnMainSync { vm.openNextChapter() }
        await { vm.state.value.activeChapter?.url == selected[2].url && vm.state.value.readerPages.isNotEmpty() }
        runOnMainSync { vm.closeReader(); vm.setChapterRead(selected[0], true) }
        await { vm.state.value.isChapterRead(selected[0]) }
        runOnMainSync { vm.setChapterGroupPreference(ChapterGroupPreference(true, "Equipe Brisa")) }
        val firstBrisa = vm.state.value.readingChapters.readingOrder().first()
        check(vm.state.value.isChapterRead(firstBrisa))
        runOnMainSync { vm.setChapterRead(firstBrisa, false) }
        await { !vm.state.value.isChapterRead(firstBrisa) }
        check(container.database.progressDao().progressForChapter(199299, selected[0].url) == null)
        val file = File(targetContext.cacheDir, "chapter-groups-backup.json")
        val backup = AppSettingsBackupDataSource(container)
        backup.saveBackup(Uri.fromFile(file), vm.state.value)
        val expected = store.chapterGroupPreferences()
        store.saveChapterGroupPreferences(emptyMap())
        backup.restoreBackup(Uri.fromFile(file))
        check(SettingsStore(targetContext).chapterGroupPreferences() == expected)
        file.delete()
    } finally {
        store.saveChapterGroupPreferences(saved)
        runOnMainSync { vm.closeReader(); activity.finish() }
    }
}

private suspend fun Instrumentation.checkChapterGroupMigration(schema15: String) {
    val databaseName = "chapter-group-migration-qa.db"
    targetContext.deleteDatabase(databaseName)
    val schema = JSONObject(File(schema15).readText()).getJSONObject("database")
    targetContext.openOrCreateDatabase(databaseName, 0, null).use { db ->
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val name = entity.getString("tableName")
            db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", name))
            val indexes = entity.optJSONArray("indices") ?: org.json.JSONArray()
            for (j in 0 until indexes.length()) db.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", name))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
        db.execSQL("INSERT INTO source_chapters VALUES (992, 'paper', 'old', 'Chapter 1', 1, 'Legacy team', NULL, 1, NULL)")
        db.version = 15
    }
    val migration = DatabaseFactory::class.java.declaredMethods.first { it.name.startsWith("getMIGRATION_15_16") }
        .invoke(DatabaseFactory) as Migration
    val db = Room.databaseBuilder(targetContext, TankobunDatabase::class.java, databaseName).addMigrations(migration).build()
    try {
        val migrated = db.chapterDao().cachedChapters(992, "paper").single().toModel()
        check(migrated.scanlator == "Legacy team" && migrated.volume == null && migrated.scanlators.isEmpty())
        val updated = migrated.copy(volume = "2", chapterNumberText = "1.5", scanlators = listOf("Aurora", "Brisa"))
        db.chapterDao().upsertChapters(listOf(updated.toEntity(2)))
        check(db.chapterDao().cachedChapters(992, "paper").single().toModel() == updated)
    } finally { db.close(); targetContext.deleteDatabase(databaseName) }
}
