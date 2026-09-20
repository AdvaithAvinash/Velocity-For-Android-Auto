package com.velocity.auto.youtube

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.velocity.auto.R
import com.velocity.auto.databinding.ActivityYoutubeHomeBinding
import com.velocity.auto.util.SystemUiHelper
import com.velocity.auto.youtube.extractor.YouTubeSearchService
import kotlinx.coroutines.launch

/** Search-only YouTube front end - no home feed, no recommendations rabbit hole, just find a video and play it. */
class YouTubeHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityYoutubeHomeBinding
    private val adapter = YouTubeResultsAdapter { video ->
        startActivity(YouTubePlayerActivity.intentFor(this, video.url, video.title))
    }
    private var lastQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubeHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)

        binding.resultsList.layoutManager = LinearLayoutManager(this)
        binding.resultsList.adapter = adapter
        binding.resultsList.itemAnimator = null

        binding.backButton.setOnClickListener { finish() }
        binding.searchButton.setOnClickListener { runSearch() }
        binding.retryButton.setOnClickListener { lastQuery?.let { search(it) } }
        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            val triggeredByEnter = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == EditorInfo.IME_ACTION_SEARCH || triggeredByEnter) {
                runSearch()
                true
            } else {
                false
            }
        }
    }

    private fun runSearch() {
        val query = binding.searchInput.text?.toString()?.trim().orEmpty()
        if (query.isNotEmpty()) search(query)
    }

    private fun search(query: String) {
        lastQuery = query
        showLoading()
        lifecycleScope.launch {
            try {
                val results = YouTubeSearchService.search(query)
                if (results.isEmpty()) showEmpty(getString(R.string.youtube_empty)) else showResults(results)
            } catch (e: Exception) {
                showEmpty(getString(R.string.youtube_error), showRetry = true)
            }
        }
    }

    private fun showLoading() {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.resultsList.visibility = View.GONE
    }

    private fun showResults(videos: List<com.velocity.auto.youtube.model.YtVideoItem>) {
        binding.loadingIndicator.visibility = View.GONE
        binding.emptyState.visibility = View.GONE
        binding.resultsList.visibility = View.VISIBLE
        adapter.submitList(videos)
    }

    private fun showEmpty(message: String, showRetry: Boolean = false) {
        binding.loadingIndicator.visibility = View.GONE
        binding.resultsList.visibility = View.GONE
        binding.emptyState.visibility = View.VISIBLE
        binding.emptyStateMessage.text = message
        binding.retryButton.visibility = if (showRetry) View.VISIBLE else View.GONE
    }

    companion object {
        fun intentFor(context: android.content.Context) = Intent(context, YouTubeHomeActivity::class.java)
    }
}
