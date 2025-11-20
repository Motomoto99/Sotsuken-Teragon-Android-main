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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog // ★ 追加
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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
    private lateinit var textCharLimit: TextView

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
        textCharLimit = findViewById(R.id.textCharLimit)

        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val header = findViewById<View>(R.id.header)

        val menuAllergy = findViewById<View>(R.id.menuAllergySettings)
        val menuSignOut = findViewById<View>(R.id.menuSignOut)

        // WindowInsets (ヘッダーのパディング調整)
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

        // 初期設定
        editTextUsername.isEnabled = false
        editTextUsername.setTextColor(Color.parseColor("#404040"))

        loadUserProfile()

        // --- リスナー設定 ---
        backButton.setOnClickListener { finish() }

        // アレルギー設定画面へ遷移
        menuAllergy.setOnClickListener {
            val intent = Intent(this, AllergySettingsActivity::class.java)
            startActivity(intent)
        }

        // ★★★ 修正箇所: サインアウト確認ダイアログを表示 ★★★
        menuSignOut.setOnClickListener {
            showSignOutConfirmation()
        }

        // ユーザー名編集ロジック
        buttonEditUsername.setOnClickListener {
            editTextUsername.isEnabled = true
            editTextUsername.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextUsername, InputMethodManager.SHOW_IMPLICIT)
            editTextUsername.setSelection(editTextUsername.text.length)

            buttonSaveUsername.visibility = View.VISIBLE
            buttonEditUsername.visibility = View.INVISIBLE
            textCharLimit.visibility = View.VISIBLE
        }

        buttonSaveUsername.setOnClickListener {
            saveUsername()
        }
    }

    // ★★★ 追加: 確認ダイアログ表示メソッド ★★★
    private fun showSignOutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("サインアウト")
            .setMessage("本当にサインアウトしますか？")
            .setPositiveButton("サインアウト") { _, _ ->
                // 「サインアウト」が押されたら実行
                performSignOut()
            }
            .setNegativeButton("キャンセル", null) // キャンセルなら何もしない
            .show()
    }

    // ★★★ 追加: 実際のサインアウト処理 ★★★
    private fun performSignOut() {
        auth.signOut()
        Toast.makeText(this, "サインアウトしました", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val username = document?.getString("username")
                    ?: user.displayName
                    ?: "初期ユーザー"
                editTextUsername.setText(username)
            }
            .addOnFailureListener {
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

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = hashMapOf("username" to newName)
                    db.collection("users").document(user.uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "更新しました", Toast.LENGTH_SHORT).show()

                            editTextUsername.isEnabled = false
                            editTextUsername.setTextColor(Color.parseColor("#404040"))

                            buttonSaveUsername.visibility = View.GONE
                            buttonEditUsername.visibility = View.VISIBLE
                            textCharLimit.visibility = View.GONE
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