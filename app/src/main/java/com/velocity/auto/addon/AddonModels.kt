package com.velocity.auto.addon

data class AddonManifest(
    val manifestUrl: String,
    val baseUrl: String,
    val id: String,
    val name: String,
    val description: String,
    val catalogs: List<AddonCatalog>
)

data class AddonCatalog(val type: String, val id: String, val name: String)

data class CatalogItem(
    val id: String,
    val type: String,
    val name: String,
    val posterUrl: String?
)

/**
 * One playback option an addon offered for an item. Only [url] (a direct
 * HTTP/HTTPS/HLS/DASH link) is playable here - an addon that only returns
 * [infoHash] (a torrent) needs a BitTorrent client this app doesn't include.
 */
data class StreamOption(
    val title: String,
    val url: String?,
    val infoHash: String?
) {
    val isPlayable: Boolean get() = !url.isNullOrEmpty()
}
