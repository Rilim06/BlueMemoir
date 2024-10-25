package com.example.bluememoir

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var diariesWrittenTextView: TextView
    private var profileImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        profileImageView = view.findViewById(R.id.profile_avatar)
        nameTextView = view.findViewById(R.id.profile_name)
        emailTextView = view.findViewById(R.id.profile_email)
        diariesWrittenTextView = view.findViewById(R.id.diaries_written)

        // Load user info from Firestore
        loadUserProfile()

        // Google Sign-In options
        val gso: GoogleSignInOptions? =
            @Suppress("DEPRECATION")
            arguments?.getParcelable("googleSignInOptions")

        if (gso != null) {
            googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        }

        // Handle logout button click
        val logoutButton = view.findViewById<ImageView>(R.id.logout_icon)
        logoutButton.setOnClickListener {
            signOut()
        }

        // Handle settings button click
        val settingsButton = view.findViewById<ImageView>(R.id.settings_icon)
        settingsButton.setOnClickListener {
            // Navigate to settings page
            startActivity(Intent(activity, SettingFragment::class.java))
        }

        // Handle profile picture click to show it in larger size
        profileImageView.setOnClickListener {
            showProfilePictureDialog(profileImageUrl)
        }

        return view
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = db.collection("users").document(userId)
            userRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    // Load profile image, name, email, and diaries count
                    val name = document.getString("name")
                    val email = document.getString("email")
                    profileImageUrl = document.getString("profileImage")
                    val diariesWritten = document.getLong("diariesWritten")?.toInt() ?: 0

                    nameTextView.text = name ?: "No Name"
                    emailTextView.text = email ?: "No Email"
                    diariesWrittenTextView.text = "Diaries Written: $diariesWritten"

                    // Load profile image using Glide (for image caching and loading from URL)
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.profile_picture) // Placeholder if image not found
                        .into(profileImageView)
                }
            }.addOnFailureListener { e ->
                Log.e("Profile", "Error loading profile: ", e)
            }
        }
    }

    private fun showProfilePictureDialog(profileImageUrl: String?) {
        if (profileImageUrl != null && profileImageUrl.isNotEmpty()) {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_profile_picture)

            val imageView = dialog.findViewById<ImageView>(R.id.dialog_profile_image)

            // Gunakan Glide untuk memuat gambar di dalam dialog
            Glide.with(this)
                .load(profileImageUrl)
                .placeholder(R.drawable.profile_picture) // Placeholder jika gambar tidak ada
                .into(imageView)

            dialog.show()
        } else {
            Log.e("Profile", "Profile image URL is empty or null")
        }
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
