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
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.velocity.auto.youtube.extractor.VelocityPlaybackCache

/** Plays a single direct addon stream URL in the car - same Surface trick as [YouTubePlayerCarScreen], no quality picker. */
@UnstableApi
class GenericPlayerCarScreen(
    carContext: CarContext,
    private val streamUrl: String,
    private val title: String
) : Screen(carContext) {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var loading = true
    private var errored = false
    private var currentSurface: Surface? = null

    private val surfaceCallback = object : SurfaceCallback {
        override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
            currentSurface = surfaceContainer.surface ?: return
            startPlayback()
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

    private fun startPlayback() {
        val surface = currentSurface ?: return
        loading = true
        invalidate()

        val dataSourceFactory = VelocityPlaybackCache.dataSourceFactory(carContext)
        val exoPlayer = ExoPlayer.Builder(carContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(carContext, exoPlayer).build()
        exoPlayer.setVideoSurface(surface)
        exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))

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
        invalidate()
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
        private val BACKGROUND_COLOR = android.graphics.Color.parseColor("#0D0F12")
    }
}
