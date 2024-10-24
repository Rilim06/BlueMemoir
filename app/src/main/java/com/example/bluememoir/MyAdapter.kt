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


class MyAdapter(private val dataList: List<MyModel>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val model = dataList[position]
        val storageReference = FirebaseStorage.getInstance().reference.child(model.imagePath)

        Glide.with(holder.itemView.context)
            .load(model.imagePath)
            .into(holder.photoView)

        holder.titleView.text = model.title
        holder.dateView.text = model.date
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: ImageView = itemView.findViewById(R.id.photoView)
        val dateView: TextView = itemView.findViewById(R.id.dateView)
        val titleView: TextView = itemView.findViewById(R.id.titleView)
    }
}
