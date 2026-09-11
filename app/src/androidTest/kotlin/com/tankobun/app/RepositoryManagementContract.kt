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
    val beforeRename = RepositoryQaActivity.requests.size
    runOnMainSync { vm.renameExtensionRepository(first, "  Paper Panels  ") }
    check(vm.state.value.extensionRepositoryNames == mapOf(first to "Paper Panels"))
    check(store.extensionRepositoryNames() == vm.state.value.extensionRepositoryNames)
    check(RepositoryQaActivity.requests.size == beforeRename)
    val firstRequests = RepositoryQaActivity.requests.size
    add(second)
    check(RepositoryQaActivity.requests.drop(firstRequests) == listOf("/novels/index.json"))
    check(vm.state.value.extensionRepositories == listOf(first, second))
    check(vm.state.value.availableExtensions.size == 2 && vm.state.value.extensionRepositoryUrl.isEmpty())
    runOnMainSync { vm.renameExtensionRepository(second, "Paper Words") }
    val expectedNames = mapOf(first to "Paper Panels", second to "Paper Words")
    check(vm.state.value.extensionRepositoryNames == expectedNames)
    val beforeVisibility = vm.state.value
    val visibilityRequests = RepositoryQaActivity.requests.size
    runOnMainSync {
        vm.setExtensionRepositoryVisible(first, false)
        vm.setExtensionRepositoryVisible(second, false)
    }
    check(vm.state.value.hiddenExtensionRepositories == setOf(first, second))
    check(store.hiddenExtensionRepositories() == setOf(first, second))
    check(vm.state.value.availableExtensions == beforeVisibility.availableExtensions)
    check(vm.state.value.installedSources == beforeVisibility.installedSources)
    check(vm.state.value.disabledSourceKeys == beforeVisibility.disabledSourceKeys)
    check(RepositoryQaActivity.requests.size == visibilityRequests)
    runOnMainSync { vm.setExtensionRepositoryVisible(second, true) }
    check(vm.state.value.hiddenExtensionRepositories == setOf(first))
    val manga = vm.state.value.availableExtensions.first { it.repositoryUrl == first }
    check(vm.extensionIconUrl(manga) == "https://example.invalid/panels/icon/${manga.packageName}.png")
    check(vm.extensionApkUrl(manga) == "https://example.invalid/panels/apk/paper.apk")
    val mirror = "https://example.invalid/mirror/index.json"
    add(mirror)
    val sharedPackage = vm.state.value.availableExtensions.filter { it.packageName == manga.packageName }
    check(sharedPackage.map { it.repositoryUrl }.toSet() == setOf(first, mirror))
    check(vm.extensionApkUrl(sharedPackage.single { it.repositoryUrl == mirror }) == "https://example.invalid/mirror/apk/mirror.apk")
    runOnMainSync { vm.removeExtensionRepository(mirror) }
    check(vm.state.value.availableExtensions.single { it.packageName == manga.packageName } == manga)
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
    check(vm.state.value.extensionRepositoryNames == expectedNames)
    val backup = AppSettingsBackupDataSource(container)
    check(vm.state.value.hiddenExtensionRepositories == setOf(first))
    val file = File(targetContext.cacheDir, "repository-management-qa.json")
    backup.saveBackup(Uri.fromFile(file), vm.state.value)
    store.saveExtensionRepositories(emptyList())
    backup.restoreBackup(Uri.fromFile(file))
    check(store.extensionRepositories().toSet() == setOf(first, second) && store.extensionRepositoryUrl().isEmpty())
    check(store.extensionRepositoryNames() == expectedNames)
    check(store.hiddenExtensionRepositories() == setOf(first))
    // Backups created before names existed remain readable and keep matching local names.
    val legacy = org.json.JSONObject(file.readText()).apply {
        getJSONObject("sources").remove("extensionRepositoryNames")
        getJSONObject("sources").remove("hiddenExtensionRepositories")
    }
    file.writeText(legacy.toString())
    backup.restoreBackup(Uri.fromFile(file))
    check(store.extensionRepositoryNames() == expectedNames)
    file.delete()
    runOnMainSync { activity.finish() }
    RepositoryQaActivity.requests.clear()
    activity = launch()
    vm = activity.model
    waitLoaded()
    check(vm.state.value.extensionRepositoryUrl.isEmpty())
    check(vm.state.value.availableExtensions.size == 2)
    check(vm.state.value.extensionRepositories.toSet() == setOf(first, second))
    check(vm.state.value.extensionRepositoryNames == expectedNames)
    check(vm.state.value.hiddenExtensionRepositories == setOf(first))
    runOnMainSync { vm.removeExtensionRepository(first) }
    check(vm.state.value.extensionRepositories == listOf(second))
    check(vm.state.value.extensionRepositoryNames == mapOf(second to "Paper Words"))
    check(vm.state.value.hiddenExtensionRepositories.isEmpty())
    check(vm.state.value.availableExtensions.single().repositoryUrl == second)
    // Restore two listed repositories for the manual phone/tablet visual check.
    add(first)
    runOnMainSync { vm.renameExtensionRepository(second, "   ") }
    check(second !in store.extensionRepositoryNames())
    // A saved descriptor can later resolve to an index URL; keep its user-defined name.
    runOnMainSync { vm.removeExtensionRepository(first) }
    val alias = "https://example.invalid/alias.json"
    store.saveExtensionRepositories(listOf(second, alias))
    store.saveExtensionRepositoryNames(mapOf(alias to "My panels"))
    store.saveHiddenExtensionRepositories(setOf(alias))
    runOnMainSync { activity.finish() }
    activity = launch()
    vm = activity.model
    waitLoaded()
    check(vm.state.value.extensionRepositoryNames == mapOf(first to "My panels"))
    check(vm.state.value.extensionRepositories.toSet() == setOf(first, second))
    check(vm.state.value.hiddenExtensionRepositories == setOf(first))
    runOnMainSync { vm.setExtensionRepositoryVisible(first, true) }
    println("PASS: repository add/clear, failure retention, no refetch on add, edited draft retention, canonical deduplication, icons/APKs with empty input, refresh saved only, restart, legacy settings, backup/restore and removal")
}
