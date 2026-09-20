package com.velocity.auto.settings

import android.content.Context

/** One toggle: cap YouTube playback quality to load faster and use less data - mainly for spotty in-car cellular. */
object PlaybackPrefs {
    private const val PREFS_NAME = "velocity_settings"
    private const val KEY_DATA_SAVER = "data_saver"

    fun isDataSaverEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DATA_SAVER, false)

    fun setDataSaverEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DATA_SAVER, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
