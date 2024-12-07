package com.example.bluememoir

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import android.os.Handler

class AllDiary : Fragment() {

    private lateinit var sliderViewPager: ViewPager2
    private lateinit var sliderAdapter: SliderAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: MyAdapter
    private lateinit var noDiary: TextView
    private lateinit var allDiary: LinearLayout

    private var sliderDataList = mutableListOf<MyModel>()
    private var recyclerDataList = mutableListOf<MyModel>()
    private lateinit var db: FirebaseFirestore
    private lateinit var handler: android.os.Handler
    private lateinit var runnable: Runnable
    private var currentPage = 0

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_diary, container, false)

        sliderViewPager = view.findViewById(R.id.sliderViewPager)
        recyclerView = view.findViewById(R.id.allView)
        noDiary = view.findViewById(R.id.noDiary)
        allDiary = view.findViewById(R.id.belowSearchLayout)

        sliderAdapter = SliderAdapter(sliderDataList) { clickedDetailId ->
            navigateToDetailDiary(clickedDetailId)
        }
        sliderViewPager.adapter = sliderAdapter

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerAdapter = MyAdapter(recyclerDataList) { detailId ->
            navigateToDetailDiary(detailId)
        }
        recyclerView.adapter = recyclerAdapter

        db = FirebaseFirestore.getInstance()
        handler = android.os.Handler(Looper.getMainLooper())

        fetchDiaryEntries()

        return view
    }

    private fun fetchDiaryEntries() {

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser
        val userId = currentUser?.uid

        var documentsProcessed = 0

        db.collection("Diary")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val tempDataList = mutableListOf<MyModel>()

                for (document in result) {
                    if (result.isEmpty) {
                        noDiary.visibility = View.VISIBLE
                        view?.findViewById<LinearLayout>(R.id.belowSearchLayout)?.visibility = View.GONE
                    } else {
                        noDiary.visibility = View.GONE
                        view?.findViewById<LinearLayout>(R.id.belowSearchLayout)?.visibility = View.VISIBLE
                        val detailId = document.getString("detailId") ?: continue

                        db.collection("DiaryDetail").document(detailId)
                            .get()
                            .addOnSuccessListener { detailDocument ->
                                val title = detailDocument.getString("title") ?: ""
                                val date = detailDocument.getString("date") ?: ""
                                val imagePath = detailDocument.getString("photo") ?: ""
                                val id = detailDocument.id
                                tempDataList.add(MyModel(id, title, date, imagePath))

                                documentsProcessed++

                                if (documentsProcessed == result.size()) {
                                    tempDataList.sortByDescending { it.date }
                                    // Divide the data into sliderDataList and recyclerDataList
                                    sliderDataList.clear()
                                    recyclerDataList.clear()

                                    sliderDataList.addAll(tempDataList.take(3)) // Take the first 3 for the slider
                                    recyclerDataList.addAll(tempDataList)

                                    sliderAdapter.notifyDataSetChanged()
                                    recyclerAdapter.notifyDataSetChanged()

                                    checkData()

                                    // Start auto-scroll for the slider
                                    startAutoScroll()
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching data: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkData() {
        if (recyclerDataList.isEmpty()) {
            allDiary.visibility = View.GONE
            noDiary.visibility = View.VISIBLE
        } else {
            allDiary.visibility = View.VISIBLE
            noDiary.visibility = View.GONE
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


    private fun startAutoScroll() {
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (sliderDataList.isNotEmpty()) {
                    currentPage = (currentPage + 1) % sliderDataList.size
                    sliderViewPager.setCurrentItem(currentPage, true)
                }
                handler.postDelayed(this, 2000) // Schedule the next slide
            }
        }
        handler.postDelayed(runnable, 2000) // Start after 1 second
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop auto-scrolling when the view is destroyed
        if (::handler.isInitialized && ::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }
}