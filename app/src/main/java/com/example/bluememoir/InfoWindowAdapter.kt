package com.example.bluememoir

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class InfoWindowAdapter(val context: Context) : GoogleMap.InfoWindowAdapter {
    private val windowView: View = LayoutInflater.from(context).inflate(R.layout.info_window, null)

    override fun getInfoWindow(marker: Marker): View? {
        return null // Use default background frame.
    }

    override fun getInfoContents(marker: Marker): View {
        val diaryTitle = windowView.findViewById<TextView>(R.id.diaryTitle)
        val diaryImage = windowView.findViewById<ImageView>(R.id.diaryImage)

        // Get data from marker's tag
        val markerData = marker.tag as? MarkerData
        val imagePath = markerData?.imagePath

        diaryTitle.text = markerData?.title ?: "Untitled"

        Log.d("InfoWindowAdapter", "Image Path: $imagePath")

        // Load image using Glide
        Glide.with(context)
            .load(imagePath)
            .placeholder(R.drawable.photo_no)
            .error(R.drawable.photo_no)
            .transform(CenterCrop(), RoundedCorners(16))
            .into(diaryImage)

        return windowView
    }
}
