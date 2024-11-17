package com.example.bluememoir

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class Setting : Fragment() {

    private lateinit var saveButton: Button
    private lateinit var profileView: ImageView
    private lateinit var photoView: ImageView
    private lateinit var changePhotoButton: ImageButton
    private lateinit var nameField: EditText
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var photoUri: Uri? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private var initialName: String? = null
    private var initialPhotoUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        auth = FirebaseAuth.getInstance()

        // Initialize result launchers for camera and gallery
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as Bitmap
                swapView()
                photoUri = saveImageToCache(bitmap)
                photoView.setImageBitmap(bitmap)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                photoUri = result.data?.data
                swapView()
                photoView.setImageURI(photoUri)
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Initialize UI components
        saveButton = view.findViewById(R.id.saveButton)
        profileView = view.findViewById(R.id.profileView)
        photoView = view.findViewById(R.id.photoView)
        changePhotoButton = view.findViewById(R.id.changeButton)
        nameField = view.findViewById(R.id.addName)

        // Load initial data from Firebase
        loadProfileData()

        saveButton.setOnClickListener {
            if (nameField.text.isNotEmpty()) {
                updateProfile()
            } else {
                Toast.makeText(requireContext(), "Please fill the name field", Toast.LENGTH_SHORT).show()
            }
        }

        changePhotoButton.setOnClickListener { showImageSourceDialog() }

        return view
    }

    private fun swapView() {
        profileView.visibility = View.GONE
        photoView.visibility = View.VISIBLE
    }

    private fun loadProfileData() {
        val userId = auth.currentUser?.uid ?: return

        // Query to get the document ID for the current user's profile
        firestore.collection("Profile")
            .whereEqualTo("userId", userId) // Assuming you have a userId field in your Profile collection
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Get the first document
                    initialName = document.getString("name")
                    initialPhotoUrl = document.getString("photo")

                    nameField.setText(initialName)
                    if (initialPhotoUrl != null) {
                        swapView()
                        Glide.with(this).load(initialPhotoUrl).into(photoView)
                    }
                } else {
                    Toast.makeText(requireContext(), "No profile found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Failed to load data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile() {
        val userId = auth.currentUser?.uid ?: return
        val updateData = mutableMapOf<String, Any?>("name" to nameField.text.toString())

        CoroutineScope(Dispatchers.Main).launch {
            // Query to get the document ID for the current user's profile
            val querySnapshot = firestore.collection("Profile")
                .whereEqualTo("userId", userId) // Assuming you have a userId field in your Profile collection
                .get()
                .await() // Wait for the result

            if (!querySnapshot.isEmpty) {
                val documentId = querySnapshot.documents[0].id // Get the document ID

                if (photoUri != null) {
                    updateData["photo"] = withContext(Dispatchers.IO) { uploadImageToFirebase(photoUri!!) }
                } else {
                    updateData["photo"] = initialPhotoUrl
                }

                firestore.collection("Profile").document(documentId) // Update using the document ID
                    .update(updateData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        loadProfileData() // Refresh data
                        (activity as MainActivity).replaceFragment(Profile())
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "No profile found to update", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkPermissionsAndOpenCamera()
                    1 -> openGallery()
                }
            }.show()
    }

    private fun checkPermissionsAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> openCamera()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun saveImageToCache(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireActivity().contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }


    private suspend fun uploadImageToFirebase(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val fileName = "profile_images/${System.currentTimeMillis()}.jpg"
        val fileReference = storageReference.child(fileName)
        fileReference.putFile(imageUri).await()
        fileReference.downloadUrl.await().toString()
    }
}
