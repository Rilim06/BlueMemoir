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
import android.widget.TextView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDiary : Fragment() {

    private lateinit var imageButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoChange: ImageButton
    private lateinit var photoCard: CardView
    private lateinit var backButton: ImageButton
    private lateinit var dateText: TextView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var photoUri: Uri? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private var downloadPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        auth = FirebaseAuth.getInstance()

        // Register the camera and gallery result launchers
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the image capture and store its URI
                val bitmap = result.data?.extras?.get("data") as Bitmap
                swapView()
                photoView.setImageBitmap(bitmap) // Display the captured image on the ImageButton
                // Convert bitmap to URI
                photoUri = saveImageToCache(bitmap)
            }
        }

        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val selectedImageUri: Uri? = result.data?.data
                swapView()
                photoView.setImageURI(selectedImageUri) // Display the selected image on the ImageButton
                photoUri = selectedImageUri // Store the URI for further use
            }
        }

        // Register permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera() // Permission granted, open camera
            } else {
                // Permission denied
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_diary, container, false)

        imageButton = view.findViewById(R.id.addPhoto) // ImageButton ID
        saveButton = view.findViewById(R.id.saveButton) // Add a save button
        photoView = view.findViewById(R.id.photoView) // ImageView ID
        photoChange = view.findViewById(R.id.changeButton) // ChangeButton ID
        photoCard = view.findViewById(R.id.photoCardView) // photoCard ID
        backButton = view.findViewById(R.id.backButton) // BackButton ID
        dateText = view.findViewById(R.id.date) // DateText ID

        val currentDate = Calendar.getInstance().time
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)
        dateText.text = formattedDate

        backButton.setOnClickListener{
            (activity as MainActivity).replaceFragment(Home())
        }

        imageButton.setOnClickListener {
            showImageSourceDialog() // Show the dialog to select image source
        }

        photoChange.setOnClickListener {
            showImageSourceDialog() // Show the dialog to select image source
        }

        saveButton.setOnClickListener {
            val title = view.findViewById<EditText>(R.id.addTitle)
            val text = view.findViewById<EditText>(R.id.addText)

            if(title != null && text != null && photoUri != null){
                saveDiary() // Save diary details when save button is clicked
            }else{
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Camera", "Gallery")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Select Image Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> checkPermissionsAndOpenCamera() // Camera option selected
                1 -> openGallery() // Gallery option selected
            }
        }
        builder.show()
    }

    private fun checkPermissionsAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                openCamera() // Permissions granted, open camera
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA) // Request camera permission
            }
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
        // Function to save bitmap to cache and return its URI
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(requireActivity().contentResolver, bitmap, "Title", null)
        return Uri.parse(path)
    }

    private fun saveDiary() {
        val location = "Abydos" // Dummy data
        val latitude = "0.0" // Dummy data
        val longitude = "0.0" // Dummy data
        val tagId = "kJ3nw0aB27e4fgLnWlt3" // Travel
        val currentDate = Calendar.getInstance().time
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)
        val title = view?.findViewById<EditText>(R.id.addTitle)
        val text = view?.findViewById<EditText>(R.id.addText)

        val titleText = title?.text.toString()
        val bodyText = text?.text.toString()

        val user = auth.currentUser
        val userId = user?.uid

        // Upload photo to Firebase Storage
        CoroutineScope(Dispatchers.Main).launch{
            Toast.makeText(requireContext(), "Saving Diary...", Toast.LENGTH_SHORT).show()
            val downloadPath = withContext(Dispatchers.IO) {
                uploadImageToFirebase(photoUri!!)
            }

            // Save details to Firestore
            val diaryDetail = hashMapOf(
                "title" to titleText,
                "photo" to downloadPath,
                "text" to bodyText,
                "isFavorite" to false,
                "date" to formattedDate,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val detailIdNew = firestore.collection("DiaryDetail").add(diaryDetail).await() // Await the result to get document reference
            val detailId = detailIdNew.id

            val locationDetail = hashMapOf(
                "longitude" to longitude,
                "latitude" to latitude
            )

            firestore.collection("Location").add(locationDetail)

            val diary = hashMapOf(
                "tagId" to tagId,
                "locationId" to location,
                "detailId" to detailId,
                "userId" to userId,
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection("Diary")
                .add(diary)
                .addOnSuccessListener {
                    redirectToHome()
                    Toast.makeText(requireContext(), "Diary saved!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun uploadImageToFirebase(imageUri: Uri): String = suspendCancellableCoroutine { continuation ->
        val fileName = "images/${System.currentTimeMillis()}.jpg" // Create a unique file name
        val fileReference = storageReference.child(fileName) // Reference to the file in storage

        // Upload the image
        fileReference.putFile(imageUri)
            .addOnSuccessListener {
                // Get the download URL
                fileReference.downloadUrl.addOnSuccessListener { downloadUri ->
                    // Resume the coroutine with the download URL
                    continuation.resume(downloadUri.toString())
                }.addOnFailureListener { exception ->
                    // Resume the coroutine with an exception
                    continuation.resumeWithException(exception)
                }
            }
            .addOnFailureListener { exception ->
                // Resume the coroutine with an exception
                continuation.resumeWithException(exception)
            }
    }

    private fun swapView() {
        imageButton.visibility = View.GONE
        photoChange.visibility = View.VISIBLE
        photoCard.visibility = View.VISIBLE
    }

    private fun redirectToHome() {
        val intent = Intent(activity, MainActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}
