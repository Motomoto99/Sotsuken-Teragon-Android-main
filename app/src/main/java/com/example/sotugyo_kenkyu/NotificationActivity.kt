package com.example.sotugyo_kenkyu

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var textLatestMessage: TextView
    private lateinit var textUnreadCount: TextView // ★ 追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        textLatestMessage = findViewById(R.id.textLatestMessage)
        textUnreadCount = findViewById(R.id.textUnreadCount) // ★ 追加
        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val buttonAdminChat = findViewById<View>(R.id.buttonAdminChat)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backButton.setOnClickListener { finish() }

        buttonAdminChat.setOnClickListener {
            val intent = Intent(this, AdminChatActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateLatestMessage()
        updateUnreadCount() // ★ 追加: 未読数を更新
    }

    // ★★★ 追加: 未読数をカウントして表示する ★★★
    private fun updateUnreadCount() {
        val db = FirebaseFirestore.getInstance()
        val prefs = getSharedPreferences("prefs_notification", Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

        db.collection("notifications").get()
            .addOnSuccessListener { result ->
                var unreadCount = 0
                for (document in result) {
                    val notification = document.toObject(Notification::class.java)
                    val date = notification.date
                    // 最後に見た時間より新しいものをカウント
                    if (date != null && date.toDate().time > lastSeenTime) {
                        unreadCount++
                    }
                }

                if (unreadCount > 0) {
                    textUnreadCount.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                    textUnreadCount.visibility = View.VISIBLE
                } else {
                    textUnreadCount.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                textUnreadCount.visibility = View.GONE
            }
    }

    private fun updateLatestMessage() {
        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val notification = documents.documents[0].toObject(Notification::class.java)
                    textLatestMessage.text = notification?.content ?: ""
                } else {
                    textLatestMessage.text = "現在、お知らせはありません"
                }
            }
            .addOnFailureListener {
                textLatestMessage.text = "読み込みに失敗しました"
            }
    }
}