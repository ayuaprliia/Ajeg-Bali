package com.example.ajegbali // Pastikan ini sesuai nama package Anda

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Kita tidak perlu supportActionBar?.hide() lagi
        // karena sudah diatur di themes.xml tadi.

        // LOGIKA PINDAH HALAMAN (DELAY 3 DETIK)
        Handler(Looper.getMainLooper()).postDelayed({
            // 1. Buat niat (Intent) mau pindah ke MainActivity
            val intent = Intent(this, MainActivity::class.java)

            // 2. Jalankan niat itu
            startActivity(intent)

            // 3. Hancurkan Splash Screen agar kalau di-Back tidak balik ke sini
            finish()
        }, 3000) // 3000 ms = 3 detik
    }
}