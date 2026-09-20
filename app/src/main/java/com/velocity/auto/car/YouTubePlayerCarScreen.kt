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

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            val surface = surfaceContainer.surface ?: return
            startPlayback(surface)
        }

        override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
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

    private fun startPlayback(surface: Surface) {
        lifecycle.coroutineScope.launch {
            try {
                val stream = YouTubeStreamResolver.resolve(video.url, PlaybackPrefs.isDataSaverEnabled(carContext))
                attachAndPlay(stream, surface)
            } catch (e: Exception) {
                errored = true
                invalidate()
            }
        }
    }

    private fun attachAndPlay(stream: PlayableStream, surface: Surface) {
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

        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
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
        val actionStrip = ActionStrip.Builder()
            .addAction(
                Action.Builder()
                    .setTitle(playPauseTitle)
                    .setOnClickListener { togglePlayback() }
                    .build()
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(actionStrip)
            .setBackgroundColor(CarColor.createCustom(BACKGROUND_COLOR, BACKGROUND_COLOR))
            .build()
    }

    companion object {
        // A plain val (not const) since android.graphics.Color.parseColor
        // isn't a compile-time constant expression.
        private val BACKGROUND_COLOR = android.graphics.Color.parseColor("#0D0F12")
    }
}
