package com.eleeth.tv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ChannelAdapter(
    private val onClick: (Channel) -> Unit
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private val items = mutableListOf<Channel>()

    fun replace(newItems: List<Channel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logo: ImageView = itemView.findViewById(R.id.chLogo)
        private val name: TextView = itemView.findViewById(R.id.chName)
        private val group: TextView = itemView.findViewById(R.id.chGroup)

        fun bind(ch: Channel) {
            name.text = ch.name
            val firstGroup = ch.group.split(";").firstOrNull()?.trim() ?: ""
            group.text = firstGroup
            group.visibility = if (firstGroup.isNotEmpty()) View.VISIBLE else View.GONE
            if (ch.logo.isNotEmpty()) {
                Glide.with(logo)
                    .load(ch.logo)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .into(logo)
            } else {
                logo.setImageResource(android.R.color.darker_gray)
            }
            itemView.setOnClickListener { onClick(ch) }
        }
    }
}