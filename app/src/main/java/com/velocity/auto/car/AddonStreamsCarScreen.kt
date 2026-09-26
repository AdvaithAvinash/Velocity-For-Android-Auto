package com.velocity.auto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.coroutineScope
import androidx.media3.common.util.UnstableApi
import com.velocity.auto.R
import com.velocity.auto.addon.AddonClient
import com.velocity.auto.addon.AddonManifest
import com.velocity.auto.addon.CatalogItem
import com.velocity.auto.addon.StreamOption
import kotlinx.coroutines.launch

/** Lists an item's resolved streams; only direct-URL ones are clickable, torrent-only ones are shown but inert. */
@UnstableApi
class AddonStreamsCarScreen(
    carContext: CarContext,
    private val manifest: AddonManifest,
    private val item: CatalogItem
) : Screen(carContext) {

    private var streams: List<StreamOption> = emptyList()
    private var loading = true
    private var errored = false

    init {
        loadStreams()
    }

    private fun loadStreams() {
        lifecycle.coroutineScope.launch {
            streams = try {
                AddonClient.fetchStreams(manifest, item.type, item.id)
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
            .setTitle(item.name)
            .setHeaderAction(Action.BACK)
            .setLoading(loading)

        if (!loading) {
            val itemList = ItemList.Builder()
            if (streams.isEmpty()) {
                itemList.setNoItemsMessage(carContext.getString(R.string.addon_no_streams))
            }
            streams.forEach { stream ->
                val row = Row.Builder().setTitle(stream.title)
                if (stream.isPlayable) {
                    row.addText(carContext.getString(R.string.stream_direct))
                        .setOnClickListener {
                            screenManager.push(
                                GenericPlayerCarScreen(carContext, stream.url!!, item.name)
                            )
                        }
                } else {
                    row.addText(carContext.getString(R.string.stream_unsupported))
                }
                itemList.addItem(row.build())
            }
            builder.setSingleList(itemList.build())
        }

        return builder.build()
    }
}
