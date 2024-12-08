package com.example.bluememoir

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MapView : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_map_view, container, false)
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            googleMap = map
            googleMap.setInfoWindowAdapter(InfoWindowAdapter(requireContext()))
            googleMap.setOnInfoWindowClickListener { marker ->
                val markerData = marker.tag as? MarkerData
                markerData?.let { navigateToDetailDiary(it.detailId) }
            }
            retrieveDataAndAddPins()
        }
        return view
    }

    private fun retrieveDataAndAddPins() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        db.collection("Diary")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                var firstLocationLatLng: LatLng? = null
                var documentsProcessed = 0

                for (document in result) {
                    val detailId = document.getString("detailId") ?: continue
                    val locationId = document.getString("locationId") ?: continue

                    // Fetch title from DiaryDetail
                    db.collection("DiaryDetail").document(detailId)
                        .get()
                        .addOnSuccessListener { detailDocument ->
                            val title = detailDocument.getString("title") ?: "Untitled"
                            val imagePath = detailDocument.getString("photo") ?: ""

                            // Fetch location from Location collection
                            db.collection("Location").document(locationId)
                                .get()
                                .addOnSuccessListener { locationDocument ->
                                    val lat = locationDocument.getString("latitude")?.toDoubleOrNull()
                                    val lng = locationDocument.getString("longitude")?.toDoubleOrNull()

                                    if (lat != null && lng != null) {
                                        val latLng = LatLng(lat, lng)

                                        // Add marker to the map
                                        val marker = googleMap.addMarker(
                                            MarkerOptions()
                                                .position(latLng)
                                                .title(title)
                                                .snippet("Location ID: $locationId")
                                        )
                                        marker?.tag = MarkerData(detailId, title, imagePath)


                                        // Track the first location to move the camera
                                        if (firstLocationLatLng == null) {
                                            firstLocationLatLng = latLng
                                        }
                                    }
                                    documentsProcessed++

                                    // Move camera once all documents are processed
                                    if (documentsProcessed == result.size()) {
                                        firstLocationLatLng?.let {
                                            googleMap.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(it, 10f)
                                            )
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    Log.e("MapFragment", "Failed to fetch location for ID: $locationId")
                                }
                        }
                        .addOnFailureListener {
                            Log.e("MapFragment", "Failed to fetch detail for ID: $detailId")
                        }
                }

                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No diaries found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error loading diaries: $exception", Toast.LENGTH_SHORT).show()
            }

    }

    private fun navigateToDetailDiary(detailId: String) {
        val detailFragment = DetailDiary()
        val bundle = Bundle().apply {
            putString("detailId", detailId)
        }
        detailFragment.arguments = bundle
        (activity as MainActivity).replaceFragment(detailFragment)
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

data class MarkerData(
    val detailId: String,
    val title: String,
    val imagePath: String
)
