package com.example.bluememoir

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import android.util.Log
import android.text.Editable
import android.text.TextWatcher

class Home : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MyAdapter
    private lateinit var noDiary: TextView
    private lateinit var searchInput: EditText
    private var dataList = mutableListOf<MyModel>()
    private var filteredList = mutableListOf<MyModel>()
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

        adapter = MyAdapter(dataList) { detailId ->
            navigateToDetailDiary(detailId)
        }
        recyclerView.adapter = adapter

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            retrieveData()
        }

        searchInput = view.findViewById(R.id.searchInput)
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterData(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val addButton = view.findViewById<ImageButton>(R.id.addButton)
        addButton.setOnClickListener {
            (activity as MainActivity).replaceFragment(AddDiary())
        }

        val allButton = view.findViewById<ImageButton>(R.id.seeAllButton)
        allButton.setOnClickListener {
            (activity as MainActivity).replaceFragment(AllDiary())
        }

        val recentButton = view.findViewById<ImageButton>(R.id.recentButton)
        val oldestButton = view.findViewById<ImageButton>(R.id.oldestButton)

        recentButton.setOnClickListener {
            recentData()
            recentButton.visibility = View.GONE
            oldestButton.visibility = View.VISIBLE
        }

        oldestButton.setOnClickListener {
            oldestData()
            recentButton.visibility = View.VISIBLE
            oldestButton.visibility = View.GONE
        }

        return view
    }

    private fun retrieveData(){
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        val userId = currentUser?.uid
        var documentsProcessed = 0
        val tempDataList = mutableListOf<MyModel>()

        // Fetch Diary collection filtered by the logged-in user
        db.collection("Diary")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
                            val id = detailDocument.id
                            Log.d("DetailDiary", "Retrieved detailId: $detailId")

                            // Add to dataList
                            val model = MyModel(id, title, date, imagePath)
                            tempDataList.add(model)

                            documentsProcessed++

                            if (documentsProcessed == result.size()) {
                                tempDataList.sortByDescending { it.date }

                                // Clear the original data list and add sorted data
                                dataList.clear()
                                dataList.addAll(tempDataList)

                                // Notify adapter of data change once
                                adapter.notifyDataSetChanged()

                                // Call checkData after all data has been processed and added
                                checkData()
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error getting documents: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterData(query: String) {
        //filteredList.clear()
        //filteredList.addAll(dataList)

        val filteredResults = if (query.isEmpty()) {
            dataList
        } else {
            dataList.filter { it.title.contains(query, ignoreCase = true) }
        }

        Log.d("FilterData", "dataList size: ${dataList.size}")
        //Log.d("FilterData", "filteredList size: ${filteredList.size}")
        Log.d("FilterData", "filteredResults size: ${filteredResults.size}")

        adapter.updateData(filteredResults)
        //retrieveData()
    }


    private fun recentData() {
        // Sort dataList by date in descending order (recent first)
        dataList.sortByDescending { it.date }
        adapter.notifyDataSetChanged()
    }

    private fun oldestData() {
        // Sort dataList by date in ascending order (oldest first)
        dataList.sortBy { it.date }
        adapter.notifyDataSetChanged()
    }

    private fun navigateToDetailDiary(detailId: String) {
        // Create the DetailDiary fragment instance
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
            noDiary.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noDiary.visibility = View.GONE
        }
    }

}
