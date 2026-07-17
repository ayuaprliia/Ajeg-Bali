package com.example.ajegbali

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.ajegbali.feature.chatbot.ChatbotActivity
import com.example.ajegbali.feature.home.HomeFragment
import com.example.ajegbali.feature.jejahitan.JejahitanFragment
import com.example.ajegbali.feature.ketupat.KetupatFragment
import com.example.ajegbali.feature.wayang.WayangFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // --- INI KUNCI RAHASIANYA ---
        // Mematikan filter warna bawaan Android agar ikon tampil dengan warna aslinya
        bottomNav.itemIconTintList = null
        // ----------------------------

        // 1. Ubah halaman pertama yang dibuka menjadi HomeFragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            // Set agar ikon Home di navbar ikut menyala
            bottomNav.selectedItemId = R.id.menu_home
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.menu_jejahitan -> {
                    replaceFragment(JejahitanFragment())
                    true
                }
                R.id.menu_ketupat -> {
                    replaceFragment(KetupatFragment())
                    true
                }
                R.id.menu_wayang -> {
                    replaceFragment(WayangFragment())
                    true
                }
                else -> false
            }
        }

        // --- FAB CHATBOT ---
        val fabChatbot = findViewById<FloatingActionButton>(R.id.fabChatbot)
        fabChatbot.setOnClickListener {
            val intent = Intent(this, ChatbotActivity::class.java)
            startActivity(intent)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}