package com.velocity.auto.addon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.velocity.auto.databinding.ActivityAddonCatalogBinding
import com.velocity.auto.util.SystemUiHelper
import kotlinx.coroutines.launch

/** Browses one addon's catalogs (a spinner when there's more than one) and their poster grids. */
class AddonCatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddonCatalogBinding
    private lateinit var manifestUrl: String
    private var manifest: AddonManifest? = null
    private val adapter = CatalogItemAdapter { item ->
        manifest?.let { m ->
            startActivity(AddonItemActivity.intentFor(this, m.manifestUrl, item.type, item.id, item.name))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddonCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)

        manifestUrl = intent.getStringExtra(EXTRA_MANIFEST_URL).orEmpty()
        binding.itemsList.layoutManager = GridLayoutManager(this, GRID_SPAN_COUNT)
        binding.itemsList.adapter = adapter
        binding.itemsList.itemAnimator = null
        binding.backButton.setOnClickListener { finish() }

        loadManifest()
    }

    private fun loadManifest() {
        binding.loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val loaded = AddonClient.fetchManifest(manifestUrl)
                manifest = loaded
                binding.addonTitle.text = loaded.name
                if (loaded.catalogs.isEmpty()) {
                    binding.loadingIndicator.visibility = View.GONE
                    binding.emptyState.visibility = View.VISIBLE
                    return@launch
                }
                setUpCatalogSpinner(loaded)
                loadCatalog(loaded, loaded.catalogs.first())
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun setUpCatalogSpinner(manifest: AddonManifest) {
        if (manifest.catalogs.size <= 1) return
        binding.catalogSpinner.visibility = View.VISIBLE
        binding.catalogSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            manifest.catalogs.map { it.name }
        )
        binding.catalogSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadCatalog(manifest, manifest.catalogs[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun loadCatalog(manifest: AddonManifest, catalog: AddonCatalog) {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val items = AddonClient.fetchCatalog(manifest, catalog)
                binding.loadingIndicator.visibility = View.GONE
                adapter.submitList(items)
                binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                binding.loadingIndicator.visibility = View.GONE
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val EXTRA_MANIFEST_URL = "extra_manifest_url"
        private const val GRID_SPAN_COUNT = 3

        fun intentFor(context: Context, manifestUrl: String): Intent =
            Intent(context, AddonCatalogActivity::class.java)
                .putExtra(EXTRA_MANIFEST_URL, manifestUrl)
    }
}
