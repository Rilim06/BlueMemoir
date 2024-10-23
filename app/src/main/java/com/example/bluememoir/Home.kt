package com.example.bluememoir

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController

class Home : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val currentUser = auth.currentUser
        val userEmail = currentUser?.email
        val userId = currentUser?.uid

        val addButton = view.findViewById<Button>(R.id.addButton)
        addButton.setOnClickListener {
            (activity as MainActivity).replaceFragment(AddDiary())
        }

        return view
    }
}
