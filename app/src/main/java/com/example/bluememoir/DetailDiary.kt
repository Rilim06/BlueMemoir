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

class DetailDiary : Fragment() {

    private var detailId: String? = null
    private lateinit var titleView: TextView
    private lateinit var dateView: TextView
    private lateinit var photoView: ImageView
    private lateinit var viewText: TextView
    private lateinit var favoriteButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var editButton: ImageButton
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
        favoriteButton = view.findViewById(R.id.favoriteButton)
        backButton = view.findViewById(R.id.backButton)
        editButton = view.findViewById(R.id.editButton)

        // Load diary details and initialize isFavorite state
        loadDiaryDetails()

        // Set up favorite toggle functionality
        favoriteButton.setOnClickListener {
            toggleFavoriteStatus()
        }

        backButton.setOnClickListener{
            (activity as MainActivity).replaceFragment(Home())
        }

        editButton.setOnClickListener {
            val editFragment = EditDiary()
            val bundle = Bundle().apply {
                putString("detailId", detailId)
            }
            editFragment.arguments = bundle
            (activity as MainActivity).replaceFragment(editFragment)
        }


        return view
    }

    private fun loadDiaryDetails() {
        detailId?.let { id ->
            // Retrieve data from Firestore
            db.collection("DiaryDetail").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Retrieve details from document
                        titleView.text = document.getString("title")
                        dateView.text = document.getString("date")
                        viewText.text = document.getString("text")
                        val imagePath = document.getString("photo")

                        // Load image with Glide
                        Glide.with(this)
                            .load(imagePath)
                            .into(photoView)

                        // Initialize favorite state
                        isFavorite = document.getBoolean("isFavorite") ?: false
                        favoriteButton.isSelected = isFavorite
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error loading diary details: $exception", Toast.LENGTH_SHORT).show()
                    Log.e("DetailDiary", "Error loading diary details", exception)
                }
        } ?: Log.e("DetailDiary", "detailId is null")
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
