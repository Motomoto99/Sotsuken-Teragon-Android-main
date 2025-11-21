package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope // ★追加
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch // ★追加

class AllergySettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var checkBoxes: List<CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_allergy_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val header = findViewById<View>(R.id.header)

        checkBoxes = listOf(
            findViewById(R.id.cbEgg), findViewById(R.id.cbMilk),
            findViewById(R.id.cbWheat), findViewById(R.id.cbBuckwheat),
            findViewById(R.id.cbPeanut), findViewById(R.id.cbShrimp),
            findViewById(R.id.cbCrab)
        )

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

        // ★戻るボタン: 保存してから終了する
        backButton.setOnClickListener {
            saveAllergiesAndFinish()
        }

        // データ読み込み
        loadAllergies()
    }

    private fun loadAllergies() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val savedAllergies = document?.get("allergies") as? List<String> ?: emptyList()
                checkBoxes.forEach { checkBox ->
                    if (savedAllergies.contains(checkBox.text.toString())) {
                        checkBox.isChecked = true
                    }
                }
            }
    }

    // ★ 保存して画面を閉じる処理
    private fun saveAllergiesAndFinish() {
        val user = auth.currentUser
        // ユーザー未ログインなどの異常時はそのまま閉じる
        if (user == null) {
            finish()
            return
        }

        val selectedAllergies = checkBoxes.filter { it.isChecked }.map { it.text.toString() }
        val userData = hashMapOf("allergies" to selectedAllergies)

        db.collection("users").document(user.uid)
            .set(userData, SetOptions.merge())
            .addOnSuccessListener {
                // ★変更: 保存成功時にAIセッションを作り直す（プロンプト再読み込み）
                lifecycleScope.launch {
                    try {
                        // ここで新しいセッションを開始すると、最新のアレルギー情報を読み込んでくれます
                        AiChatSessionManager.startNewSession()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        Toast.makeText(this@AllergySettingsActivity, "保存しました", Toast.LENGTH_SHORT).show()
                        finish() // 画面を閉じる
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "保存に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}