package com.tankobun.app

import android.app.Application
import android.net.Uri
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.DeDupeConcurrentRequestStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.tankobun.core.anilist.AnilistGraphQlClient
import com.tankobun.core.anilist.AnilistRepository
import com.tankobun.core.database.DatabaseFactory
import com.tankobun.core.database.toDownloadPageEntity
import com.tankobun.core.database.toModel
import com.tankobun.core.downloads.DownloadCoordinator
import com.tankobun.core.downloads.DownloadPageFetcher
import com.tankobun.core.downloads.DownloadPageStorage
import com.tankobun.core.downloads.DownloadTaskRunner
import com.tankobun.core.downloads.DownloadWorkerDelegateRegistry
import com.tankobun.core.downloads.RoomDownloadStateStore
import com.tankobun.core.extensions.ExtensionIndexRepository
import com.tankobun.core.extensions.InstalledExtensionScanner
import com.tankobun.core.extensions.SourceMatcher
import com.tankobun.core.extensions.TachiyomiSourceHost
import com.tankobun.core.model.DownloadJob
import com.tankobun.core.model.DownloadState
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import com.tankobun.core.network.RespectfulRateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import uy.kohesive.injekt.TankobunInjektRegistry
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TankobunApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    @OptIn(ExperimentalCoilApi::class)
    override fun onCreate() {
        super.onCreate()
        TankobunInjektRegistry.registerApplication(this)
        val appContainer = container
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(it)
                .components {
                    add(ReaderPageFetcher.Factory(appContainer))
                    add(ReaderPageImageModelKeyer())
                    add(SourceThumbnailFetcher.Factory(appContainer))
                    add(SourceThumbnailImageModelKeyer())
                    add(
                        OkHttpNetworkFetcherFactory(
                            callFactory = { appContainer.imageOkHttpClient },
                            concurrentRequestStrategy = { DeDupeConcurrentRequestStrategy() },
                        ),
                    )
                }
                .build()
        }
        ScheduledBackupWork.sync(this, container.settingsStore.backupSchedule())
        NewChapterCheckWork.sync(this, container.settingsStore.newChapterChecksEnabled())
    }
}

class AppContainer(application: Application) {
    val application: Application = application
    val database = DatabaseFactory.create(application)

    private val cacheDir = File(application.cacheDir, "http").also { it.mkdirs() }
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .cache(Cache(cacheDir, 128L * 1024L * 1024L))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val imageOkHttpClient: OkHttpClient by lazy {
        // Coil owns its disk cache. Share OkHttp's dispatcher and connection pool,
        // but avoid storing every image a second time in the API response cache.
        okHttpClient.newBuilder()
            .cache(null)
            .build()
    }

    val tokenStore = SecureTokenStore(application)
    val settingsStore = SettingsStore(application)

    fun string(@StringRes id: Int, vararg args: Any): String =
        application.getAppString(settingsStore.appLanguage(), id, *args)

    fun quantityString(@PluralsRes id: Int, quantity: Int, vararg args: Any): String =
        application.getAppQuantityString(settingsStore.appLanguage(), id, quantity, *args)

    val anilistRepository = AnilistRepository(
        AnilistGraphQlClient(
            okHttpClient = okHttpClient,
            rateLimiter = RespectfulRateLimiter(
                minSpacingMillis = 2_200L,
                serverRateLimitWindowMillis = 60_000L,
                targetUtilization = 0.9,
            ),
            userAgent = tankobunAniListUserAgent(),
        ),
    )

    val extensionRepository = ExtensionIndexRepository(
        okHttpClient = okHttpClient,
        rateLimiter = RespectfulRateLimiter(minSpacingMillis = 1_500L),
    )

    val extensionScanner = InstalledExtensionScanner(application)
    val extensionTrustStore = com.tankobun.core.extensions.ExtensionTrustStore(application)
    val sourceHost = TachiyomiSourceHost(application, extensionTrustStore)
    val sourceMatcher = SourceMatcher()

    val downloadCoordinator = DownloadCoordinator(
        context = application,
        downloadDao = database.downloadDao(),
        downloadPageDao = database.downloadPageDao(),
        deleteFilesForJobs = { jobIds -> deleteDownloadedFilesForJobs(jobIds) },
        deleteFilesForMedia = { mediaId -> deleteDownloadedFilesForMedia(mediaId) },
        deleteFilesForMediaSource = { mediaId, sourceId -> deleteDownloadedFilesForMediaSource(mediaId, sourceId) },
        deleteAllFiles = { deleteAllDownloadedFiles() },
    )
    private val downloadSemaphores = ConcurrentHashMap<Long, Semaphore>()

    init {
        DownloadWorkerDelegateRegistry.delegate = { jobId -> runDownloadJob(jobId) }
    }

    private suspend fun runDownloadJob(jobId: String): Boolean {
        val jobRow = database.downloadDao().getDownload(jobId) ?: return true
        if (jobRow.state != DownloadState.QUEUED && jobRow.state != DownloadState.RUNNING) {
            return true
        }
        val job = jobRow.toModel()
        val source = resolveInstalledSource(job.sourceId)
        if (source == null) {
            RoomDownloadStateStore(database.downloadDao()).markFailed(job.id, "Source is not installed")
            return true
        }
        val chapter = database.chapterDao().cachedChapterByUrl(job.chapterUrl)?.toModel()
            ?: SourceChapter(
                sourceId = job.sourceId,
                mangaUrl = job.mangaUrl,
                url = job.chapterUrl,
                name = job.chapterName,
                chapterNumber = 0f,
                scanlator = null,
                uploadedAtEpochMillis = null,
            )
        val semaphore = downloadSemaphores.getOrPut(job.sourceId) { Semaphore(DOWNLOAD_SOURCE_CONCURRENCY) }
        semaphore.withPermit {
            DownloadTaskRunner(
                pageFetcher = sourcePageFetcher(source, chapter, job),
                pageStorage = filePageStorage(),
                stateStore = RoomDownloadStateStore(database.downloadDao()),
            ).run(job)
        }
        return true
    }

