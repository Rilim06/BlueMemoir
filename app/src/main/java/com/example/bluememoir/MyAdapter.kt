package com.example.bluememoir

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.storage.FirebaseStorage
import android.util.Log
import com.bumptech.glide.Glide


class MyAdapter(
    private val dataList: List<MyModel>,
    private val clickListener: (String) -> Unit // Passes detailId to handle clicks
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = dataList[position]
        holder.bind(model)
        // Set up click listener for each item in the RecyclerView
        holder.itemView.setOnClickListener {
            clickListener(model.detailId)
        }
    }

    override fun getItemCount(): Int = dataList.size

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

