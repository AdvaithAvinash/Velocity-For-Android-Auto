package com.velocity.auto.home

import android.content.Context
import androidx.core.content.ContextCompat
import com.velocity.auto.R
import org.json.JSONArray
import org.json.JSONObject

data class CustomApp(val name: String, val url: String)

/**
 * Holds Velocity's app picker: the built-in YouTube/Netflix/Stremio tiles
 * plus whatever web apps the user has added, persisted as a small JSON blob
 * in SharedPreferences - no database needed for a handful of shortcuts.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun tiles(): List<AppTile> = builtInTiles() + customTiles() + addAppTile()

    fun customApps(): List<CustomApp> {
        val raw = prefs.getString(KEY_CUSTOM_APPS, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CustomApp(o.getString("name"), o.getString("url"))
        }
    }

    fun addCustomApp(name: String, url: String) {
        val apps = customApps().toMutableList()
        apps.add(CustomApp(name, url))
        persist(apps)
    }

    fun removeCustomApp(app: CustomApp) {
        val apps = customApps().toMutableList()
        apps.removeAll { it.name == app.name && it.url == app.url }
        persist(apps)
    }

    private fun persist(apps: List<CustomApp>) {
        val array = JSONArray()
        apps.forEach { app ->
            array.put(
                JSONObject().apply {
                    put("name", app.name)
                    put("url", app.url)
                }
            )
        }
        prefs.edit().putString(KEY_CUSTOM_APPS, array.toString()).apply()
    }

    private fun builtInTiles(): List<AppTile> = listOf(
        AppTile(
            id = "youtube",
            label = appContext.getString(R.string.tile_youtube),
            iconRes = R.drawable.ic_play_circle,
            tintColor = color(R.color.youtube_tint),
            kind = AppTile.Kind.YOUTUBE
        ),
        AppTile(
            id = "netflix",
            label = appContext.getString(R.string.tile_netflix),
            iconRes = R.drawable.ic_movie,
            tintColor = color(R.color.netflix_tint),
            kind = AppTile.Kind.WEB,
            url = "https://www.netflix.com/browse"
        ),
        AppTile(
            id = "stremio",
            label = appContext.getString(R.string.tile_stremio),
            iconRes = R.drawable.ic_globe,
            tintColor = color(R.color.stremio_tint),
            kind = AppTile.Kind.WEB,
            url = "https://web.stremio.com/"
        ),
        AppTile(
            id = "disney_plus",
            label = appContext.getString(R.string.tile_disney_plus),
            iconRes = R.drawable.ic_movie,
            tintColor = color(R.color.disney_tint),
            kind = AppTile.Kind.WEB,
            url = "https://www.disneyplus.com"
        ),
        AppTile(
            id = "prime_video",
            label = appContext.getString(R.string.tile_prime_video),
            iconRes = R.drawable.ic_movie,
            tintColor = color(R.color.primevideo_tint),
            kind = AppTile.Kind.WEB,
            url = "https://www.primevideo.com"
        ),
        AppTile(
            id = "max",
            label = appContext.getString(R.string.tile_max),
            iconRes = R.drawable.ic_movie,
            tintColor = color(R.color.max_tint),
            kind = AppTile.Kind.WEB,
            url = "https://www.max.com"
        ),
        AppTile(
            id = "addons",
            label = appContext.getString(R.string.tile_addons),
            iconRes = R.drawable.ic_extension,
            tintColor = color(R.color.addon_tint),
            kind = AppTile.Kind.ADDONS
        )
    )

    private fun customTiles(): List<AppTile> = customApps().map { app ->
        AppTile(
            id = "custom:${app.name}:${app.url}",
            label = app.name,
            iconRes = R.drawable.ic_globe,
            tintColor = color(R.color.web_tint),
            kind = AppTile.Kind.WEB,
            url = app.url,
            removable = true
        )
    }

    private fun addAppTile(): AppTile = AppTile(
        id = "add",
        label = appContext.getString(R.string.tile_add_app),
        iconRes = R.drawable.ic_add,
        tintColor = color(R.color.on_background_muted),
        kind = AppTile.Kind.ADD_APP
    )

    private fun color(res: Int) = ContextCompat.getColor(appContext, res)

    companion object {
        private const val PREFS_NAME = "velocity_apps"
        private const val KEY_CUSTOM_APPS = "custom_apps"
    }
}
