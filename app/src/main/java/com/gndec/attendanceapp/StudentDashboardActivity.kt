package com.gndec.attendanceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StudentDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvRollNumber = findViewById<TextView>(R.id.tvRollNumber)

        val currentUser = auth.currentUser
        val fakeEmail = currentUser?.email ?: ""
        val rollNumber = fakeEmail.substringBefore("@")

        tvWelcome.text = "Welcome, Student"
        tvRollNumber.text = "Roll Number: $rollNumber"

        findViewById<Button>(R.id.btnScanQr).setOnClickListener {
            startActivity(Intent(this, StudentScanActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        loadAttendance()
    }

    private fun loadAttendance() {
        val currentUser = auth.currentUser ?: return
        val studentUid = currentUser.uid

        db.collection("attendance")
            .whereEqualTo("studentUid", studentUid)
            .orderBy("scannedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val container = findViewById<LinearLayout>(R.id.attendanceContainer)
                container.removeAllViews()

                val totalCount = querySnapshot.size()

                val summaryText = TextView(this)
                summaryText.text = "Total classes attended: $totalCount"
                summaryText.textSize = 16f
                summaryText.setTypeface(null, android.graphics.Typeface.BOLD)
                summaryText.setPadding(0, 0, 0, 16)
                container.addView(summaryText)

                if (querySnapshot.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No attendance records yet."
                    emptyText.setTextColor(android.graphics.Color.parseColor("#757575"))
                    container.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in querySnapshot.documents) {
                    val subject = doc.getString("subject") ?: "Unknown"
                    val timestamp = doc.getTimestamp("scannedAt")
                    val dateStr = timestamp?.toDate()?.let {
                        java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(it)
                    } ?: "Unknown date"

                    val row = TextView(this)
                    row.text = "$subject — $dateStr"
                    row.textSize = 14f
                    row.setPadding(0, 4, 0, 4)
                    container.addView(row)
                }
            }
            .addOnFailureListener {
                val container = findViewById<LinearLayout>(R.id.attendanceContainer)
                container.removeAllViews()
                val errorText = TextView(this)
                errorText.text = "Failed to load attendance: ${it.message}"
                errorText.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                container.addView(errorText)
            }
    }
}