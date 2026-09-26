package com.velocity.auto.car

import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.coroutineScope
import com.velocity.auto.R
import com.velocity.auto.addon.AddonClient
import com.velocity.auto.addon.AddonRepository
import kotlinx.coroutines.launch

/** Car-side addon list - manage (add/remove) still happens on the phone; this just browses what's already added. */
class AddonHomeCarScreen(carContext: CarContext) : Screen(carContext) {

    private val repository = AddonRepository(carContext)
    private var loading = false

    override fun onGetTemplate(): Template {
        val addons = repository.manifestUrls()
        if (addons.isEmpty()) {
            return MessageTemplate.Builder(carContext.getString(R.string.addon_empty))
                .setHeaderAction(Action.BACK)
                .build()
        }

        val builder = ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.addon_manager_title))
            .setHeaderAction(Action.BACK)
            .setLoading(loading)

        if (!loading) {
            val itemList = ItemList.Builder()
            addons.forEach { manifestUrl ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle(Uri.parse(manifestUrl).host ?: manifestUrl)
                        .setOnClickListener { openAddon(manifestUrl) }
                        .build()
                )
            }
            builder.setSingleList(itemList.build())
        }

        return builder.build()
    }

    private fun openAddon(manifestUrl: String) {
        loading = true
        invalidate()
        lifecycle.coroutineScope.launch {
            try {
                val manifest = AddonClient.fetchManifest(manifestUrl)
                loading = false
                val catalog = manifest.catalogs.firstOrNull()
                if (catalog != null) {
                    screenManager.push(AddonCatalogCarScreen(carContext, manifest, catalog))
                } else {
                    invalidate()
                }
            } catch (e: Exception) {
                loading = false
                invalidate()
            }
        }
    }
}
