package com.example.sotugyo_kenkyu

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var editTextUsername: EditText
    private lateinit var buttonEditUsername: ImageButton
    private lateinit var buttonSaveUsername: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Viewの取得
        editTextUsername = findViewById(R.id.editTextUsername)
        buttonEditUsername = findViewById(R.id.buttonEditUsername)
        buttonSaveUsername = findViewById(R.id.buttonSaveUsername)
        val backButton: ImageButton = findViewById(R.id.buttonBack)
        val signOutButton: Button = findViewById(R.id.buttonSignOut)

        // WindowInsets (ステータスバーの余白調整)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初期状態の設定
        // 無効化時のテキスト色を黒に強制
        editTextUsername.isEnabled = false
        editTextUsername.setTextColor(Color.parseColor("#404040"))

        // ユーザー情報のロード
        loadUserProfile()

        // --- リスナー設定 ---

        backButton.setOnClickListener { finish() }

        signOutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "サインアウトしました", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 鉛筆ボタン：編集モードへ切り替え
        buttonEditUsername.setOnClickListener {
            // EditTextを有効化
            editTextUsername.isEnabled = true

            // フォーカスを当ててキーボードを表示
            editTextUsername.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextUsername, InputMethodManager.SHOW_IMPLICIT)

            // カーソルを末尾に移動
            editTextUsername.setSelection(editTextUsername.text.length)

            // 保存ボタンを表示
            buttonSaveUsername.visibility = View.VISIBLE

            // 鉛筆ボタンを隠す（INVISIBLEにして場所は確保する）
            buttonEditUsername.visibility = View.INVISIBLE
        }

        // 保存ボタン：保存して表示モードへ戻る
        buttonSaveUsername.setOnClickListener {
            saveUsername()
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return

        // Firestoreから取得を試みる
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val username = document?.getString("username")
                    ?: user.displayName
                    ?: "初期ユーザー"
                editTextUsername.setText(username)
            }
            .addOnFailureListener {
                // 失敗時はAuthの情報を使用
                editTextUsername.setText(user.displayName ?: "初期ユーザー")
            }
    }

    private fun saveUsername() {
        val newName = editTextUsername.text.toString().trim()
        val user = auth.currentUser ?: return

        if (newName.isEmpty()) {
            Toast.makeText(this, "ユーザー名を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Authプロフィールの更新
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 2. Firestoreの更新
                    val userData = hashMapOf("username" to newName)
                    db.collection("users").document(user.uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "更新しました", Toast.LENGTH_SHORT).show()

                            // 編集モード終了：元に戻す
                            editTextUsername.isEnabled = false
                            editTextUsername.setTextColor(Color.parseColor("#404040"))

                            // 保存ボタンを隠す
                            buttonSaveUsername.visibility = View.GONE

                            // 鉛筆ボタンを再表示
                            buttonEditUsername.visibility = View.VISIBLE
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "DB更新失敗: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "更新失敗: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}