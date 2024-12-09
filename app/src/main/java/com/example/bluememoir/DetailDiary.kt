package com.example.bluememoir

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import android.location.Geocoder
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale

class DetailDiary : Fragment() {

    private var detailId: String? = null
    private var latDouble: Double = 0.0
    private var lonDouble: Double = 0.0
    private var latitude: String = ""
    private var longitude: String = ""
    private lateinit var titleView: TextView
    private lateinit var dateView: TextView
    private lateinit var photoView: ImageView
    private lateinit var locationView: TextView
    private lateinit var tagView: TextView
    private lateinit var viewText: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var editButton: ImageButton
    private lateinit var deleteButton: ImageButton
    private lateinit var locationButton: ImageButton

    private lateinit var db: FirebaseFirestore
    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the detailId from arguments
        detailId = arguments?.getString("detailId")
        Log.d("DetailDiary", "Retrieved detailId: $detailId")

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detail_diary, container, false)

        // Initialize views
        titleView = view.findViewById(R.id.titleView)
        dateView = view.findViewById(R.id.dateView)
        photoView = view.findViewById(R.id.photoView)
        viewText = view.findViewById(R.id.viewText)
        locationView = view.findViewById(R.id.locationView)
        favoriteButton = view.findViewById(R.id.favoriteButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        backButton = view.findViewById(R.id.backButton)
        editButton = view.findViewById(R.id.editButton)
        locationButton = view.findViewById(R.id.locationButton)
        tagView = view.findViewById(R.id.tagView)

        // Load diary details and initialize isFavorite state
        loadDiaryDetails()

        // Set up favorite toggle functionality
        favoriteButton.setOnClickListener {
            toggleFavoriteStatus()
        }

        backButton.setOnClickListener{
            (activity as MainActivity).replaceFragment(Home())
        }

        deleteButton.setOnClickListener {
            detailId?.let { id ->
                // Fetch the image path from Firestore before deleting the entry
                db.collection("DiaryDetail").document(id)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document != null && document.exists()) {
                            val imagePath = document.getString("photo")!!.substringAfter("/o/").substringBefore("?").replace("%2F", "/")

                            // Delete the image from Firebase Storage
                            val storageRef = FirebaseStorage.getInstance().getReference(imagePath)
                            storageRef.delete()
                                .addOnSuccessListener {
                                    Log.d("DetailDiary", "Image deleted successfully from Firebase Storage")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("DetailDiary", "Failed to delete image from Firebase Storage", e)
                                }

                            // Proceed to delete the diary entry
                            db.collection("DiaryDetail").document(id)
                                .delete()
                                .addOnSuccessListener {
                                    db.collection("Diary")
                                        .whereEqualTo("detailId", id)
                                        .get()
                                        .addOnSuccessListener { diaryResult ->
                                            if (!diaryResult.isEmpty) {
                                                for (diaryDoc in diaryResult) {
                                                    diaryDoc.reference.delete()
                                                    Log.d("DetailDiary", "Deleted Diary entry with id: ${diaryDoc.id}")
                                                }
                                            }
                                            Toast.makeText(requireContext(), "Diary entry deleted", Toast.LENGTH_SHORT).show()
                                            // Navigate back to Home
                                            (activity as MainActivity).replaceFragment(Home())
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(requireContext(), "Failed to delete diary entry from Diary: $e", Toast.LENGTH_SHORT).show()
                                            Log.e("DetailDiary", "Error deleting diary entry from Diary", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(requireContext(), "Failed to delete diary entry from DiaryDetail: $e", Toast.LENGTH_SHORT).show()
                                    Log.e("DetailDiary", "Error deleting diary entry from DiaryDetail", e)
                                }
                        } else {
                            Log.e("DetailDiary", "Document does not exist for detailId: $id")
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to fetch diary details for deletion: $e", Toast.LENGTH_SHORT).show()
                        Log.e("DetailDiary", "Error fetching diary details for deletion", e)
                    }
            } ?: Log.e("DetailDiary", "detailId is null when trying to delete diary entry")
        }

        editButton.setOnClickListener {
            val editFragment = EditDiary()
            val bundle = Bundle().apply {
                putString("detailId", detailId)
            }
            editFragment.arguments = bundle
            (activity as MainActivity).replaceFragment(editFragment)
        }

        locationButton.setOnClickListener {

            val mapDetailFragment = MapDetail()
            val bundle = Bundle().apply {
                putDouble("latitude", latDouble)
                putDouble("longitude", lonDouble)
            }
            mapDetailFragment.arguments = bundle

            // Navigate to the map fragment
            (activity as MainActivity).replaceFragment(mapDetailFragment)
        }

        return view
    }

    private fun loadDiaryDetails() {
        detailId?.let { id ->
            db.collection("Diary")
                .whereEqualTo("detailId", id)
                .get()
                .addOnSuccessListener { diarySnapshot ->
                    if (!diarySnapshot.isEmpty) {
                        val diaryDocument = diarySnapshot.documents[0]
                        val tagId = diaryDocument.getString("tagId")

                        db.collection("DiaryDetail").document(id)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {

                                    titleView.text = document.getString("title")
                                    dateView.text = document.getString("date")
                                    viewText.text = document.getString("text")
                                    val imagePath = document.getString("photo")

                                    if (tagId != null) {
                                        db.collection("Tag").document(tagId)
                                            .get()
                                            .addOnSuccessListener { tagDocument ->
                                                if (tagDocument != null && tagDocument.exists()) {
                                                    val tagName = tagDocument.getString("name")
                                                    tagView.text = tagName
                                                }
                                            }
                                    }
                                    Glide.with(this)
                                        .load(imagePath)
                                        .into(photoView)

                                    isFavorite = document.getBoolean("isFavorite") ?: false
                                    favoriteButton.isSelected = isFavorite
                                }
                                fetchLocationId(id)
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(requireContext(), "Error loading diary details: $exception", Toast.LENGTH_SHORT).show()
                                Log.e("DetailDiary", "Error loading diary details", exception)
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error loading diary details: $exception", Toast.LENGTH_SHORT).show()
                    Log.e("DetailDiary", "Error loading diary data", exception)
                }
        } ?: Log.e("DetailDiary", "detailId is null")
    }

    private fun fetchLocationId(detailId: String) {
        db.collection("Diary")
            .whereEqualTo("detailId", detailId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val locationId = document.getString("locationId")
                    if (locationId != null) {
                        // Fetch latitude and longitude based on locationId
                        fetchLocationDetails(locationId)
                    } else {
                        Log.e("DetailDiary", "locationId is null")
                    }
                } else {
                    Log.e("DetailDiary", "No Diary document found for detailId: $detailId")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching locationId: $exception", Toast.LENGTH_SHORT).show()
                Log.e("DetailDiary", "Error fetching locationId", exception)
            }
    }

    private fun fetchLocationDetails(locationId: String) {
        db.collection("Location").document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    latitude = document.getString("latitude").toString()
                    longitude = document.getString("longitude").toString()

                    // Convert latitude and longitude to Double and fetch country name
                    latDouble = latitude.toDouble()
                    lonDouble = longitude.toDouble()

                    val countryName = getCityAndCountryFromCoordinates(latDouble, lonDouble)
                    locationView.text = countryName ?: "Country not found"
                } else {
                    Log.e("DetailDiary", "Location document does not exist for locationId: $locationId")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching location details: $exception", Toast.LENGTH_SHORT).show()
                Log.e("DetailDiary", "Error fetching location details", exception)
            }
    }

    private fun getCityAndCountryFromCoordinates(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses!!.isNotEmpty()) {
                val city = addresses[0].locality
                val country = addresses[0].countryName
                if (city != null) "$city, $country" else country // Return city and country, or just country
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun toggleFavoriteStatus() {
        isFavorite = !isFavorite
        favoriteButton.isSelected = isFavorite

        // Update `isFavorite` in Firebase
        detailId?.let { id ->
            db.collection("DiaryDetail").document(id)
                .update("isFavorite", isFavorite)
                .addOnSuccessListener {
                    val message = if (isFavorite) "Added to favorites." else "Removed from favorites."
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to update favorite status: $e", Toast.LENGTH_SHORT).show()
                    Log.e("DetailDiary", "Error updating favorite status", e)
                }
        } ?: Log.e("DetailDiary", "detailId is null when updating favorite status")
    }
}
