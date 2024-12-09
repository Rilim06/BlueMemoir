package com.example.bluememoir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class TagAdapter(private val tags: List<Tag>, private val onTagClick: (Tag) -> Unit) :
    RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION

    fun setInitialSelectedTag(tagId: String) {
        val initialPosition = tags.indexOfFirst { it.id == tagId }
        if (initialPosition != -1) {
            selectedPosition = initialPosition
            notifyItemChanged(selectedPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        holder.tagName.text = tag.name

        holder.itemView.isSelected = position == selectedPosition
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onTagClick(tag)
        }
    }

    override fun getItemCount() = tags.size

    class TagViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tagName: TextView = view.findViewById(R.id.tagName)
    }
}
