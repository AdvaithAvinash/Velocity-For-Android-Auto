package com.velocity.auto.addon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.velocity.auto.databinding.ActivityAddonStreamsBinding
import com.velocity.auto.util.SystemUiHelper
import kotlinx.coroutines.launch

/** Lists the streams an addon offers for one catalog item, and plays whichever direct-link one you pick. */
@UnstableApi
class AddonItemActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddonStreamsBinding
    private val adapter = StreamAdapter { stream ->
        stream.url?.let { url ->
            startActivity(GenericPlayerActivity.intentFor(this, url, binding.itemTitle.text.toString()))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddonStreamsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)

        val manifestUrl = intent.getStringExtra(EXTRA_MANIFEST_URL).orEmpty()
        val type = intent.getStringExtra(EXTRA_TYPE).orEmpty()
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        binding.itemTitle.text = intent.getStringExtra(EXTRA_ITEM_NAME).orEmpty()
        binding.backButton.setOnClickListener { finish() }

        binding.streamsList.layoutManager = LinearLayoutManager(this)
        binding.streamsList.adapter = adapter
        binding.streamsList.itemAnimator = null

        loadStreams(manifestUrl, type, itemId)
    }

    private fun loadStreams(manifestUrl: String, type: String, itemId: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val manifest = AddonClient.fetchManifest(manifestUrl)
                val streams = AddonClient.fetchStreams(manifest, type, itemId)
                binding.loadingIndicator.visibility = View.GONE
                adapter.submitList(streams)
                binding.emptyState.visibility = if (streams.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val EXTRA_MANIFEST_URL = "extra_manifest_url"
        private const val EXTRA_TYPE = "extra_type"
        private const val EXTRA_ITEM_ID = "extra_item_id"
        private const val EXTRA_ITEM_NAME = "extra_item_name"

        fun intentFor(context: Context, manifestUrl: String, type: String, itemId: String, itemName: String): Intent =
            Intent(context, AddonItemActivity::class.java)
                .putExtra(EXTRA_MANIFEST_URL, manifestUrl)
                .putExtra(EXTRA_TYPE, type)
                .putExtra(EXTRA_ITEM_ID, itemId)
                .putExtra(EXTRA_ITEM_NAME, itemName)
    }
}
