package com.example.bluememoir

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Favorites : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    private lateinit var noFavorite: TextView
    private var dataList = mutableListOf<MyModel>()
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favorites, container, false)

        noFavorite = view.findViewById(R.id.noFavorite)
        recyclerView = view.findViewById(R.id.favoriteView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MyAdapter(dataList) { detailId ->
            navigateToDetailDiary(detailId)
        }
        recyclerView.adapter = adapter

        fetchFavorites()
        return view
    }

    private fun fetchFavorites() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            db.collection("Diary")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        checkData()
                        return@addOnSuccessListener
                    }

                    val favoriteTasks = mutableListOf<Task<DocumentSnapshot>>()
                    for (document in result) {
                        val detailId = document.getString("detailId") ?: continue
                        favoriteTasks.add(db.collection("DiaryDetail").document(detailId).get())
                    }

                    Tasks.whenAllComplete(favoriteTasks).addOnCompleteListener {
                        dataList.clear()
                        for (task in favoriteTasks) {
                            val detailDocument = task.result as? DocumentSnapshot ?: continue
                            val isFavorite = detailDocument.getBoolean("isFavorite") ?: false
                            if (isFavorite) {
                                val title = detailDocument.getString("title") ?: ""
                                val date = detailDocument.getString("date") ?: ""
                                val imagePath = detailDocument.getString("photo") ?: ""

                                val model = MyModel(detailDocument.id, title, date, imagePath)
                                dataList.add(model)
                            }
                        }
                        dataList.sortByDescending { it.date }
                        adapter.notifyDataSetChanged()
                        checkData()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error getting documents: $exception", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun navigateToDetailDiary(detailId: String) {
        val detailFragment = DetailDiary()
        val bundle = Bundle().apply {
            putString("detailId", detailId)
        }
        detailFragment.arguments = bundle
        (activity as MainActivity).replaceFragment(detailFragment)
    }

    private fun checkData() {
        if (dataList.isEmpty()) {
            recyclerView.visibility = View.GONE
            noFavorite.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noFavorite.visibility = View.GONE
        }
    }
}
