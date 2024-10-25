package com.example.bluememoir

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingFragment : Fragment() {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var editPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var buttonNotification: Button // Changed from EditText to Button
    private lateinit var saveButton: ImageView

    private var isNotificationAllowed: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Initialize views
        editName = view.findViewById(R.id.edit_name)
        editEmail = view.findViewById(R.id.edit_email)
        editPassword = view.findViewById(R.id.edit_password)
        editNewPassword = view.findViewById(R.id.edit_new_password)
        buttonNotification = view.findViewById(R.id.button_notification) // Updated to Button
        saveButton = view.findViewById(R.id.save_button)

        // Load user data from Firestore
        loadUserData()

        // Set click listener for save button
        saveButton.setOnClickListener {
            saveUserData()
        }

        // Set click listener for notification button
        buttonNotification.setOnClickListener {
            showNotificationDialog()
        }

        return view
    }

    private fun loadUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)
            userRef.get().addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name")
                    val email = document.getString("email")
                    isNotificationAllowed = document.getBoolean("isNotif") ?: true

                    // Set Text to EditTexts
                    editName.setText(name)
                    editEmail.setText(email)

                    // Set notification button text
                    buttonNotification.text = if (isNotificationAllowed) "Allow" else "Disable"
                }
            }.addOnFailureListener { e ->
                Log.e("SettingFragment", "Error loading user data", e)
            }
        }
    }

    private fun saveUserData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val newName = editName.text.toString().trim()
        val newEmail = editEmail.text.toString().trim()
        val newPassword = editNewPassword.text.toString().trim()

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)

            // Update user data in Firestore
            val updates = hashMapOf<String, Any>(
                "name" to newName,
                "email" to newEmail,
                "isNotif" to isNotificationAllowed
            )

            userRef.update(updates).addOnSuccessListener {
                // Update email in FirebaseAuth
                val user = FirebaseAuth.getInstance().currentUser
                user?.updateEmail(newEmail)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Email updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to update email", Toast.LENGTH_SHORT).show()
                    }
                }

                // Update password in FirebaseAuth
                if (newPassword.isNotEmpty()) {
                    user?.updatePassword(newPassword)?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Password updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to update password", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to display the notification dialog
    private fun showNotificationDialog() {
        val options = arrayOf("Allow", "Disable")

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Notification Setting")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        isNotificationAllowed = true
                        buttonNotification.text = "Allow"
                    }
                    1 -> {
                        isNotificationAllowed = false
                        buttonNotification.text = "Disable"
                    }
                }
            }
        builder.show()
    }
}
