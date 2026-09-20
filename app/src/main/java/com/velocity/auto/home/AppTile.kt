package com.velocity.auto.home

data class AppTile(
    val id: String,
    val label: String,
    val iconRes: Int,
    val tintColor: Int,
    val kind: Kind,
    val url: String? = null,
    val removable: Boolean = false
) {
    enum class Kind { YOUTUBE, WEB, ADD_APP }
}
