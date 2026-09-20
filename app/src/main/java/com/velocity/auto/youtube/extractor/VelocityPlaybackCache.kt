package com.velocity.auto.youtube.extractor

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import java.io.File
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

/**
 * One shared, connection-pooled [OkHttpClient] and one on-disk [SimpleCache],
 * reused across every playback screen (phone and car) instead of each one
 * building its own HTTP stack from scratch. Switching videos back and forth,
 * or replaying one, no longer means a fresh TLS handshake and a full
 * re-download - that's most of what "faster" means for a video player.
 */
@UnstableApi
object VelocityPlaybackCache {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    private const val CACHE_SIZE_BYTES = 150L * 1024 * 1024

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private var cache: SimpleCache? = null

    @Synchronized
    fun dataSourceFactory(context: Context): CacheDataSource.Factory {
        val appContext = context.applicationContext
        val simpleCache = cache ?: SimpleCache(
            File(appContext.cacheDir, "media"),
            LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES)
        ).also { cache = it }

        val httpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(USER_AGENT)

        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
