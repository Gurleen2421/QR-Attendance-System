package com.gndec.attendanceapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class CreateSessionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_session)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etSubject = findViewById<EditText>(R.id.etSubject)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            val subject = etSubject.text.toString().trim()

            if (subject.isEmpty()) {
                tvStatus.text = "Enter a subject name"
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                tvStatus.text = "Not logged in. Please log in again."
                return@setOnClickListener
            }

            val teacherId = currentUser.uid
            val teacherName = currentUser.email?.substringBefore("@") ?: "Teacher"
            val initialToken = generateToken()

            val sessionData = hashMapOf(
                "subject" to subject,
                "teacherId" to teacherId,
                "teacherName" to teacherName,
                "startedAt" to Timestamp.now(),
                "currentQrToken" to initialToken,
                "qrLastRotatedAt" to Timestamp.now(),
                "isActive" to true
            )
            db.collection("sessions").add(sessionData)
                .addOnSuccessListener { docRef ->
                    val intent = android.content.Intent(this, GenerateQrActivity::class.java)
                    intent.putExtra("sessionId", docRef.id)
                    intent.putExtra("subject", subject)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    tvStatus.text = "Failed to start session: ${e.message}"
                }
        }
    }

    private fun generateToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }
}