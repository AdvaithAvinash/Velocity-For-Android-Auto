package com.velocity.auto.addon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.velocity.auto.R

class StreamAdapter(
    private val onClick: (StreamOption) -> Unit
) : RecyclerView.Adapter<StreamAdapter.StreamViewHolder>() {

    private val items = mutableListOf<StreamOption>()

    fun submitList(streams: List<StreamOption>) {
        items.clear()
        items.addAll(streams)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stream, parent, false)
        return StreamViewHolder(view)
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.streamTitle)
        private val subtitle: TextView = itemView.findViewById(R.id.streamSubtitle)

        fun bind(stream: StreamOption, onClick: (StreamOption) -> Unit) {
            title.text = stream.title
            if (stream.isPlayable) {
                subtitle.text = itemView.context.getString(R.string.stream_direct)
                itemView.alpha = 1f
                itemView.isEnabled = true
                itemView.setOnClickListener { onClick(stream) }
            } else {
                subtitle.text = itemView.context.getString(R.string.stream_unsupported)
                subtitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_dim))
                itemView.alpha = 0.55f
                itemView.isEnabled = false
                itemView.setOnClickListener(null)
            }
        }
    }
}
