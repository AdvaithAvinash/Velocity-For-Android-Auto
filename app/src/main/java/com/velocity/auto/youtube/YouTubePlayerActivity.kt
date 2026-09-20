package com.velocity.auto.youtube

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.velocity.auto.databinding.ActivityYoutubePlayerBinding
import com.velocity.auto.util.SystemUiHelper
import com.velocity.auto.youtube.extractor.PlayableStream
import com.velocity.auto.youtube.extractor.YouTubeStreamResolver
import kotlinx.coroutines.launch

/** Plays a single resolved YouTube stream through ExoPlayer - no comments, no autoplay queue, no suggestions. */
class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)
        SystemUiHelper.keepScreenOn(this, true)

        binding.playerTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.backButton.setOnClickListener { finish() }

        loadAndPlay(intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty())
    }

    private fun loadAndPlay(videoUrl: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val stream = YouTubeStreamResolver.resolve(videoUrl)
                startPlayback(stream)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun startPlayback(stream: PlayableStream) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        binding.playerView.player = exoPlayer

        when (stream) {
            is PlayableStream.Progressive -> {
                exoPlayer.setMediaItem(MediaItem.fromUri(stream.url))
            }
            is PlayableStream.Merged -> {
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(stream.videoUrl))
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(stream.audioUrl))
                exoPlayer.setMediaSource(MergingMediaSource(videoSource, audioSource))
            }
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    binding.loadingIndicator.visibility = View.GONE
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                showError()
            }
        })

        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun showError() {
        binding.loadingIndicator.visibility = View.GONE
        binding.errorMessage.visibility = View.VISIBLE
    }

    override fun onStop() {
        player?.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

        fun intentFor(context: Context, videoUrl: String, title: String): Intent =
            Intent(context, YouTubePlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, videoUrl)
                .putExtra(EXTRA_TITLE, title)
    }
}
