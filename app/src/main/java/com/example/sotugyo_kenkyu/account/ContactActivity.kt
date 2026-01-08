package com.example.sotugyo_kenkyu.account

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.sotugyo_kenkyu.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class ContactActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextSubject: EditText
    private lateinit var editTextContent: EditText
    private lateinit var buttonSend: Button
    private lateinit var loadingOverlay: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        editTextSubject = findViewById(R.id.editTextSubject)
        editTextContent = findViewById(R.id.editTextContent)
        buttonSend = findViewById(R.id.buttonSend)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        val header = findViewById<View>(R.id.header)
        val backButton = findViewById<ImageButton>(R.id.buttonBack)

        // ステータスバー調整
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        backButton.setOnClickListener { finish() }

        buttonSend.setOnClickListener {
            sendInquiry()
        }
    }

    private fun sendInquiry() {
        val subject = editTextSubject.text.toString().trim()
        val content = editTextContent.text.toString().trim()
        val user = auth.currentUser

        if (subject.isEmpty()) {
            editTextSubject.error = "件名を入力してください"
            return
        }
        if (content.isEmpty()) {
            editTextContent.error = "内容を入力してください"
            return
        }
        if (user == null) {
            Toast.makeText(this, "ユーザー情報の取得に失敗しました", Toast.LENGTH_SHORT).show()
            return
        }

        // 送信処理開始
        setLoading(true)

        val inquiryData = hashMapOf(
            "userId" to user.uid,
            "userName" to (user.displayName ?: "No Name"),
            "userEmail" to (user.email ?: "No Email"),
            "subject" to subject,
            "content" to content,
            "createdAt" to FieldValue.serverTimestamp(),
            "status" to "unread" // 管理者サイトでの未読管理用
        )

        db.collection("inquiries")
            .add(inquiryData)
            .addOnSuccessListener {
                setLoading(false)
                Toast.makeText(this, "お問い合わせを送信しました", Toast.LENGTH_LONG).show()
                finish() // 送信完了したら画面を閉じる
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, "送信に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingOverlay.visibility = View.VISIBLE
            buttonSend.isEnabled = false
        } else {
            loadingOverlay.visibility = View.GONE
            buttonSend.isEnabled = true
        }
    }
}