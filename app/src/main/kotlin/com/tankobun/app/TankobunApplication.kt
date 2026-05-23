package com.tankobun.app

import android.app.Application
import android.net.Uri
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
import okhttp3.Request
import uy.kohesive.injekt.TankobunInjektRegistry
import java.io.File
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TankobunApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        TankobunInjektRegistry.registerApplication(this)
        container
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

    val tokenStore = SecureTokenStore(application)
    val settingsStore = SettingsStore(application)

    val anilistRepository = AnilistRepository(
        AnilistGraphQlClient(
            okHttpClient = okHttpClient,
            rateLimiter = RespectfulRateLimiter(minSpacingMillis = 2_500L),
        ),
    )

    val extensionRepository = ExtensionIndexRepository(
        okHttpClient = okHttpClient,
        rateLimiter = RespectfulRateLimiter(minSpacingMillis = 1_500L),
    )

    val extensionScanner = InstalledExtensionScanner(application)
    val sourceHost = TachiyomiSourceHost(application)
    val sourceMatcher = SourceMatcher()

    val downloadCoordinator = DownloadCoordinator(
        context = application,
        downloadDao = database.downloadDao(),
        downloadPageDao = database.downloadPageDao(),
    )
    private val downloadSemaphores = ConcurrentHashMap<Long, Semaphore>()
    private val downloadRateLimiters = ConcurrentHashMap<Long, RespectfulRateLimiter>()

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
        val semaphore = downloadSemaphores.getOrPut(job.sourceId) { Semaphore(2) }
        val limiter = downloadRateLimiters.getOrPut(job.sourceId) {
            RespectfulRateLimiter(minSpacingMillis = DOWNLOAD_REQUEST_SPACING_MILLIS)
        }
        semaphore.withPermit {
            DownloadTaskRunner(
                pageFetcher = sourcePageFetcher(source, chapter),
                pageStorage = filePageStorage(),
                stateStore = RoomDownloadStateStore(database.downloadDao()),
                sourceRateLimiter = limiter,
            ).run(job)
        }
        return true
    }

    private fun sourcePageFetcher(
        source: SourceDescriptor,
        chapter: SourceChapter,
    ): DownloadPageFetcher =
        object : DownloadPageFetcher {
            override suspend fun pages(job: DownloadJob): List<ReaderPage> =
                sourceHost.pages(source, chapter)

            override suspend fun bytes(page: ReaderPage): ByteArray =
                downloadPageBytes(page)
        }

    private fun filePageStorage(): DownloadPageStorage =
        object : DownloadPageStorage {
            override suspend fun writePage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String =
                writeDownloadedPage(job, page, bytes)
        }

    private suspend fun downloadPageBytes(page: ReaderPage): ByteArray =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(page.imageUrl)
                .apply {
                    page.headers.forEach { (name, value) ->
                        if (name.isNotBlank() && value.isNotBlank()) header(name, value)
                    }
                }
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Page ${page.index + 1} failed: HTTP ${response.code}")
                }
                response.body.bytes()
            }
        }

    private suspend fun writeDownloadedPage(job: DownloadJob, page: ReaderPage, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val chapterDir = File(
                application.filesDir,
                "downloads/${job.mediaId}/${job.sourceId}/${stableFileKey(job.chapterUrl)}",
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
        private const val DOWNLOAD_REQUEST_SPACING_MILLIS = 2_500L
        private val DOWNLOAD_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif")
    }
}
