package com.velocity.auto.addon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.velocity.auto.R
import com.velocity.auto.databinding.ActivityAddonManagerBinding
import com.velocity.auto.util.SystemUiHelper
import kotlinx.coroutines.launch

/** Where addons are added/removed by manifest URL - browsing happens in [AddonCatalogActivity]. */
class AddonManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddonManagerBinding
    private lateinit var repository: AddonRepository
    private val adapter = AddonListAdapter(
        onClick = { manifestUrl -> startActivity(AddonCatalogActivity.intentFor(this, manifestUrl)) },
        onLongClick = { manifestUrl -> confirmRemove(manifestUrl) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddonManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)

        repository = AddonRepository(this)
        binding.addonList.layoutManager = LinearLayoutManager(this)
        binding.addonList.adapter = adapter
        binding.addonList.itemAnimator = null

        binding.backButton.setOnClickListener { finish() }
        binding.addButton.setOnClickListener { showAddDialog() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val urls = repository.manifestUrls()
        binding.emptyState.visibility = if (urls.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(urls)
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_addon, null)
        val urlField = view.findViewById<TextInputEditText>(R.id.addonUrlInput)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.addon_add_title)
            .setView(view)
            .setPositiveButton(R.string.add_app_save) { _, _ ->
                val url = urlField.text?.toString()?.trim().orEmpty()
                if (url.isNotEmpty()) verifyAndAdd(url)
            }
            .setNegativeButton(R.string.add_app_cancel, null)
            .show()
    }

    private fun verifyAndAdd(manifestUrl: String) {
        lifecycleScope.launch {
            try {
                // Fetch once just to confirm this is really a valid addon before saving it.
                AddonClient.fetchManifest(manifestUrl)
                repository.addManifestUrl(manifestUrl)
                refresh()
            } catch (e: Exception) {
                Snackbar.make(binding.root, R.string.addon_add_failed, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmRemove(manifestUrl: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.addon_remove_title)
            .setPositiveButton(R.string.remove_app_confirm) { _, _ ->
                repository.removeManifestUrl(manifestUrl)
                refresh()
            }
            .setNegativeButton(R.string.remove_app_cancel, null)
            .show()
    }

    companion object {
        fun intentFor(context: Context): Intent = Intent(context, AddonManagerActivity::class.java)
    }
}
