package com.velocity.auto.settings

import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.velocity.auto.R

/** Just the one toggle so far (Data Saver) - a full settings screen isn't worth it for a single switch. */
object SettingsDialog {

    fun show(context: Context) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_settings, null)
        val dataSaverSwitch = view.findViewById<SwitchMaterial>(R.id.dataSaverSwitch)
        dataSaverSwitch.isChecked = PlaybackPrefs.isDataSaverEnabled(context)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_title)
            .setView(view)
            .setPositiveButton(R.string.settings_done) { _, _ ->
                PlaybackPrefs.setDataSaverEnabled(context, dataSaverSwitch.isChecked)
            }
            .setNegativeButton(R.string.add_app_cancel, null)
            .show()
    }
}
