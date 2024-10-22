package com.example.bluememoir

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.bluememoir.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        replaceFragment(Home())

        binding.bottomNavigationView.setOnItemSelectedListener {

            when (it.itemId) {
                R.id.home -> replaceFragment(Home())
                R.id.favorites -> replaceFragment(Favorites())
                R.id.mapview -> replaceFragment(MapView())
                R.id.profile -> replaceFragment(Profile())
                else -> {}
            }
            true
        }
    }

    private var currentFragmentTag: String? = null

    private fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val nextFragmentTag = fragment::class.java.simpleName
        val animEnter: Int
        val animExit: Int

        // Check if the user is clicking the currently displayed fragment
        if (nextFragmentTag == currentFragmentTag) {
            // No animation
            animEnter = 0
            animExit = 0
        } else {
            // Determine the direction based on the current and next fragments
            when (currentFragmentTag) {
                "Home" -> {
                    animEnter = R.anim.slide_in_right // Home to Favorites or others (right)
                    animExit = R.anim.slide_out_left
                }
                "Favorites" -> {
                    animEnter = if (nextFragmentTag == "MapView" || nextFragmentTag == "Profile") {
                        R.anim.slide_in_right // Favorites to MapView/Profile (right)
                    } else {
                        R.anim.slide_in_left // Favorites to Home (left)
                    }
                    animExit = if (nextFragmentTag == "MapView" || nextFragmentTag == "Profile") {
                        R.anim.slide_out_left
                    } else {
                        R.anim.slide_out_right
                    }
                }
                "MapView" -> {
                    animEnter = if (nextFragmentTag == "Profile") {
                        R.anim.slide_in_right // MapView to Profile (right)
                    } else {
                        R.anim.slide_in_left // MapView to Favorites (left)
                    }
                    animExit = if (nextFragmentTag == "Profile") {
                        R.anim.slide_out_left
                    } else {
                        R.anim.slide_out_right
                    }
                }
                "Profile" -> {
                    animEnter = R.anim.slide_in_left
                    animExit = R.anim.slide_out_right
                }
                else -> {
                    animEnter = R.anim.slide_in_right
                    animExit = R.anim.slide_out_left
                }
            }
        }

        // Set the custom animations
        if (animEnter != 0 && animExit != 0) {
            fragmentTransaction.setCustomAnimations(animEnter, animExit)
        }

        // Replace the fragment
        fragmentTransaction.replace(R.id.frame_layout, fragment, nextFragmentTag)

        // Commit the transaction
        fragmentTransaction.commit()

        // Update the current fragment tag
        currentFragmentTag = nextFragmentTag
    }
}