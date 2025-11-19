package com.example.sotugyo_kenkyu

import android.content.Context // ★ 追加
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_admin_chat)

        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewChat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backButton.setOnClickListener { finish() }

        val layoutManager = LinearLayoutManager(this)
        // layoutManager.stackFromEnd = true // 必要ならコメントアウト解除
        recyclerView.layoutManager = layoutManager

        val db = FirebaseFirestore.getInstance()

        // データ取得
        db.collection("notifications")
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Notification::class.java)
                recyclerView.adapter = AdminChatAdapter(list)

                if (list.isNotEmpty()) {
                    recyclerView.scrollToPosition(list.size - 1)

                    // ★★★ 追加: 最新のお知らせの日付を「既読」として保存 ★★★
                    markAsRead(list.last())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "読み込み失敗: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ★★★ 追加: 既読処理 ★★★
    private fun markAsRead(latestNotification: Notification) {
        val date = latestNotification.date
        if (date != null) {
            val prefs = getSharedPreferences("prefs_notification", Context.MODE_PRIVATE)
            val currentSaved = prefs.getLong("last_seen_timestamp", 0L)
            val latestTime = date.toDate().time

            // 今保存されている日付より新しい場合のみ更新
            if (latestTime > currentSaved) {
                prefs.edit().putLong("last_seen_timestamp", latestTime).apply()
            }
        }
    }
}