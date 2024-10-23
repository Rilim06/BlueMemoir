package com.example.bluememoir

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.bluememoir.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private var currentFragmentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (savedInstanceState == null) {
            replaceFragment(Home())
        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.home -> replaceFragment(Home())
                R.id.favorites -> replaceFragment(Favorites())
                R.id.mapview -> replaceFragment(MapView())
                R.id.profile -> {
                    val profileFragment = Profile()

                    val bundle = Bundle()
                    bundle.putParcelable("googleSignInOptions", gso)
                    profileFragment.arguments = bundle

                    replaceFragment(profileFragment)
                }
                else -> {}
            }
            true
        }
    }

    internal fun replaceFragment(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val nextFragmentTag = fragment::class.java.simpleName
        val animEnter: Int
        val animExit: Int

        if (nextFragmentTag == currentFragmentTag) {
            animEnter = 0
            animExit = 0
        } else {
            when (currentFragmentTag) {
                "Home" -> {
                    animEnter = R.anim.slide_in_right
                    animExit = R.anim.slide_out_left
                }
                "Favorites" -> {
                    animEnter = if (nextFragmentTag == "MapView" || nextFragmentTag == "Profile") {
                        R.anim.slide_in_right
                    } else {
                        R.anim.slide_in_left
                    }
                    animExit = if (nextFragmentTag == "MapView" || nextFragmentTag == "Profile") {
                        R.anim.slide_out_left
                    } else {
                        R.anim.slide_out_right
                    }
                }
                "MapView" -> {
                    animEnter = if (nextFragmentTag == "Profile") {
                        R.anim.slide_in_right
                    } else {
                        R.anim.slide_in_left
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

        if (animEnter != 0 && animExit != 0) {
            fragmentTransaction.setCustomAnimations(animEnter, animExit)
        }

        fragmentTransaction.replace(R.id.frame_layout, fragment, nextFragmentTag)
        fragmentTransaction.commit()

        currentFragmentTag = nextFragmentTag
    }
}
