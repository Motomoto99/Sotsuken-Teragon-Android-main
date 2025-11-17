package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.widget.Button // Buttonをインポート
import android.widget.EditText // EditTextをインポート
import android.widget.ImageButton // ImageButtonをインポート
import android.widget.Toast // Toast（通知）をインポート
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth // Firebase Auth をインポート
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


class EmailLoginActivity : AppCompatActivity() {

    // Firebase Auth のインスタンスを宣言
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_login)

        // Firebase Auth のインスタンスを初期化
        auth = Firebase.auth

        // XMLレイアウトからViewを取得
        val backButton: ImageButton = findViewById(R.id.buttonBack)
        val emailEditText: EditText = findViewById(R.id.editTextEmail)
        val passwordEditText: EditText = findViewById(R.id.editTextPassword)
        val loginButton: Button = findViewById(R.id.buttonLogin)


        // Android 10以降の全画面表示（ステータスバーなど）の調整
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- 1. ログインボタンが押された時の処理 ---
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            // 入力が空でないかチェック
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "メールアドレスとパスワードを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener // 処理を中断
            }

            // Firebaseでログイン
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // ログイン成功
                        Toast.makeText(this, "ログインしました", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish() // このログイン画面を閉じる
                    } else {
                        // ログイン失敗
                        Toast.makeText(this, "ログインに失敗しました: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        // --- 2. 戻るボタンが押された時の処理 ---
        backButton.setOnClickListener {
            finish() // この画面(Activity)を閉じて、前の画面に戻る
        }
    }
}