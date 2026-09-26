package com.velocity.auto.addon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import com.velocity.auto.databinding.ActivityYoutubePlayerBinding
import com.velocity.auto.util.SystemUiHelper
import com.velocity.auto.youtube.extractor.VelocityPlaybackCache

/** Plays a direct stream URL an addon resolved - just one link, no YouTube-style quality picking. */
@UnstableApi
class GenericPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)
        SystemUiHelper.keepScreenOn(this, true)

        binding.playerTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.qualityButton.visibility = View.GONE
        binding.backButton.setOnClickListener { finish() }

        startPlayback(intent.getStringExtra(EXTRA_URL).orEmpty())
    }

    private fun startPlayback(url: String) {
        binding.loadingIndicator.visibility = View.VISIBLE

        val dataSourceFactory = VelocityPlaybackCache.dataSourceFactory(this)
        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
        binding.playerView.player = exoPlayer

        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        binding.loadingIndicator.visibility = View.GONE
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.errorMessage.visibility = View.VISIBLE
                }
            }
        )
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"

        fun intentFor(context: Context, url: String, title: String): Intent =
            Intent(context, GenericPlayerActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
    }
}
