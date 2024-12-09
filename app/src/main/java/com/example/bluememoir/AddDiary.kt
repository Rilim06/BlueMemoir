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
import com.google.android.gms.location.FusedLocationProviderClient
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
import com.google.android.gms.location.LocationServices
import android.location.Geocoder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AddDiary : Fragment() {

    private lateinit var imageButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoChange: ImageButton
    private lateinit var photoCard: CardView
    private lateinit var backButton: ImageButton
    private lateinit var editLocationButton: ImageButton
    private lateinit var dateText: TextView
    private lateinit var locationView: TextView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var photoUri: Uri? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private var downloadPath: String? = null
    private var latitude: String? = null
    private var longitude: String? = null
    private var newLat: String? = null
    private var newLng: String? = null
    private var country: String? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: TagAdapter
    private val tags = mutableListOf<Tag>()
    private var selectedTag: Tag? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Request location permission
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                fetchLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

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

        fetchLocation()

        imageButton = view.findViewById(R.id.addPhoto) // ImageButton ID
        saveButton = view.findViewById(R.id.saveButton) // Add a save button
        photoView = view.findViewById(R.id.photoView) // ImageView ID
        photoChange = view.findViewById(R.id.changeButton) // ChangeButton ID
        photoCard = view.findViewById(R.id.photoCardView) // photoCard ID
        backButton = view.findViewById(R.id.backButton) // BackButton ID
        dateText = view.findViewById(R.id.date) // DateText ID
        locationView = view.findViewById(R.id.addLocation) // LocationText ID
        editLocationButton = view.findViewById(R.id.editLocationButton) // EditLocationButton ID

        val currentDate = Calendar.getInstance().time
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)
        dateText.text = formattedDate

        backButton.setOnClickListener{
            (activity as MainActivity).replaceFragment(Home())
        }

        imageButton.setOnClickListener {
            showImageSourceDialog()
        }

        photoChange.setOnClickListener {
            showImageSourceDialog()
        }

        saveButton.setOnClickListener {
            val title = view.findViewById<EditText>(R.id.addTitle)
            val text = view.findViewById<EditText>(R.id.addText)

            if(title != null && text != null && photoUri != null){
                saveDiary()
            }else{
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        editLocationButton.setOnClickListener {
            val fragment = MapEdit().apply {
                arguments = Bundle().apply {
                    putDouble("latitude", latitude?.toDouble() ?: 0.0)
                    putDouble("longitude", longitude?.toDouble() ?: 0.0)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listen for location result from MapEdit
        parentFragmentManager.setFragmentResultListener("editLocationResult", viewLifecycleOwner) { _, bundle ->
            val newLatitude = bundle.getDouble("latitude")
            val newLongitude = bundle.getDouble("longitude")

            // Update state with new location
            newLat = newLatitude.toString()
            newLng = newLongitude.toString()

            // Update location display
            country = getCityAndCountryFromCoordinates(newLatitude, newLongitude)
            locationView.text = country
            Log.d("AddDiary", "New location: $country")
        }

        if (photoUri != null) {
            photoView.setImageURI(photoUri)
            swapView()
        }

        tagRecyclerView = view.findViewById(R.id.tagSlider)
        tagRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        tagAdapter = TagAdapter(tags) { tag ->
            selectedTag = tag
        }
        tagRecyclerView.adapter = tagAdapter

        fetchTag()

    }

    private fun fetchTag() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Tag")
            .get()
            .addOnSuccessListener { querySnapshot ->
                tags.clear()
                for (doc in querySnapshot.documents) {
                    val id = doc.id
                    val name = doc.getString("name")
                    if (name != null) {
                        tags.add(Tag(id, name))
                    }
                }
                tagAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("AddDiary", "Error fetching tags", e)
            }
    }

    private fun fetchLocation() {

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latitude = location.latitude.toString()
                    longitude = location.longitude.toString()

                    if(newLat != null && newLng != null){
                        latitude = newLat
                        longitude = newLng
                    }
                    country = getCityAndCountryFromCoordinates(latitude!!.toDouble(), longitude!!.toDouble())
                    locationView.text = country
                } else {
                    Toast.makeText(requireContext(), "Unable to get location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // Request location permissions
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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

        val latitude = latitude
        val longitude = longitude
        val tagId = selectedTag?.id
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

            val locationIdNew = firestore.collection("Location").add(locationDetail).await()
            val locationId = locationIdNew.id

            val diary = hashMapOf(
                "tagId" to tagId,
                "locationId" to locationId,
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

data class Tag(
    val id: String,
    val name: String
)
