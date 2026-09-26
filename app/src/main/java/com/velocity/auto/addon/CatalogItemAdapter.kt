package com.velocity.auto.addon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.velocity.auto.R

class CatalogItemAdapter(
    private val onClick: (CatalogItem) -> Unit
) : RecyclerView.Adapter<CatalogItemAdapter.CatalogItemViewHolder>() {

    private val items = mutableListOf<CatalogItem>()

    fun submitList(newItems: List<CatalogItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatalogItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_catalog_item, parent, false)
        return CatalogItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CatalogItemViewHolder, position: Int) {
        holder.bind(items[position], onClick)
    }

    override fun getItemCount(): Int = items.size

    class CatalogItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val poster: ImageView = itemView.findViewById(R.id.posterImage)
        private val name: TextView = itemView.findViewById(R.id.itemName)

        fun bind(item: CatalogItem, onClick: (CatalogItem) -> Unit) {
            name.text = item.name
            poster.load(item.posterUrl) { crossfade(false) }
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
