package com.gndec.attendanceapp

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TeacherDashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val currentUser = auth.currentUser
        val emailPrefix = currentUser?.email?.substringBefore("@") ?: "Teacher"
        tvWelcome.text = "Welcome, $emailPrefix"

        findViewById<Button>(R.id.btnStartSession).setOnClickListener {
            startActivity(Intent(this, CreateSessionActivity::class.java))
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
        android.util.Log.d("DASHBOARD_DEBUG", "onResume called")
        loadSessions()
    }

    private fun loadSessions() {
        val currentUser = auth.currentUser
        android.util.Log.d("DASHBOARD_DEBUG", "loadSessions called, currentUser = $currentUser")
        if (currentUser == null) {
            android.util.Log.d("DASHBOARD_DEBUG", "currentUser is null, returning early")
            return
        }
        val teacherId = currentUser.uid
        android.util.Log.d("DASHBOARD_DEBUG", "teacherId = $teacherId")

        db.collection("sessions")
            .whereEqualTo("teacherId", teacherId)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                android.util.Log.d("DASHBOARD_DEBUG", "Query succeeded, doc count = ${querySnapshot.size()}")
                val container = findViewById<LinearLayout>(R.id.sessionsContainer)
                container.removeAllViews()

                if (querySnapshot.isEmpty) {
                    val emptyText = TextView(this)
                    emptyText.text = "No sessions yet."
                    emptyText.setTextColor(android.graphics.Color.parseColor("#757575"))
                    container.addView(emptyText)
                    return@addOnSuccessListener
                }

                for (doc in querySnapshot.documents) {
                    val subject = doc.getString("subject") ?: "Unknown"
                    val isActive = doc.getBoolean("isActive") ?: false
                    val statusText = if (isActive) "Active" else "Ended"
                    val statusColor = if (isActive) "#2E7D32" else "#757575"

                    val row = LinearLayout(this)
                    row.orientation = LinearLayout.HORIZONTAL
                    row.gravity = Gravity.CENTER_VERTICAL
                    val rowParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    rowParams.bottomMargin = 12
                    row.layoutParams = rowParams

                    val subjectView = TextView(this)
                    subjectView.text = subject
                    subjectView.textSize = 16f
                    val subjectParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    subjectView.layoutParams = subjectParams

                    val statusView = TextView(this)
                    statusView.text = statusText
                    statusView.setTextColor(android.graphics.Color.parseColor(statusColor))
                    statusView.textSize = 14f

                    row.addView(subjectView)
                    row.addView(statusView)
                    container.addView(row)
                }
            }
            .addOnFailureListener {
                android.util.Log.e("DASHBOARD_DEBUG", "Query FAILED: ${it.message}", it)
                val container = findViewById<LinearLayout>(R.id.sessionsContainer)
                container.removeAllViews()
                val errorText = TextView(this)
                errorText.text = "Failed to load sessions: ${it.message}"
                errorText.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
                container.addView(errorText)
            }
    }
}