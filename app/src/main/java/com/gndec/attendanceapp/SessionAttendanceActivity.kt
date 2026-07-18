package com.gndec.attendanceapp

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SessionAttendanceActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_attendance)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val sessionId = intent.getStringExtra("sessionId") ?: ""
        val subject = intent.getStringExtra("subject") ?: "Session"

        findViewById<TextView>(R.id.tvSubject).text = subject

        if (sessionId.isEmpty()) {
            findViewById<TextView>(R.id.tvCount).text = "Error: no session ID"
            return
        }

        loadAttendance(sessionId)
    }

    private fun loadAttendance(sessionId: String) {
        val teacherId = auth.currentUser?.uid
        if (teacherId == null) {
            findViewById<TextView>(R.id.tvCount).text = "Not logged in"
            return
        }

        db.collection("attendance")
            .whereEqualTo("sessionId", sessionId)
            .whereEqualTo("teacherId", teacherId)
            .orderBy("scannedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val count = querySnapshot.size()
                findViewById<TextView>(R.id.tvCount).text = "$count student(s) marked"

                val container = findViewById<LinearLayout>(R.id.attendanceListContainer)
                container.removeAllViews()

                if (querySnapshot.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No students have marked attendance yet."
                    emptyText.setTextColor(android.graphics.Color.parseColor("#757575"))
                    container.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in querySnapshot.documents) {
                    val name = doc.getString("studentName") ?: "Unknown"
                    val rollNumber = doc.getString("studentRollNumber") ?: "Unknown"
                    val timestamp = doc.getTimestamp("scannedAt")
                    val timeStr = timestamp?.toDate()?.let {
                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(it)
                    } ?: ""

                    val row = TextView(this)
                    row.text = "$name ($rollNumber) — $timeStr"
                    row.textSize = 15f
                    row.setPadding(0, 8, 0, 8)
                    container.addView(row)
                }
            }
            .addOnFailureListener {
                findViewById<TextView>(R.id.tvCount).text = "Failed to load: ${it.message}"
            }
    }
}