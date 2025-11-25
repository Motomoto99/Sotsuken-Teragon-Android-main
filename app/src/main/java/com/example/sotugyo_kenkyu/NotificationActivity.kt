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
import androidx.core.view.updatePadding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.tasks.Tasks // ★追加
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot // ★追加
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

        textLatestMessage = findViewById(R.id.textLatestMessage)
        textUnreadCount = findViewById(R.id.textUnreadCount)
        textTime = findViewById(R.id.textTime)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val buttonAdminChat = findViewById<View>(R.id.buttonAdminChat)
        val header = findViewById<View>(R.id.header)

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

    // ★★★ 修正: 全体と個人の両方を取得して、新しい方を表示する ★★★
    private fun updateLatestMessage(onComplete: () -> Unit = {}) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. 全体のお知らせの最新
        val globalTask = db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()

        // 2. 個人宛のお知らせの最新
        val personalTask = db.collection("users").document(user.uid)
            .collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get()

        // 両方の取得を待つ
        Tasks.whenAllSuccess<QuerySnapshot>(globalTask, personalTask)
            .addOnSuccessListener { results ->
                val globalSnap = results[0]
                val personalSnap = results[1]

                val globalNotif = globalSnap.documents.firstOrNull()?.toObject(Notification::class.java)
                val personalNotif = personalSnap.documents.firstOrNull()?.toObject(Notification::class.java)

                // 日付を比較して新しい方を採用
                val latest = when {
                    globalNotif == null -> personalNotif
                    personalNotif == null -> globalNotif
                    else -> {
                        val gDate = globalNotif.date?.toDate()
                        val pDate = personalNotif.date?.toDate()
                        if (gDate != null && pDate != null && gDate > pDate) globalNotif else personalNotif
                    }
                }

                if (latest != null) {
                    textLatestMessage.text = latest.content
                    if (latest.date != null) {
                        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
                        textTime.text = sdf.format(latest.date.toDate())
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

    // ★★★ 修正: 全体と個人の両方の未読数を合計する ★★★
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

                // 2. 全体のお知らせを取得
                val globalTask = db.collection("notifications").get()

                // 3. 個人宛のお知らせを取得
                val personalTask = db.collection("users").document(user.uid)
                    .collection("notifications").get()

                Tasks.whenAllSuccess<QuerySnapshot>(globalTask, personalTask)
                    .addOnSuccessListener { results ->
                        var unreadCount = 0

                        // 全体のカウント
                        for (doc in results[0]) {
                            val n = doc.toObject(Notification::class.java)
                            if (n.date != null && n.date.toDate().time > threshold) {
                                unreadCount++
                            }
                        }
                        // 個人のカウント
                        for (doc in results[1]) {
                            val n = doc.toObject(Notification::class.java)
                            if (n.date != null && n.date.toDate().time > threshold) {
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