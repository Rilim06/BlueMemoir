package com.example.bluememoir

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapEdit : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var saveButton: ImageButton
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get latitude and longitude from arguments
        arguments?.let {
            selectedLat = it.getDouble("latitude")
            selectedLng = it.getDouble("longitude")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map_edit, container, false)

        mapView = view.findViewById(R.id.mapView)
        saveButton = view.findViewById(R.id.saveLocation)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            val initialLat = selectedLat
            val initialLng = selectedLng
            val initialLocation = LatLng(initialLat, initialLng)

            // Move camera to initial location
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))

            // Add a marker at the initial location
            var marker = googleMap.addMarker(MarkerOptions().position(initialLocation).title("Selected Location"))

            // Update coordinates and marker position as the map moves
            googleMap.setOnCameraIdleListener {
                val center = googleMap.cameraPosition.target

                // Update selectedLat and selectedLng
                selectedLat = center.latitude
                selectedLng = center.longitude

                marker?.remove()
                marker = googleMap.addMarker(
                    MarkerOptions().position(center).title("Selected Location")
                )
            }
        }


        saveButton.setOnClickListener {
            val result = Bundle().apply {
                putDouble("latitude", selectedLat)
                putDouble("longitude", selectedLng)
            }
            parentFragmentManager.setFragmentResult("editLocationResult", result)
            parentFragmentManager.popBackStack() // Return to AddDiary
        }


        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val initialLocation = LatLng(selectedLat, selectedLng)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f))

        // Keep a static center pin (optional)
        val markerOptions = MarkerOptions().position(initialLocation).title("Selected Location")
        var marker = googleMap.addMarker(markerOptions)

        googleMap.setOnCameraIdleListener {
            val center = googleMap.cameraPosition.target
            selectedLat = center.latitude
            selectedLng = center.longitude

            // Update the marker's position dynamically
            marker?.position = center
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
