package com.tankobun.app

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tankobun.app.backup.AppSettingsBackupDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.CountDownLatch

internal suspend fun Instrumentation.checkRepositoryManagement() {
    val container = (targetContext.applicationContext as TankobunApplication).container
    val store = container.settingsStore
    val first = "https://example.invalid/panels/index.json"
    val second = "https://example.invalid/novels/index.json"
    val prefs = targetContext.getSharedPreferences("tankobun_settings", Context.MODE_PRIVATE)
    // Existing single-repository installations keep the saved index, but show an empty add field.
    prefs.edit().remove("extension.repositories").putString("extension.repository.url", first).commit()
    check(store.extensionRepositoryUrl().isEmpty() && store.extensionRepositories() == listOf(first))
    store.saveExtensionRepositoryUrl(second)
    check(store.extensionRepositories() == listOf(first) && store.extensionRepositoryUrl() == second)
    store.saveExtensionRepositories(emptyList())
    store.saveExtensionRepositoryUrl("")

    fun launch() = startActivitySync(Intent(targetContext, RepositoryQaActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as RepositoryQaActivity
    var activity = launch()
    var vm = activity.model
    suspend fun waitLoaded() = withTimeout(15_000) { while (vm.state.value.extensionRepositoryLoading) delay(25) }
    suspend fun add(url: String) {
        runOnMainSync { vm.setExtensionRepositoryUrl(url); vm.addExtensionRepository() }
        waitLoaded()
    }
    RepositoryQaActivity.requests.clear()
    add(first)
    check(vm.state.value.extensionRepositoryUrl.isEmpty() && store.extensionRepositoryUrl().isEmpty())
    check(vm.state.value.extensionRepositories == listOf(first))
    val firstRequests = RepositoryQaActivity.requests.size
    add(second)
    check(RepositoryQaActivity.requests.drop(firstRequests) == listOf("/novels/index.json"))
    check(vm.state.value.extensionRepositories == listOf(first, second))
    check(vm.state.value.availableExtensions.size == 2 && vm.state.value.extensionRepositoryUrl.isEmpty())
    val manga = vm.state.value.availableExtensions.first { it.repositoryUrl == first }
    check(vm.extensionIconUrl(manga) == "https://example.invalid/panels/icon/${manga.packageName}.png")
    check(vm.extensionApkUrl(manga) == "https://example.invalid/panels/apk/paper.apk")
    add("https://example.invalid/missing/index.json")
    check(vm.state.value.extensionRepositoryUrl.endsWith("missing/index.json"))
    check(vm.state.value.extensionRepositories.size == 2 && vm.state.value.availableExtensions.size == 2)
    add("https://example.invalid/alias.json")
    check(vm.state.value.extensionRepositories.toSet() == setOf(first, second))
    check(vm.state.value.extensionRepositories.size == 2 && vm.state.value.extensionRepositoryUrl.isEmpty())

    RepositoryQaActivity.gate = CountDownLatch(1)
    runOnMainSync {
        vm.setExtensionRepositoryUrl("https://example.invalid/slow/index.json")
        vm.addExtensionRepository()
        vm.setExtensionRepositoryUrl("https://example.invalid/draft/index.json")
    }
    RepositoryQaActivity.gate!!.countDown()
    waitLoaded()
    check(vm.state.value.extensionRepositoryUrl.endsWith("draft/index.json"))
    runOnMainSync { vm.removeExtensionRepository("https://example.invalid/slow/index.json") }
    RepositoryQaActivity.requests.clear()
    runOnMainSync { vm.refreshExtensionIndex() }
    waitLoaded()
    check(RepositoryQaActivity.requests.toSet() == setOf("/panels/index.json", "/novels/index.json"))
    check(vm.state.value.extensionRepositoryUrl.endsWith("draft/index.json"))
    runOnMainSync { vm.setExtensionRepositoryUrl("") }
    val backup = AppSettingsBackupDataSource(container)
    val file = File(targetContext.cacheDir, "repository-management-qa.json")
    backup.saveBackup(Uri.fromFile(file), vm.state.value)
    store.saveExtensionRepositories(emptyList())
    backup.restoreBackup(Uri.fromFile(file))
    check(store.extensionRepositories().toSet() == setOf(first, second) && store.extensionRepositoryUrl().isEmpty())
    file.delete()
    runOnMainSync { activity.finish() }
    RepositoryQaActivity.requests.clear()
    activity = launch()
    vm = activity.model
    waitLoaded()
    check(vm.state.value.extensionRepositoryUrl.isEmpty())
    check(vm.state.value.availableExtensions.size == 2)
    check(vm.state.value.extensionRepositories.toSet() == setOf(first, second))
    runOnMainSync { vm.removeExtensionRepository(first) }
    check(vm.state.value.extensionRepositories == listOf(second))
    check(vm.state.value.availableExtensions.single().repositoryUrl == second)
    // Restore two listed repositories for the manual phone/tablet visual check.
    add(first)
    println("PASS: repository add/clear, failure retention, no refetch on add, edited draft retention, canonical deduplication, icons/APKs with empty input, refresh saved only, restart, legacy settings, backup/restore and removal")
}
