package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope // ★追加
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch // ★追加
import kotlinx.coroutines.tasks.await // ★追加

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
        // チャットのように下から積む場合は true ですが、今回は日付順に並べるのでデフォルトでOK
        // layoutManager.stackFromEnd = true
        recyclerView.layoutManager = layoutManager

        // ★★★ 修正: 2つのコレクションからデータを取得して結合する ★★★
        lifecycleScope.launch {
            loadCombinedNotifications(recyclerView)
        }
    }

    private suspend fun loadCombinedNotifications(recyclerView: RecyclerView) {
        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser
        val notificationList = mutableListOf<Notification>()

        try {
            // 1. 全体のお知らせを取得
            val globalSnap = db.collection("notifications").get().await()
            notificationList.addAll(globalSnap.toObjects(Notification::class.java))

            // 2. 自分宛てのお知らせを取得（ログインしている場合）
            if (user != null) {
                val personalSnap = db.collection("users")
                    .document(user.uid)
                    .collection("notifications")
                    .get()
                    .await()
                notificationList.addAll(personalSnap.toObjects(Notification::class.java))
            }

            // 3. 日付順（古い順）に並び替え
            // チャット形式なら「古いものが上、新しいものが下」が一般的です
            val sortedList = notificationList.sortedBy { it.date }

            // 4. 表示
            recyclerView.adapter = AdminChatAdapter(sortedList)

            if (sortedList.isNotEmpty()) {
                // 一番下（最新）までスクロール
                recyclerView.scrollToPosition(sortedList.size - 1)

                // 最新のものを既読にする
                markAsRead(sortedList.last())
            }

        } catch (e: Exception) {
            Toast.makeText(this, "読み込み失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun markAsRead(latestNotification: Notification) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val date = latestNotification.date ?: return

        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf("lastSeenNotificationDate" to date)

        db.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
    }
}