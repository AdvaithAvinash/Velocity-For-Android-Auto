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

/** One entry in the quality picker, e.g. "1080p" backed by that resolution's pixel height. */
data class QualityOption(val heightPx: Int, val label: String)

data class ResolvedVideo(
    val stream: PlayableStream,
    /** Every distinct resolution this video actually has, highest first - what the quality picker lists. */
    val qualities: List<QualityOption>,
    /** Null means Auto/Data Saver picked it; non-null echoes back the height the caller asked for. */
    val selectedHeight: Int?
)

object YouTubeStreamResolver {

    private const val DATA_SAVER_MAX_HEIGHT = 480

    /**
     * @param targetHeight an exact quality the user picked (e.g. from [QualityOption]); when null,
     *   falls back to Auto (best available) or, with [dataSaver], the best quality at or under 480p.
     */
    suspend fun resolve(
        videoUrl: String,
        targetHeight: Int? = null,
        dataSaver: Boolean = false
    ): ResolvedVideo = withContext(Dispatchers.IO) {
        val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)
        val progressiveCandidates = info.videoStreams.filter { !it.isVideoOnly }
        val videoOnlyCandidates = info.videoOnlyStreams

        val qualities = (progressiveCandidates + videoOnlyCandidates)
            .map { heightOf(it) }
            .filter { it > 0 }
            .distinct()
            .sortedDescending()
            .map { QualityOption(it, "${it}p") }

        val bestProgressive = if (targetHeight != null) {
            pickExact(progressiveCandidates, targetHeight)
        } else {
            pick(progressiveCandidates, dataSaver)
        }
        if (bestProgressive != null) {
            return@withContext ResolvedVideo(
                PlayableStream.Progressive(bestProgressive.content),
                qualities,
                targetHeight
            )
        }

        val bestVideoOnly = if (targetHeight != null) {
            pickExact(videoOnlyCandidates, targetHeight)
        } else {
            pick(videoOnlyCandidates, dataSaver)
        }
        val bestAudio = info.audioStreams.maxByOrNull { bitrateOf(it) }

        if (bestVideoOnly != null && bestAudio != null) {
            return@withContext ResolvedVideo(
                PlayableStream.Merged(bestVideoOnly.content, bestAudio.content),
                qualities,
                targetHeight
            )
        }

        bestVideoOnly?.let {
            return@withContext ResolvedVideo(PlayableStream.Progressive(it.content), qualities, targetHeight)
        }

        error("No playable stream found for $videoUrl")
    }

    /** Best quality by default; with Data Saver on, the best quality at or under the cap, or the smallest available if nothing qualifies. */
    private fun pick(candidates: List<VideoStream>, dataSaver: Boolean): VideoStream? {
        if (candidates.isEmpty()) return null
        if (!dataSaver) return candidates.maxByOrNull { heightOf(it) }
        val capped = candidates.filter { heightOf(it) in 1..DATA_SAVER_MAX_HEIGHT }
        return capped.maxByOrNull { heightOf(it) } ?: candidates.minByOrNull { heightOf(it) }
    }

    /** The stream whose height is closest to what the user asked for - exact match when available. */
    private fun pickExact(candidates: List<VideoStream>, targetHeight: Int): VideoStream? =
        candidates.minByOrNull { kotlin.math.abs(heightOf(it) - targetHeight) }

    private fun heightOf(stream: VideoStream): Int =
        Regex("\\d+").find(stream.resolution.orEmpty())?.value?.toIntOrNull() ?: 0

    private fun bitrateOf(stream: AudioStream): Int = stream.averageBitrate
}
