package com.tankobun.app

import android.util.Log
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceChapter
import com.tankobun.core.model.SourceDescriptor
import okio.Buffer

data class ReaderPageImageModel(
    val mediaId: Int,
    val chapter: SourceChapter,
    val source: SourceDescriptor,
    val page: ReaderPage,
    val retryAttempt: Int = 0,
) {
    val cacheKey: String =
        "reader:$mediaId:${source.id}:${chapter.url}:${page.index}:${page.sourcePageUrl}:${page.imageUrl}:retry:$retryAttempt"
}

class ReaderPageFetcher(
    private val data: ReaderPageImageModel,
    private val container: AppContainer,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val cachedBytes = if (data.retryAttempt == 0) {
            ReaderPageCache.cachedBytes(
                context = container.application,
                mediaId = data.mediaId,
                chapter = data.chapter,
                page = data.page,
            )
        } else {
            null
        }
        val bytes = cachedBytes ?: container.sourceHost.imageBytes(data.source, data.page).also { fetchedBytes ->
            runCatching {
                ReaderPageCache.writePage(
                    context = container.application,
                    mediaId = data.mediaId,
                    chapter = data.chapter,
                    page = data.page,
                    bytes = fetchedBytes,
                )
            }.onFailure { error ->
                Log.w(TAG, "Reader page cache write failed for ${data.chapter.name} page ${data.page.index + 1}", error)
            }
        }

        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = null,
            dataSource = if (cachedBytes == null) DataSource.NETWORK else DataSource.DISK,
        )
    }

    class Factory(private val container: AppContainer) : Fetcher.Factory<ReaderPageImageModel> {
        override fun create(
            data: ReaderPageImageModel,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher =
            ReaderPageFetcher(data, container, options)
    }

    companion object {
        private const val TAG = "TankobunReader"
    }
}

class ReaderPageImageModelKeyer : Keyer<ReaderPageImageModel> {
    override fun key(data: ReaderPageImageModel, options: Options): String = data.cacheKey
}
