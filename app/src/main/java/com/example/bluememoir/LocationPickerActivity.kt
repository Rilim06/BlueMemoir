package com.example.bluememoir

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.GoogleMap.OnMapClickListener

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        // Initialize the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set a default location (Example: Jakarta)
        val defaultLocation = LatLng(-6.2, 106.816) // Jakarta
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // Handle map click to select a location
        mMap.setOnMapClickListener(OnMapClickListener { latLng ->
            // Clear previous markers
            mMap.clear()

            // Add a marker to the selected location
            mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))

            // Save the selected location
            selectedLocation = latLng
        })
    }

    override fun onBackPressed() {
        // Pass the selected location back to the previous activity
        selectedLocation?.let {
            val resultIntent = Intent()
            resultIntent.putExtra("latitude", it.latitude)
            resultIntent.putExtra("longitude", it.longitude)
            setResult(Activity.RESULT_OK, resultIntent)
        }
        super.onBackPressed()
    }
}
