package com.tankobun.app

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import com.tankobun.core.model.ReaderPage
import com.tankobun.core.model.SourceDescriptor
import okio.Buffer

data class SourceThumbnailImageModel(
    val source: SourceDescriptor,
    val imageUrl: String,
) {
    val cacheKey: String =
        "source-thumbnail:${source.packageName}:${source.id}:${source.versionCode}:$imageUrl"
}

class SourceThumbnailFetcher(
    private val data: SourceThumbnailImageModel,
    private val container: AppContainer,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val result = container.sourceCoverCache.load(
            key = data.cacheKey,
            readEnabled = options.diskCachePolicy.readEnabled,
            writeEnabled = options.diskCachePolicy.writeEnabled,
            networkEnabled = options.networkCachePolicy.readEnabled,
        ) {
            container.sourceHost.imageBytes(
                source = data.source,
                page = ReaderPage(
                    index = 0,
                    imageUrl = data.imageUrl,
                    cachedFilePath = null,
                    sourcePageUrl = data.imageUrl,
                    imageUrlResolved = true,
                ),
            )
        }
        return SourceFetchResult(
            source = ImageSource(Buffer().write(result.bytes), options.fileSystem),
            mimeType = null,
            dataSource = if (result.fromCache) DataSource.DISK else DataSource.NETWORK,
        )
    }

    class Factory(private val container: AppContainer) : Fetcher.Factory<SourceThumbnailImageModel> {
        override fun create(
            data: SourceThumbnailImageModel,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher = SourceThumbnailFetcher(data, container, options)
    }
}

class SourceThumbnailImageModelKeyer : Keyer<SourceThumbnailImageModel> {
    override fun key(data: SourceThumbnailImageModel, options: Options): String = data.cacheKey
}
