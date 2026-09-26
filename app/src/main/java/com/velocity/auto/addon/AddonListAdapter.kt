package com.velocity.auto.addon

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.velocity.auto.R

class AddonListAdapter(
    private val onClick: (String) -> Unit,
    private val onLongClick: (String) -> Unit
) : RecyclerView.Adapter<AddonListAdapter.AddonViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(manifestUrls: List<String>) {
        items.clear()
        items.addAll(manifestUrls)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_addon, parent, false)
        return AddonViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddonViewHolder, position: Int) {
        holder.bind(items[position], onClick, onLongClick)
    }

    override fun getItemCount(): Int = items.size

    class AddonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.addonName)
        private val urlView: TextView = itemView.findViewById(R.id.addonUrl)

        fun bind(manifestUrl: String, onClick: (String) -> Unit, onLongClick: (String) -> Unit) {
            nameView.text = Uri.parse(manifestUrl).host ?: manifestUrl
            urlView.text = manifestUrl
            itemView.setOnClickListener { onClick(manifestUrl) }
            itemView.setOnLongClickListener {
                onLongClick(manifestUrl)
                true
            }
        }
    }
}
