package com.velocity.auto.youtube

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.velocity.auto.R
import com.velocity.auto.youtube.model.YtVideoItem

class YouTubeResultsAdapter(
    private val onClick: (YtVideoItem) -> Unit
) : RecyclerView.Adapter<YouTubeResultsAdapter.ResultViewHolder>() {

    private val items = mutableListOf<YtVideoItem>()

    fun submitList(videos: List<YtVideoItem>) {
        items.clear()
        items.addAll(videos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_youtube_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.resultThumbnail)
        private val title: TextView = itemView.findViewById(R.id.resultTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.resultSubtitle)

        fun bind(video: YtVideoItem, onClick: (YtVideoItem) -> Unit) {
            title.text = video.title
            subtitle.text = itemView.context.getString(
                R.string.result_subtitle_format,
                video.uploader,
                video.formattedDuration()
            )
            thumbnail.load(video.thumbnailUrl) {
                crossfade(false)
            }
            itemView.setOnClickListener { onClick(video) }
        }
    }
}
