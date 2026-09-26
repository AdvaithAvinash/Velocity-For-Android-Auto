package com.velocity.auto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.coroutineScope
import com.velocity.auto.R
import com.velocity.auto.addon.AddonCatalog
import com.velocity.auto.addon.AddonClient
import com.velocity.auto.addon.AddonManifest
import com.velocity.auto.addon.CatalogItem
import kotlinx.coroutines.launch

/**
 * Shows one catalog's items as a plain text list - no poster art, since car
 * templates need a local bitmap for images and fetching/decoding remote
 * posters just to show a grid isn't worth the complexity here. Only browses
 * the first catalog an addon offers; switching catalogs (like the phone's
 * spinner) is a phone-only nicety for now.
 */
class AddonCatalogCarScreen(
    carContext: CarContext,
    private val manifest: AddonManifest,
    private val catalog: AddonCatalog
) : Screen(carContext) {

    private var items: List<CatalogItem> = emptyList()
    private var loading = true
    private var errored = false

    init {
        loadItems()
    }

    private fun loadItems() {
        lifecycle.coroutineScope.launch {
            items = try {
                AddonClient.fetchCatalog(manifest, catalog)
            } catch (e: Exception) {
                errored = true
                emptyList()
            }
            loading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val builder = ListTemplate.Builder()
            .setTitle(manifest.name)
            .setHeaderAction(Action.BACK)
            .setLoading(loading)

        if (!loading) {
            val itemList = ItemList.Builder()
            if (items.isEmpty()) {
                itemList.setNoItemsMessage(carContext.getString(R.string.addon_catalog_empty))
            }
            items.forEach { item ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle(item.name)
                        .setOnClickListener {
                            screenManager.push(AddonStreamsCarScreen(carContext, manifest, item))
                        }
                        .build()
                )
            }
            builder.setSingleList(itemList.build())
        }

        return builder.build()
    }
}
