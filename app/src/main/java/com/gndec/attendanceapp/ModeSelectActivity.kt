package com.gndec.attendanceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ModeSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mode_select)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)

        findViewById<Button>(R.id.btnTeacher).setOnClickListener {
            prefs.edit().putString("user_role", "teacher").apply()
            startActivity(Intent(this, TeacherLoginActivity::class.java))
        }

        findViewById<Button>(R.id.btnStudent).setOnClickListener {
            prefs.edit().putString("user_role", "student").apply()
            startActivity(Intent(this, StudentLoginActivity::class.java))
        }
    }
}