package com.velocity.auto.youtube.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.VideoStream

sealed class PlayableStream {
    /** A single stream that already has audio muxed in - the simple, preferred case. */
    data class Progressive(val url: String) : PlayableStream()

    /** YouTube's higher qualities are usually video-only; ExoPlayer merges these two at playback time. */
    data class Merged(val videoUrl: String, val audioUrl: String) : PlayableStream()
}

object YouTubeStreamResolver {

    suspend fun resolve(videoUrl: String): PlayableStream = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)

        val bestProgressive = info.videoStreams
            .filter { !it.isVideoOnly }
            .maxByOrNull { heightOf(it) }

        if (bestProgressive != null) {
            return@withContext PlayableStream.Progressive(bestProgressive.content)
        }

        val bestVideoOnly = info.videoOnlyStreams.maxByOrNull { heightOf(it) }
        val bestAudio = info.audioStreams.maxByOrNull { bitrateOf(it) }

        if (bestVideoOnly != null && bestAudio != null) {
            return@withContext PlayableStream.Merged(bestVideoOnly.content, bestAudio.content)
        }

        bestVideoOnly?.let { return@withContext PlayableStream.Progressive(it.content) }

        error("No playable stream found for $videoUrl")
    }

    private fun heightOf(stream: VideoStream): Int =
        Regex("\\d+").find(stream.resolution.orEmpty())?.value?.toIntOrNull() ?: 0

    private fun bitrateOf(stream: AudioStream): Int = stream.averageBitrate
}
