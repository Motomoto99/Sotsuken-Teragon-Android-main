package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding // ★ 追加
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.firebase.auth.FirebaseAuth // ★ 追加
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationActivity : AppCompatActivity() {

    private lateinit var textLatestMessage: TextView
    private lateinit var textUnreadCount: TextView
    private lateinit var textTime: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        // Viewの取得
        textLatestMessage = findViewById(R.id.textLatestMessage)
        textUnreadCount = findViewById(R.id.textUnreadCount)
        textTime = findViewById(R.id.textTime)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val buttonAdminChat = findViewById<View>(R.id.buttonAdminChat)
        val header = findViewById<View>(R.id.header)

        // WindowInsets設定 (ヘッダーにパディング適用)
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

        buttonAdminChat.setOnClickListener {
            val intent = Intent(this, AdminChatActivity::class.java)
            startActivity(intent)
        }

        swipeRefreshLayout.setColorSchemeColors(android.graphics.Color.parseColor("#4CAF50"))

        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        updateLatestMessage {
            updateUnreadCount {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateLatestMessage(onComplete: () -> Unit = {}) {
        val db = FirebaseFirestore.getInstance()

        db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val notification = documents.documents[0].toObject(Notification::class.java)
                    textLatestMessage.text = notification?.content ?: ""

                    if (notification?.date != null) {
                        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
                        textTime.text = sdf.format(notification.date.toDate())
                    } else {
                        textTime.text = ""
                    }
                } else {
                    textLatestMessage.text = "現在、お知らせはありません"
                    textTime.text = ""
                }
                onComplete()
            }
            .addOnFailureListener {
                textLatestMessage.text = "読み込みに失敗しました"
                textTime.text = ""
                onComplete()
            }
    }

    // ★★★ 修正: Firestoreから既読日時を取得して計算 ★★★
    private fun updateUnreadCount(onComplete: () -> Unit = {}) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            onComplete()
            return
        }

        val db = FirebaseFirestore.getInstance()

        // 1. ユーザーの既読日時を取得
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->
                val lastSeenTimestamp = userDoc.getTimestamp("lastSeenNotificationDate")
                val threshold = lastSeenTimestamp?.toDate()?.time ?: 0L

                // 2. 通知一覧を取得してカウント
                db.collection("notifications").get()
                    .addOnSuccessListener { result ->
                        var unreadCount = 0
                        for (document in result) {
                            val notification = document.toObject(Notification::class.java)
                            val date = notification.date
                            if (date != null && date.toDate().time > threshold) {
                                unreadCount++
                            }
                        }

                        if (unreadCount > 0) {
                            textUnreadCount.text = if (unreadCount > 99) "99+" else unreadCount.toString()
                            textUnreadCount.visibility = View.VISIBLE
                        } else {
                            textUnreadCount.visibility = View.GONE
                        }
                        onComplete()
                    }
                    .addOnFailureListener {
                        textUnreadCount.visibility = View.GONE
                        onComplete()
                    }
            }
            .addOnFailureListener {
                textUnreadCount.visibility = View.GONE
                onComplete()
            }
    }
}