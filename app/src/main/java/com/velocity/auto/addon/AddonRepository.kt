package com.velocity.auto.addon

import android.content.Context
import org.json.JSONArray

/** Just remembers which addon manifest URLs the user has added - everything else (name, catalogs) is fetched live. */
class AddonRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun manifestUrls(): List<String> {
        val raw = prefs.getString(KEY_URLS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getString(it) }
    }

    fun addManifestUrl(url: String) {
        persist((manifestUrls() + url).distinct())
    }

    fun removeManifestUrl(url: String) {
        persist(manifestUrls() - url)
    }

    private fun persist(urls: List<String>) {
        val array = JSONArray()
        urls.forEach { array.put(it) }
        prefs.edit().putString(KEY_URLS, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "velocity_addons"
        private const val KEY_URLS = "manifest_urls"
    }
}
