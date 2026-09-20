package com.velocity.auto.home

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.velocity.auto.R

/** A tiny name+URL prompt for pinning another web app (Disney+, Max, a work portal…) to the home grid. */
object AddAppDialog {

    fun show(context: Context, onAdd: (name: String, url: String) -> Unit) {
        val view = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_add_app, null)
        val nameField = view.findViewById<TextInputEditText>(R.id.inputName)
        val urlField = view.findViewById<TextInputEditText>(R.id.inputUrl)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.add_app_title)
            .setView(view)
            .setPositiveButton(R.string.add_app_save) { _, _ ->
                val name = nameField.text?.toString()?.trim().orEmpty()
                val url = normalizeUrl(urlField.text?.toString()?.trim().orEmpty())
                if (name.isNotEmpty() && url != null) {
                    onAdd(name, url)
                }
            }
            .setNegativeButton(R.string.add_app_cancel, null)
            .show()
    }

    private fun normalizeUrl(input: String): String? {
        if (input.isEmpty()) return null
        return if (input.startsWith("http://") || input.startsWith("https://")) {
            input
        } else {
            "https://$input"
        }
    }
}
