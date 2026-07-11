package com.gndec.attendanceapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etRollNumber = findViewById<EditText>(R.id.etRollNumber)
        val etName = findViewById<EditText>(R.id.etName)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val rollNumber = etRollNumber.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (rollNumber.isEmpty() || password.isEmpty()) {
                tvStatus.text = "Enter both roll number and password"
                return@setOnClickListener
            }

            val fakeEmail = "$rollNumber@gndec.app"

            auth.signInWithEmailAndPassword(fakeEmail, password)
                .addOnSuccessListener {
                    val intent = Intent(this, StudentDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    tvStatus.text = "Login failed: ${e.message}"
                }
        }

        findViewById<Button>(R.id.btnSignup).setOnClickListener {
            val rollNumber = etRollNumber.text.toString().trim()
            val name = etName.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (rollNumber.isEmpty() || name.isEmpty() || password.isEmpty()) {
                tvStatus.text = "Enter roll number, name, and password"
                return@setOnClickListener
            }
            if (password.length < 6) {
                tvStatus.text = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            val fakeEmail = "$rollNumber@gndec.app"

            auth.createUserWithEmailAndPassword(fakeEmail, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val studentData = hashMapOf(
                        "role" to "student",
                        "name" to name,
                        "rollNumber" to rollNumber,
                        "createdAt" to com.google.firebase.Timestamp.now()
                    )
                    db.collection("users").document(uid).set(studentData)
                        .addOnSuccessListener {
                            tvStatus.text = "Account created! You can now log in."
                        }
                }
                .addOnFailureListener { e ->
                    tvStatus.text = "Signup failed: ${e.message}"
                }
        }
    }
}