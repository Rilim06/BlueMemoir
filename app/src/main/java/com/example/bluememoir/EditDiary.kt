package com.example.bluememoir

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditDiary : Fragment() {

    private lateinit var imageButton: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoChange: ImageButton
    private lateinit var photoCard: CardView
    private lateinit var backButton: ImageButton
    private lateinit var dateText: TextView
    private lateinit var addLocation: TextView
    private lateinit var titleField: EditText
    private lateinit var textField: EditText
    private lateinit var editLocation: ImageButton
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUri: Uri? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private var downloadPath: String? = null
    private var detailId: String? = null // Added detailId

    private var initialTitle: String? = null
    private var initialText: String? = null
    private var initialPhotoUrl: String? = null

    private var initialLocationId: String? = null
    private var latitude: String? = null
    private var longitude: String? = null
    private var newLat: String? = null
    private var newLng: String? = null
    private var country: String? = null

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: TagAdapter
    private val tags = mutableListOf<Tag>()
    private var selectedTag: Tag? = null
    private lateinit var initialTag: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        detailId = arguments?.getString("detailId") // Retrieve detailId

        // Initialize result launchers for camera and gallery
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as Bitmap
                swapView()
                photoView.setImageBitmap(bitmap)
                photoUri = saveImageToCache(bitmap)
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
        val view = inflater.inflate(R.layout.fragment_add_diary, container, false)

        // Initialize UI components
        imageButton = view.findViewById(R.id.addPhoto)
        saveButton = view.findViewById(R.id.saveButton)
        photoView = view.findViewById(R.id.photoView)
        photoChange = view.findViewById(R.id.changeButton)
        photoCard = view.findViewById(R.id.photoCardView)
        backButton = view.findViewById(R.id.backButton)
        dateText = view.findViewById(R.id.date)
        titleField = view.findViewById(R.id.addTitle)
        textField = view.findViewById(R.id.addText)
        addLocation = view.findViewById(R.id.addLocation)
        editLocation = view.findViewById(R.id.editLocationButton)

        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        dateText.text = currentDate

        backButton.setOnClickListener { (activity as MainActivity).replaceFragment(Home()) }

        // Load initial data from Firebase
        loadDiaryData()

        imageButton.setOnClickListener { showImageSourceDialog() }
        photoChange.setOnClickListener { showImageSourceDialog() }

        saveButton.setOnClickListener {
            if (titleField.text.isNotEmpty() && textField.text.isNotEmpty()) {
                updateDiary()
            } else {
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        editLocation.setOnClickListener {
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
            addLocation.text = country
            Log.d("AddDiary", "New location: $country")
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

    private fun loadDiaryData() {
        detailId?.let { id ->
            firestore.collection("Diary").whereEqualTo("detailId", id).get()
                .addOnSuccessListener { diarySnapshot ->
                    if (!diarySnapshot.isEmpty) {
                        val diaryDocument = diarySnapshot.documents[0]
                        val tagId = diaryDocument.getString("tagId") // Get tagId

                        firestore.collection("DiaryDetail").document(id).get()
                            .addOnSuccessListener { document ->
                                if (document != null && document.exists()) {
                                    initialTitle = document.getString("title")
                                    initialText = document.getString("text")
                                    initialPhotoUrl = document.getString("photo")
                                    val locationId = document.getString("locationId")

                                    titleField.setText(initialTitle)
                                    textField.setText(initialText)
                                    Glide.with(this).load(initialPhotoUrl).into(photoView)

                                    fetchLocationId(id)
                                    fetchSelectedTag(tagId)
                                    swapView()
                                }
                            }.addOnFailureListener { exception ->
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to load diary details: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "Diary not found", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Failed to fetch diary: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchSelectedTag(selectedTagId: String?) {
        firestore.collection("Tag").get().addOnSuccessListener { querySnapshot ->
            val tagList = mutableListOf<Tag>()
            for (document in querySnapshot.documents) {
                val tag = Tag(
                    id = document.id,
                    name = document.getString("name") ?: ""
                )
                tagList.add(tag)
            }

            tags.clear()
            tags.addAll(tagList)
            tagAdapter.notifyDataSetChanged()

            // Set the initially selected tag
            selectedTagId?.let { id ->
                initialTag = id
                tagAdapter.setInitialSelectedTag(id)
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), "Failed to fetch tags: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchLocationId(detailId: String) {
        firestore.collection("Diary")
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
        firestore.collection("Location").document(locationId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    latitude = document.getString("latitude").toString()
                    longitude = document.getString("longitude").toString()

                    if(newLat != null && newLng != null){
                        latitude = newLat
                        longitude = newLng
                    }

                    country = getCityAndCountryFromCoordinates(latitude!!.toDouble(), longitude!!.toDouble())
                    addLocation.text = country ?: "Country not found"
                } else {
                    Log.e("DetailDiary", "Location document does not exist for locationId: $locationId")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching location details: $exception", Toast.LENGTH_SHORT).show()
                Log.e("DetailDiary", "Error fetching location details", exception)
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

    private fun updateDiary() {
        val title = if (titleField.text.toString().isNotEmpty()) titleField.text.toString() else initialTitle
        val text = if (textField.text.toString().isNotEmpty()) textField.text.toString() else initialText
        val updatedTag = selectedTag?.id ?: initialTag
        val updateData = mutableMapOf<String, Any?>("title" to title, "text" to text)

        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(requireContext(), "Updating Diary...", Toast.LENGTH_SHORT).show()

            if (photoUri != null) {
                updateData["photo"] = withContext(Dispatchers.IO) { uploadImageToFirebase(photoUri!!) }
            } else {
                updateData["photo"] = initialPhotoUrl
            }

            detailId?.let { id ->
                firestore.collection("DiaryDetail").document(id)
                    .update(updateData)
                    .addOnSuccessListener {
                        updateTagDiary(id, updatedTag)
                        updateLocation(id, latitude, longitude)
                        redirectToHome()
                        Toast.makeText(requireContext(), "Diary updated successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to update diary: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun updateTagDiary(detailId: String, tagId: String) {
        // Update the tagId in the Diary collection
        firestore.collection("Diary")
            .whereEqualTo("detailId", detailId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val diaryDocument = querySnapshot.documents[0]
                val diaryId = diaryDocument.id

                firestore.collection("Diary").document(diaryId)
                    .update("tagId", tagId)
                    .addOnSuccessListener {
                        Log.d("Update", "tagId updated in Diary collection")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to update tagId: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to find Diary document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLocation(detailId: String, lat: String?, lng: String?) {
        if (lat.isNullOrEmpty() || lng.isNullOrEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Latitude and Longitude cannot be empty.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        firestore.collection("Diary").whereEqualTo("detailId", detailId).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val diaryDocument = querySnapshot.documents[0]
                    val locationId = diaryDocument.getString("locationId")

                    if (!locationId.isNullOrEmpty()) {
                        val locationData = mapOf("latitude" to latitude, "longitude" to longitude)
                        firestore.collection("Location").document(locationId).update(locationData)
                            .addOnSuccessListener {
                                if (isAdded) {
                                    Toast.makeText(requireContext(), "Location updated successfully!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                if(isAdded){
                                    Toast.makeText(requireContext(), "Failed to update location: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Location ID not found in Diary.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Diary with the given detailId not found.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to retrieve Diary: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private suspend fun uploadImageToFirebase(imageUri: Uri): String = withContext(Dispatchers.IO) {
        val fileName = "images/${System.currentTimeMillis()}.jpg"
        val fileReference = storageReference.child(fileName)
        fileReference.putFile(imageUri).await()
        fileReference.downloadUrl.await().toString()
    }

    private fun swapView() {
        imageButton.visibility = View.GONE
        photoChange.visibility = View.VISIBLE
        photoCard.visibility = View.VISIBLE
    }

    private fun redirectToHome() {
        (activity as MainActivity).replaceFragment(Home())
    }
}