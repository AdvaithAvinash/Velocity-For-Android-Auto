package com.velocity.auto.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.velocity.auto.R
import com.velocity.auto.databinding.ActivityHomeBinding
import com.velocity.auto.util.SystemUiHelper
import com.velocity.auto.web.WebAppActivity
import com.velocity.auto.youtube.YouTubeHomeActivity

/** Velocity's front door: a grid of everything you can watch, nothing else on the screen. */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var repository: AppRepository
    private val adapter = AppTileAdapter(
        onClick = { tile -> openTile(tile) },
        onLongClick = { tile -> confirmRemove(tile) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SystemUiHelper.goEdgeToEdge(this)

        repository = AppRepository(this)

        binding.tileList.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        binding.tileList.adapter = adapter
        binding.tileList.itemAnimator = null

        refresh()
    }

    override fun onResume() {
        super.onResume()
        // Custom apps can change (add/remove) while another screen was on top.
        refresh()
    }

    private fun refresh() {
        adapter.submitList(repository.tiles())
    }

    private fun openTile(tile: AppTile) {
        when (tile.kind) {
            AppTile.Kind.YOUTUBE -> startActivity(Intent(this, YouTubeHomeActivity::class.java))
            AppTile.Kind.WEB -> startActivity(
                WebAppActivity.intentFor(this, title = tile.label, url = tile.url.orEmpty())
            )
            AppTile.Kind.ADD_APP -> AddAppDialog.show(this) { name, url ->
                repository.addCustomApp(name, url)
                refresh()
            }
        }
    }

    private fun confirmRemove(tile: AppTile) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.remove_app_title, tile.label))
            .setPositiveButton(R.string.remove_app_confirm) { _, _ ->
                repository.removeCustomApp(CustomApp(tile.label, tile.url.orEmpty()))
                refresh()
            }
            .setNegativeButton(R.string.remove_app_cancel, null)
            .show()
    }

    companion object {
        private const val SPAN_COUNT = 4
    }
}
