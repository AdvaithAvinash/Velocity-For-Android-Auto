package com.velocity.auto.youtube.extractor

import com.velocity.auto.youtube.model.YtVideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Thin wrapper around NewPipeExtractor's YouTube search - keeps every blocking call off the main thread. */
object YouTubeSearchService {

    suspend fun search(query: String): List<YtVideoItem> = withContext(Dispatchers.IO) {
        val extractor = ServiceList.YouTube.getSearchExtractor(query)
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<StreamInfoItem>()
            .map { it.toYtVideoItem() }
    }

    private fun StreamInfoItem.toYtVideoItem(): YtVideoItem = YtVideoItem(
        title = name.orEmpty(),
        url = url.orEmpty(),
        uploader = uploaderName.orEmpty(),
        durationSeconds = duration,
        thumbnailUrl = thumbnails.maxByOrNull { it.height }?.url
    )
}
