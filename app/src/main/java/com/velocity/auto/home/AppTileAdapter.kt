package com.velocity.auto.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.velocity.auto.R

class AppTileAdapter(
    private val onClick: (AppTile) -> Unit,
    private val onLongClick: (AppTile) -> Unit
) : RecyclerView.Adapter<AppTileAdapter.TileViewHolder>() {

    private val items = mutableListOf<AppTile>()

    fun submitList(tiles: List<AppTile>) {
        items.clear()
        items.addAll(tiles)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_tile, parent, false)
        return TileViewHolder(view)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) {
        holder.bind(items[position], onClick, onLongClick)
    }

    override fun getItemCount(): Int = items.size

    class TileViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.tileIcon)
        private val label: TextView = itemView.findViewById(R.id.tileLabel)

        fun bind(tile: AppTile, onClick: (AppTile) -> Unit, onLongClick: (AppTile) -> Unit) {
            icon.setImageResource(tile.iconRes)
            icon.imageTintList = android.content.res.ColorStateList.valueOf(tile.tintColor)
            label.text = tile.label
            itemView.contentDescription = itemView.context.getString(R.string.cd_app_icon, tile.label)
            itemView.setOnClickListener { onClick(tile) }
            itemView.setOnLongClickListener {
                if (tile.removable) {
                    onLongClick(tile)
                    true
                } else {
                    false
                }
            }
        }
    }
}
