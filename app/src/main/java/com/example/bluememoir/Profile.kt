package com.example.bluememoir

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.bumptech.glide.Glide

class Profile : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var nameView: TextView
    private lateinit var accountView: TextView
    private lateinit var photoView: ImageView
    private lateinit var profileView: ImageView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private lateinit var auth: FirebaseAuth
    private var previousPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageReference = storage.reference
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        nameView = view.findViewById(R.id.profile_name)
        accountView = view.findViewById(R.id.profile_email)
        profileView = view.findViewById(R.id.profileView)
        photoView = view.findViewById(R.id.photoView)

        val gso: GoogleSignInOptions? =
            @Suppress("DEPRECATION")
            arguments?.getParcelable("googleSignInOptions")

        if (gso != null) {
            googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        }

        val logoutButton = view.findViewById<ImageButton>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            signOut()
        }

        // Handle settings button click
        val settingsButton = view.findViewById<ImageButton>(R.id.settings_icon)
        settingsButton.setOnClickListener {
            // Navigate to settings page
            (activity as MainActivity).replaceFragment(Setting())
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val profileRef = FirebaseFirestore.getInstance().collection("Profile")

        profileRef.whereEqualTo("userId", currentUserId).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val name = document.getString("name") ?: "Unknown"
                    previousPhotoPath = document.getString("photo")

                    nameView.text = name

                    if(previousPhotoPath != null){
                        swapView()
                        Glide.with(this).load(previousPhotoPath).into(photoView)
                    }
                } else {
                    // Handle case when no matching document is found
                    nameView.text = "Name not found"
                    accountView.text = "NIM not found"
                }
            }
            .addOnFailureListener { exception ->
                // Handle error
                Log.e("ProfileFragment", "Error fetching profile data", exception)
            }

        return view
    }

    private fun swapView() {
        profileView.visibility = View.GONE
        photoView.visibility = View.VISIBLE
    }

    private fun signOut() {
        auth.signOut()

        googleSignInClient.signOut().addOnCompleteListener {
            if (it.isSuccessful) {
                redirectToLogin()
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(activity, LoginActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}
