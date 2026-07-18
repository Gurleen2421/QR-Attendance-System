package com.gndec.attendanceapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class StudentScanActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents == null) {
            Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            handleScannedContent(result.contents)
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startScan()
        } else {
            Toast.makeText(this, "Camera permission is required to scan", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startScan()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startScan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Scan the session QR code")
        options.setBeepEnabled(true)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun handleScannedContent(content: String) {
        val parts = content.split("|")
        if (parts.size != 2) {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val sessionId = parts[0]
        val scannedToken = parts[1]

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val studentUid = currentUser.uid
        val rollNumber = currentUser.email?.substringBefore("@") ?: "unknown"

        db.collection("users").document(studentUid).get()
            .addOnSuccessListener { userDoc ->
                val realName = userDoc.getString("name") ?: rollNumber

                db.collection("sessions").document(sessionId).get()
                    .addOnSuccessListener { sessionDoc ->
                        if (!sessionDoc.exists()) {
                            Toast.makeText(this, "Session not found", Toast.LENGTH_LONG).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        val isActive = sessionDoc.getBoolean("isActive") ?: false
                        val currentToken = sessionDoc.getString("currentQrToken")
                        val subject = sessionDoc.getString("subject") ?: "Unknown"
                        val teacherId = sessionDoc.getString("teacherId") ?: ""

                        if (!isActive) {
                            Toast.makeText(this, "This session has ended", Toast.LENGTH_LONG).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        if (currentToken != scannedToken) {
                            Toast.makeText(this, "QR code expired. Please scan the live QR.", Toast.LENGTH_LONG).show()
                            finish()
                            return@addOnSuccessListener
                        }

                        checkDuplicateAndMarkAttendance(sessionId, subject, teacherId, studentUid, rollNumber, realName, scannedToken)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error checking session: ${it.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching user info: ${it.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun checkDuplicateAndMarkAttendance(
        sessionId: String,
        subject: String,
        teacherId: String,
        studentUid: String,
        rollNumber: String,
        studentName: String,
        token: String
    ) {
        db.collection("attendance")
            .whereEqualTo("sessionId", sessionId)
            .whereEqualTo("studentUid", studentUid)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    Toast.makeText(this, "Attendance already marked for this session", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val attendanceData = hashMapOf(
                    "sessionId" to sessionId,
                    "subject" to subject,
                    "teacherId" to teacherId,
                    "studentUid" to studentUid,
                    "studentRollNumber" to rollNumber,
                    "studentName" to studentName,
                    "scannedAt" to Timestamp.now(),
                    "qrTokenUsed" to token
                )

                db.collection("attendance").add(attendanceData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Attendance marked successfully!", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to mark attendance: ${e.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error checking duplicate: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }
}