package com.example.bluememoir

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val togglePasswordVisibility = findViewById<ImageView>(R.id.togglePasswordVisibility)
        val passwordConfirmEditText = findViewById<EditText>(R.id.passwordConfirmEditText)
        val toggleConfirmPasswordVisibility = findViewById<ImageView>(R.id.toggleConfirmPasswordVisibility)
        val toSignUp = findViewById<TextView>(R.id.to_sign_in)

        val registerButton = findViewById<Button>(R.id.registerButton)

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val repassword = passwordConfirmEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && repassword.isNotEmpty()) {
                if(password == repassword){
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                                val editor = preferences.edit()
                                editor.putBoolean("registered", true)
                                editor.apply()

                                val userId = auth.currentUser?.uid
                                val name = "User"

                                addProfile(userId!!, email, name)

                                val intent = Intent(this, LoginActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                val exception = task.exception
                                if (exception !is FirebaseAuthWeakPasswordException) {
                                    if (exception is FirebaseAuthInvalidCredentialsException) {
                                        Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
                                    } else if (exception is FirebaseAuthUserCollisionException) {
                                        Toast.makeText(this, "Email is already registered", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                }else{
                    Toast.makeText(this, "Re-entered Password incorrect", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Email, Password, and Re-enter Password cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        var isPasswordVisible = true
        var isConfirmPasswordVisible = true

        togglePasswordVisibility.setOnClickListener {
            if (isPasswordVisible) {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                togglePasswordVisibility.setImageResource(R.drawable.eye_off)
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibility.setImageResource(R.drawable.eye)
            }

            passwordEditText.setSelection(passwordEditText.text.length)
            isPasswordVisible = !isPasswordVisible
        }

        toggleConfirmPasswordVisibility.setOnClickListener {
            if (isConfirmPasswordVisible) {
                passwordConfirmEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                toggleConfirmPasswordVisibility.setImageResource(R.drawable.eye_off)
            } else {
                passwordConfirmEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                toggleConfirmPasswordVisibility.setImageResource(R.drawable.eye)
            }

            passwordConfirmEditText.setSelection(passwordConfirmEditText.text.length)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        toSignUp.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addProfile(userId: String, account: String, name: String) {
        val db = FirebaseFirestore.getInstance()

        val profileData = hashMapOf(
            "userId" to userId,
            "account" to account,
            "name" to name,
            "isNotif" to true
        )

        db.collection("Profile")
            .add(profileData)
    }
}