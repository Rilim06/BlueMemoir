package com.example.bluememoir

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri

class Home : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    private lateinit var noDiary: TextView
    private var dataList = mutableListOf<MyModel>()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        noDiary = view.findViewById(R.id.noDiary)
        recyclerView = view.findViewById(R.id.homeView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MyAdapter(dataList)
        recyclerView.adapter = adapter

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            var documentsProcessed = 0

            // Fetch Diary collection filtered by the logged-in user
            db.collection("Diary")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result) {
                        val detailId = document.getString("detailId") ?: continue

                        // Fetch details from DiaryDetail collection
                        db.collection("DiaryDetail").document(detailId)
                            .get()
                            .addOnSuccessListener { detailDocument ->
                                val title = detailDocument.getString("title") ?: ""
                                val date = detailDocument.getString("date") ?: ""
                                val imagePath = detailDocument.getString("photo") ?: ""

                                // Add to dataList
                                val model = MyModel(title, date, imagePath)
                                dataList.add(model)

                                documentsProcessed++

                                // Notify adapter of data change
                                adapter.notifyDataSetChanged()

                                if (documentsProcessed == result.size()) {
                                    checkData() // Call checkData only after all documents are processed
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error getting documents: $exception", Toast.LENGTH_SHORT).show()
                }
        }

        val addButton = view.findViewById<ImageButton>(R.id.addButton)
        addButton.setOnClickListener {
            (activity as MainActivity).replaceFragment(AddDiary())
        }

        return view
    }

    private fun checkData() {
        if (dataList.isEmpty()) {
            recyclerView.visibility = View.GONE
            noDiary.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noDiary.visibility = View.GONE
        }
    }

}