    private fun sourcePageFetcher(
        source: SourceDescriptor,
        chapter: SourceChapter,
        downloadJob: DownloadJob,
    ): DownloadPageFetcher =
        object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob): List<ReaderPage> =
                sourceHost.pages(source, chapter)

            override suspend fun bytes(page: ReaderPage): ByteArray =
                downloadPageBytes(source, downloadJob, chapter, page)
        }

    private fun filePageStorage(): DownloadPageStorage =
        object : DownloadPageStorage {
            override suspend fun storedPageIndexes(job: DownloadJob): Set<Int> =
                database.downloadPageDao()
                    .pagesForJob(job.id)
                    .asSequence()
                    .filter { File(it.filePath).let { file -> file.isFile && file.length() > 0L } }
                    .map { it.pageIndex }
                    .toSet()

            override suspend fun writePage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String =
                writeDownloadedPage(job, page, bytes)
        }

    private suspend fun downloadPageBytes(
        source: SourceDescriptor,
        job: DownloadJob,
        chapter: SourceChapter,
        page: ReaderPage,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            ReaderPageCache.cachedOrFetch(
                context = application,
                mediaId = job.mediaId,
                chapter = chapter,
                page = page,
            ) {
                // DownloadTaskRunner owns retry/backoff here. Avoid multiplying its
                // attempts by the reader-oriented retries inside the source host.
                sourceHost.imageBytes(source, page, maxAttempts = 1)
            }.bytes
        }

    private suspend fun writeDownloadedPage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val chapterDir = File(
                downloadsRoot(),
                "${job.mediaId}/${job.sourceId}/${stableFileKey(job.chapterUrl)}",
            ).also { it.mkdirs() }
            val file = File(chapterDir, "${page.index.toString().padStart(4, '0')}.${pageFileExtension(page.imageUrl)}")
            val partial = File(chapterDir, "${file.name}.part")
            partial.writeBytes(bytes)
            if (file.exists()) file.delete()
            check(partial.renameTo(file)) { "Could not store page ${page.index + 1}" }
            val filePath = file.absolutePath
            database.downloadPageDao().upsertPage(
                page.toDownloadPageEntity(
                    job = job,
                    filePath = filePath,
                    updatedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
            filePath
        }

    private suspend fun deleteDownloadedFilesForJobs(jobIds: List<String>) =
        withContext(Dispatchers.IO) {
            val paths = jobIds.flatMap { jobId ->
                database.downloadPageDao().pagesForJob(jobId).map { it.filePath }
            }
            deleteDownloadedFiles(paths)
        }

    private suspend fun deleteDownloadedFilesForMedia(mediaId: Int) =
        withContext(Dispatchers.IO) {
            deleteDownloadedFiles(database.downloadPageDao().pagesForMedia(mediaId).map { it.filePath })
        }

    private suspend fun deleteDownloadedFilesForMediaSource(mediaId: Int, sourceId: Long) =
        withContext(Dispatchers.IO) {
            deleteDownloadedFiles(database.downloadPageDao().pagesForMediaSource(mediaId, sourceId).map { it.filePath })
        }

    private suspend fun deleteAllDownloadedFiles() =
        withContext(Dispatchers.IO) {
            downloadsRoot().deleteRecursively()
            downloadsRoot().mkdirs()
        }

    private fun deleteDownloadedFiles(paths: List<String>) {
        val root = downloadsRoot().canonicalFile
        val rootPath = root.path + File.separator
        paths.forEach { path ->
            val file = runCatching { File(path).canonicalFile }.getOrNull() ?: return@forEach
            if (!file.path.startsWith(rootPath) || !file.exists()) return@forEach
            file.delete()
            deleteEmptyDownloadParents(file.parentFile, root)
        }
    }

    private fun deleteEmptyDownloadParents(start: File?, root: File) {
        var parent = start
        while (parent != null && parent != root) {
            val canonical = runCatching { parent.canonicalFile }.getOrNull() ?: return
            if (!canonical.path.startsWith(root.path + File.separator)) return
            if (canonical.list()?.isNotEmpty() == true) return
            if (!canonical.delete()) return
            parent = canonical.parentFile
        }
    }

    private fun downloadsRoot(): File =
        File(application.filesDir, "downloads").also { it.mkdirs() }

    private fun resolveInstalledSource(sourceId: Long): SourceDescriptor? {
        extensionScanner.installedExtensions().forEach { extension ->
            val source = runCatching {
                sourceHost.loadSources(extension.packageName).firstOrNull { it.id == sourceId }
            }.getOrNull() ?: return@forEach
            return extension.copy(
                id = source.id,
                name = source.name,
                lang = source.lang,
            )
        }
        return null
    }

    private fun stableFileKey(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { "%02x".format(it) }.take(24)
    }

    private fun pageFileExtension(url: String): String {
        val extension = Uri.parse(url).lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase(Locale.ROOT)
        return extension?.takeIf { it in DOWNLOAD_IMAGE_EXTENSIONS } ?: "jpg"
    }

    companion object {
        private const val DOWNLOAD_SOURCE_CONCURRENCY = 1
        private val DOWNLOAD_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    }
}

private fun tankobunAniListUserAgent(): String =
    "Tankobun/${BuildConfig.VERSION_NAME} (Android; ${BuildConfig.APPLICATION_ID}; $TANKOBUN_GITHUB_URL)"

private const val TANKOBUN_GITHUB_URL = "https://github.com/JohanKRS/Tankobun"
