package com.gndec.attendanceapp

import android.graphics.Bitmap
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.random.Random

class GenerateQrActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var sessionId: String
    private lateinit var ivQrCode: ImageView
    private lateinit var tvCountdown: TextView
    private var rotationTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_generate_qr)

        db = FirebaseFirestore.getInstance()

        val subject = intent.getStringExtra("subject") ?: "Session"
        sessionId = intent.getStringExtra("sessionId") ?: ""

        findViewById<TextView>(R.id.tvSubject).text = subject
        ivQrCode = findViewById(R.id.ivQrCode)
        tvCountdown = findViewById(R.id.tvCountdown)

        if (sessionId.isEmpty()) {
            tvCountdown.text = "Error: no session ID"
            return
        }

        rotateQrAndStartTimer()

        findViewById<Button>(R.id.btnEndSession).setOnClickListener {
            rotationTimer?.cancel()
            db.collection("sessions").document(sessionId)
                .update("isActive", false)
                .addOnCompleteListener {
                    finish()
                }
        }
    }

    private fun rotateQrAndStartTimer() {
        val newToken = generateToken()

        db.collection("sessions").document(sessionId)
            .update(
                "currentQrToken", newToken,
                "qrLastRotatedAt", Timestamp.now()
            )
            .addOnSuccessListener {
                val qrContent = "$sessionId|$newToken"
                showQrCode(qrContent)
                startCountdown()
            }
    }

    private fun showQrCode(content: String) {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 600, 600)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        ivQrCode.setImageBitmap(bitmap)
    }

    private fun startCountdown() {
        rotationTimer?.cancel()
        rotationTimer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                tvCountdown.text = "Refreshing in ${secondsLeft}s"
            }

            override fun onFinish() {
                rotateQrAndStartTimer()
            }
        }.start()
    }

    private fun generateToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..12).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    override fun onDestroy() {
        super.onDestroy()
        rotationTimer?.cancel()
    }
}