package com.gndec.attendanceapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedRole = prefs.getString("user_role", null)

        if (savedRole == null) {
            // First time ever opening the app — let them choose
            startActivity(Intent(this, ModeSelectActivity::class.java))
        } else {
            // TODO: once teacher/student home screens exist, route straight there
            // based on savedRole ("teacher" or "student")
            startActivity(Intent(this, ModeSelectActivity::class.java))
        }
        finish() // close MainActivity so back-button doesn't return to a blank screen
    }
}