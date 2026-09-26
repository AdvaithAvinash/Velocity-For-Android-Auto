package com.velocity.auto.car

import android.view.Surface
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarColor
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import com.velocity.auto.settings.PlaybackPrefs
import com.velocity.auto.youtube.HistoryStore
import com.velocity.auto.youtube.extractor.PlayableStream
import com.velocity.auto.youtube.extractor.QualityOption
import com.velocity.auto.youtube.extractor.VelocityPlaybackCache
import com.velocity.auto.youtube.extractor.YouTubeStreamResolver
import com.velocity.auto.youtube.model.YtVideoItem
import kotlinx.coroutines.launch

/**
 * The actual in-car video trick: a NavigationTemplate is the one template
 * the car-app framework hands a raw [Surface] to (via [SurfaceCallback]) -
 * meant for drawing a map, but ExoPlayer will happily render decoded video
 * frames onto any Surface it's given. This is the same technique apps like
 * CarStream use to get real video playback onto the car screen at all.
 */
@UnstableApi
class YouTubePlayerCarScreen(
    carContext: CarContext,
    private val video: YtVideoItem
) : Screen(carContext) {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var loading = true
    private var errored = false
    private var currentSurface: Surface? = null
    private var qualities: List<QualityOption> = emptyList()
    private var selectedHeight: Int? = null

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            currentSurface = surfaceContainer.surface ?: return
            startPlayback(resumePositionMs = 0L)
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
            currentSurface = null
            player?.clearVideoSurface()
        }

        override fun onClick(x: Float, y: Float) {
            togglePlayback()
        }
    }

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(surfaceCallback)
        HistoryStore(carContext).record(video)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    mediaSession?.release()
                    mediaSession = null
                    player?.release()
                    player = null
                }
            }
        )
    }

    private fun startPlayback(resumePositionMs: Long) {
        loading = true
        invalidate()
        lifecycle.coroutineScope.launch {
            try {
                val resolved = YouTubeStreamResolver.resolve(
                    video.url,
                    targetHeight = selectedHeight,
                    dataSaver = PlaybackPrefs.isDataSaverEnabled(carContext)
                )
                qualities = resolved.qualities
                attachAndPlay(resolved.stream, resumePositionMs)
            } catch (e: Exception) {
                errored = true
                invalidate()
            }
        }
    }

    private fun attachAndPlay(stream: PlayableStream, resumePositionMs: Long) {
        val surface = currentSurface ?: return
        player?.release()
        mediaSession?.release()

        val dataSourceFactory = VelocityPlaybackCache.dataSourceFactory(carContext)

        val exoPlayer = ExoPlayer.Builder(carContext).build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(carContext, exoPlayer).build()
        exoPlayer.setVideoSurface(surface)

        when (stream) {
            is PlayableStream.Progressive -> exoPlayer.setMediaItem(MediaItem.fromUri(stream.url))
            is PlayableStream.Merged -> {
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(stream.videoUrl))
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(stream.audioUrl))
                exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
            }
        }

        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        loading = false
                        invalidate()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    errored = true
                    invalidate()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    invalidate()
                }
            }
        )

        if (resumePositionMs > 0L) exoPlayer.seekTo(resumePositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        invalidate()
    }

    private fun switchQuality(height: Int?) {
        selectedHeight = height
        val resumeAt = player?.currentPosition ?: 0L
        startPlayback(resumeAt)
    }

    private fun togglePlayback() {
        player?.let { p ->
            if (p.isPlaying) p.pause() else p.play()
        }
    }

    override fun onGetTemplate(): Template {
        val playPauseTitle = when {
            player?.isPlaying == true -> "Pause"
            else -> "Play"
        }
        val actionStripBuilder = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(playPauseTitle)
                    .setOnClickListener { togglePlayback() }
                    .build()
            )

        if (qualities.isNotEmpty()) {
            actionStripBuilder.addAction(
                Action.Builder()
                    .setTitle(selectedHeight?.let { "${it}p" } ?: "Auto")
                    .setOnClickListener {
                        screenManager.push(QualityPickerCarScreen(carContext, qualities, ::switchQuality))
                    }
                    .build()
            )
        }

        return NavigationTemplate.Builder()
            .setActionStrip(actionStripBuilder.build())
            .setBackgroundColor(CarColor.createCustom(BACKGROUND_COLOR, BACKGROUND_COLOR))
            .build()
    }

    companion object {
        // A plain val (not const) since android.graphics.Color.parseColor
        // isn't a compile-time constant expression.
        private val BACKGROUND_COLOR = android.graphics.Color.parseColor("#0D0F12")
    }
}
