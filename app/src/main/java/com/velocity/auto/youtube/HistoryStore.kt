package com.velocity.auto.youtube

import android.content.Context
import com.velocity.auto.youtube.model.YtVideoItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * "Continue watching" - the last few videos actually opened, newest first.
 * No accounts, no sync, just enough local memory that reopening Velocity in
 * the car doesn't mean re-searching for the same video every drive.
 */
class HistoryStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recent(): List<YtVideoItem> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            YtVideoItem(
                title = o.getString("title"),
                url = o.getString("url"),
                uploader = o.optString("uploader"),
                durationSeconds = o.optLong("durationSeconds"),
                thumbnailUrl = o.optString("thumbnailUrl").ifEmpty { null }
            )
        }
    }

    fun record(video: YtVideoItem) {
        val deduped = recent().filterNot { it.url == video.url }
        val updated = (listOf(video) + deduped).take(MAX_ENTRIES)
        val array = JSONArray()
        updated.forEach { v ->
            array.put(
                JSONObject().apply {
                    put("title", v.title)
                    put("url", v.url)
                    put("uploader", v.uploader)
                    put("durationSeconds", v.durationSeconds)
                    put("thumbnailUrl", v.thumbnailUrl ?: "")
                }
            )
        }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "velocity_history"
        private const val KEY_HISTORY = "recent_videos"
        private const val MAX_ENTRIES = 15
    }
}
