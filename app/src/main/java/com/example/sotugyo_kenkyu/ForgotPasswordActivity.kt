package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth // これだけでOKです

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_forgot_password)

        // 【変更点】ktxを使わず、標準的な方法で初期化します
        auth = FirebaseAuth.getInstance()

        val backButton: ImageButton = findViewById(R.id.buttonBack)
        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val sendButton: Button = findViewById(R.id.buttonSendEmail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backButton.setOnClickListener {
            finish()
        }

        sendButton.setOnClickListener {
            val email = emailEditText.text.toString()

            if (email.isEmpty()) {
                emailEditText.error = "メールアドレスを入力してください"
                return@setOnClickListener
            }

            sendPasswordResetEmail(email)
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        // 日本語設定 (ここはそのままでOK)
        auth.setLanguageCode("ja")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "再設定メールを送信しました", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "送信に失敗しました: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}