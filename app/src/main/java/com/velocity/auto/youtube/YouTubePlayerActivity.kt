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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.session.MediaSession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.velocity.auto.R
import com.velocity.auto.databinding.ActivityYoutubePlayerBinding
import com.velocity.auto.settings.PlaybackPrefs
import com.velocity.auto.util.SystemUiHelper
import com.velocity.auto.youtube.extractor.PlayableStream
import com.velocity.auto.youtube.extractor.QualityOption
import com.velocity.auto.youtube.extractor.ResolvedVideo
import com.velocity.auto.youtube.extractor.VelocityPlaybackCache
import com.velocity.auto.youtube.extractor.YouTubeStreamResolver
import com.velocity.auto.youtube.model.YtVideoItem
import kotlinx.coroutines.launch

/** Plays a single resolved YouTube stream through ExoPlayer - no comments, no autoplay queue, no suggestions. */
@UnstableApi
class YouTubePlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private lateinit var videoUrl: String
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var qualities: List<QualityOption> = emptyList()
    private var selectedHeight: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)
        SystemUiHelper.keepScreenOn(this, true)

        val video = videoFromIntent(intent)
        videoUrl = video.url
        binding.playerTitle.text = video.title
        binding.backButton.setOnClickListener { finish() }
        binding.qualityButton.setOnClickListener { showQualityPicker() }

        HistoryStore(this).record(video)
        loadAndPlay(resumePositionMs = 0L)
    }

    private fun loadAndPlay(resumePositionMs: Long) {
        binding.loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resolved = YouTubeStreamResolver.resolve(
                    videoUrl,
                    targetHeight = selectedHeight,
                    dataSaver = PlaybackPrefs.isDataSaverEnabled(this@YouTubePlayerActivity)
                )
                qualities = resolved.qualities
                updateQualityButton()
                startPlayback(resolved, resumePositionMs)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun startPlayback(resolved: ResolvedVideo, resumePositionMs: Long) {
        player?.release()
        mediaSession?.release()

        val dataSourceFactory = VelocityPlaybackCache.dataSourceFactory(this)

        val exoPlayer = ExoPlayer.Builder(this).build()
        player = exoPlayer
        mediaSession = MediaSession.Builder(this, exoPlayer).build()
        binding.playerView.player = exoPlayer

        when (val stream = resolved.stream) {
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

        if (resumePositionMs > 0L) exoPlayer.seekTo(resumePositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    private fun showQualityPicker() {
        if (qualities.isEmpty()) return
        val labels = listOf(getString(R.string.quality_auto)) + qualities.map { it.label }
        val checkedIndex = qualities.indexOfFirst { it.heightPx == selectedHeight }.let { if (it < 0) 0 else it + 1 }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.quality_picker_title)
            .setSingleChoiceItems(labels.toTypedArray(), checkedIndex) { dialog, which ->
                selectedHeight = if (which == 0) null else qualities[which - 1].heightPx
                dialog.dismiss()
                val resumeAt = player?.currentPosition ?: 0L
                loadAndPlay(resumePositionMs = resumeAt)
            }
            .show()
    }

    private fun updateQualityButton() {
        binding.qualityButton.visibility = if (qualities.isEmpty()) View.GONE else View.VISIBLE
        binding.qualityButton.text = selectedHeight?.let { "${it}p" }
            ?: getString(R.string.quality_auto)
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
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_UPLOADER = "extra_uploader"
        private const val EXTRA_DURATION_SECONDS = "extra_duration_seconds"
        private const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"

        private fun videoFromIntent(intent: Intent) = YtVideoItem(
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            url = intent.getStringExtra(EXTRA_VIDEO_URL).orEmpty(),
            uploader = intent.getStringExtra(EXTRA_UPLOADER).orEmpty(),
            durationSeconds = intent.getLongExtra(EXTRA_DURATION_SECONDS, 0L),
            thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL_URL)
        )

        fun intentFor(context: Context, video: YtVideoItem): Intent =
            Intent(context, YouTubePlayerActivity::class.java)
                .putExtra(EXTRA_VIDEO_URL, video.url)
                .putExtra(EXTRA_TITLE, video.title)
                .putExtra(EXTRA_UPLOADER, video.uploader)
                .putExtra(EXTRA_DURATION_SECONDS, video.durationSeconds)
                .putExtra(EXTRA_THUMBNAIL_URL, video.thumbnailUrl)
    }
}
