package com.velocity.auto.addon

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * A minimal client for the open Stremio addon protocol - manifest.json plus
 * catalog/stream endpoints. It's the same public protocol Stremio itself
 * uses, so any addon built for Stremio works here too. This client only
 * surfaces streams that resolve to a direct HTTP(S) URL as playable; a
 * torrent-only stream (infoHash, no url) needs a BitTorrent client, which
 * this app deliberately doesn't include.
 */
object AddonClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchManifest(manifestUrl: String): AddonManifest = withContext(Dispatchers.IO) {
        val json = getJson(manifestUrl)
        val baseUrl = manifestUrl.substringBeforeLast("/manifest.json")
        AddonManifest(
            manifestUrl = manifestUrl,
            baseUrl = baseUrl,
            id = json.optString("id", baseUrl),
            name = json.optString("name", baseUrl),
            description = json.optString("description", ""),
            catalogs = json.optJSONArray("catalogs")?.toCatalogList().orEmpty()
        )
    }

    suspend fun fetchCatalog(manifest: AddonManifest, catalog: AddonCatalog): List<CatalogItem> =
        withContext(Dispatchers.IO) {
            val json = getJson("${manifest.baseUrl}/catalog/${catalog.type}/${catalog.id}.json")
            val metas = json.optJSONArray("metas") ?: return@withContext emptyList()
            (0 until metas.length()).map { i ->
                val m = metas.getJSONObject(i)
                CatalogItem(
                    id = m.getString("id"),
                    type = m.optString("type", catalog.type),
                    name = m.optString("name", m.getString("id")),
                    posterUrl = m.optString("poster").ifEmpty { null }
                )
            }
        }

    suspend fun fetchStreams(manifest: AddonManifest, type: String, itemId: String): List<StreamOption> =
        withContext(Dispatchers.IO) {
            val json = getJson("${manifest.baseUrl}/stream/$type/$itemId.json")
            val streams = json.optJSONArray("streams") ?: return@withContext emptyList()
            (0 until streams.length()).map { i ->
                val s = streams.getJSONObject(i)
                val title = s.optString("title").ifEmpty { null }
                    ?: s.optString("name").ifEmpty { null }
                    ?: "Stream ${i + 1}"
                StreamOption(
                    title = title,
                    url = s.optString("url").ifEmpty { null },
                    infoHash = s.optString("infoHash").ifEmpty { null }
                )
            }
        }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            return JSONObject(response.body?.string().orEmpty())
        }
    }

    private fun JSONArray.toCatalogList(): List<AddonCatalog> =
        (0 until length()).map { i ->
            val c = getJSONObject(i)
            AddonCatalog(c.getString("type"), c.getString("id"), c.optString("name", c.getString("id")))
        }
}
