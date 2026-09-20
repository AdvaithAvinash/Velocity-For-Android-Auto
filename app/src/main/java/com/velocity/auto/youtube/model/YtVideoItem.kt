package com.velocity.auto.youtube.model

data class YtVideoItem(
    val title: String,
    val url: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?
) {
    fun formattedDuration(): String {
        val total = durationSeconds.coerceAtLeast(0)
        val hours = total / 3600
        val minutes = (total % 3600) / 60
        val seconds = total % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }
}
