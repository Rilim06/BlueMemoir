package com.example.bluememoir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SliderAdapter(
    private val dataList: List<MyModel>, // Pass the data for the slider
    private val clickListener: (String) -> Unit // Passes detailId to handle clicks
) : RecyclerView.Adapter<SliderAdapter.SliderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_slider_layout, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val model = dataList[position]
        holder.bind(model)
        // Set up click listener for each slider item
        holder.itemView.setOnClickListener {
            clickListener(model.detailId)
        }
    }

    override fun getItemCount(): Int = dataList.size

    class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoView: ImageView = itemView.findViewById(R.id.photoView)
        private val dateView: TextView = itemView.findViewById(R.id.dateView)
        private val titleView: TextView = itemView.findViewById(R.id.titleView)

        fun bind(model: MyModel) {
            Glide.with(itemView.context)
                .load(model.imagePath)
                .into(photoView)

            dateView.text = model.date
            titleView.text = model.title
        }
    }
}
