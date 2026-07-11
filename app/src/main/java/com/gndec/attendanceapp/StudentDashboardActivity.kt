package com.gndec.attendanceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        auth = FirebaseAuth.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvRollNumber = findViewById<TextView>(R.id.tvRollNumber)

        val currentUser = auth.currentUser
        val fakeEmail = currentUser?.email ?: ""
        val rollNumber = fakeEmail.substringBefore("@")

        tvWelcome.text = "Welcome, Student"
        tvRollNumber.text = "Roll Number: $rollNumber"

        findViewById<Button>(R.id.btnScanQr).setOnClickListener {
            // TODO: navigate to StudentScanActivity once QR scanning is built
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            prefs.edit().remove("user_role").apply()
            val intent = Intent(this, ModeSelectActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}