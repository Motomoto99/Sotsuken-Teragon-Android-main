package com.example.sotugyo_kenkyu

import android.content.Intent // ★ インポート
import android.os.Bundle
import android.widget.Button // ★ インポート
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast // ★ インポート
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth // ★ インポート
import com.google.firebase.auth.UserProfileChangeRequest

class AccountSettingsActivity : AppCompatActivity() {

    // ★ Firebase Auth のインスタンスを宣言
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_settings)

        // ★ Firebase Auth のインスタンスを初期化
        auth = FirebaseAuth.getInstance()

        // WindowInsetsの調整 (変更なし)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 戻るボタンの処理 (変更なし)
        val backButton: ImageButton = findViewById(R.id.buttonBack)
        backButton.setOnClickListener {
            finish()
        }

        // ★★★ 追加 ★★★
        // サインアウトボタンの処理
        val signOutButton: Button = findViewById(R.id.buttonSignOut)
        signOutButton.setOnClickListener {
            // Firebaseからサインアウト
            auth.signOut()

            // ユーザーに通知
            Toast.makeText(this, "サインアウトしました", Toast.LENGTH_SHORT).show()

            // ログイン画面に戻す
            val intent = Intent(this, LoginActivity::class.java)
            // これまでのタスク（HomeActivityなど）を全て消去して新しいタスクでLoginActivityを起動
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)

            // 現在のAccountSettingsActivityを閉じる
            finish()
        }

        //ユーザ名変更ボタン（鉛筆マーク）を押したとき
        val usernameedit : ImageButton = findViewById(R.id.buttonEditUsername)
        val usernamedisp : TextView = findViewById(R.id.textViewUsername)

        usernameedit.setOnClickListener {
            val user = FirebaseAuth.getInstance().currentUser
            val updateName = usernamedisp.text.toString()

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(updateName)
                .build()

            user?.updateProfile(profileUpdates)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "ユーザ名を変更しました", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "変更失敗: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}