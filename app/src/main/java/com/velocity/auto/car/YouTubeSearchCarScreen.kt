package com.velocity.auto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.coroutineScope
import com.velocity.auto.R
import com.velocity.auto.youtube.HistoryStore
import com.velocity.auto.youtube.extractor.YouTubeSearchService
import com.velocity.auto.youtube.model.YtVideoItem
import kotlinx.coroutines.launch

/** Search-and-play, car-template style - the same search-only philosophy as the phone screen. */
class YouTubeSearchCarScreen(carContext: CarContext) : Screen(carContext) {

    // Continue watching, shown until the driver actually searches for
    // something - beats a blank search box every time the car reconnects.
    private var results: List<YtVideoItem> = HistoryStore(carContext).recent()
    private var loading = false
    private var lastQuery = ""

    private val searchListener = object : SearchTemplate.SearchCallback {
        override fun onSearchTextChanged(searchText: String) {
            // Only search on submit (Enter / mic-done) - searching on every
            // keystroke would spam the extractor for no benefit on a car
            // screen where typing is slow and deliberate.
        }

        override fun onSearchSubmitted(searchText: String) {
            runSearch(searchText)
        }
    }

    private fun runSearch(query: String) {
        if (query.isBlank()) return
        lastQuery = query
        loading = true
        invalidate()
        lifecycle.coroutineScope.launch {
            results = try {
                YouTubeSearchService.search(query)
            } catch (e: Exception) {
                emptyList()
            }
            loading = false
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val builder = SearchTemplate.Builder(searchListener)
            .setHeaderAction(Action.BACK)
            .setSearchHint(carContext.getString(R.string.youtube_search_hint))
            .setShowKeyboardByDefault(lastQuery.isEmpty() && results.isEmpty())
            .setLoading(loading)

        // Templates that support a loading spinner generally reject also
        // setting content for that same build - only attach the list once
        // there's something (or nothing-found) to show.
        if (!loading) {
            val itemListBuilder = ItemList.Builder()
            if (results.isEmpty() && lastQuery.isNotEmpty()) {
                itemListBuilder.setNoItemsMessage(carContext.getString(R.string.youtube_empty))
            }
            results.forEach { video ->
                itemListBuilder.addItem(
                    Row.Builder()
                        .setTitle(video.title)
                        .addText("${video.uploader} · ${video.formattedDuration()}")
                        .setOnClickListener {
                            screenManager.push(YouTubePlayerCarScreen(carContext, video))
                        }
                        .build()
                )
            }
            builder.setItemList(itemListBuilder.build())
        }

        return builder.build()
    }
}
