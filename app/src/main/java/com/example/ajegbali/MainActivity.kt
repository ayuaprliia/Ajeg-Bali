package com.example.ajegbali

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

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
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}