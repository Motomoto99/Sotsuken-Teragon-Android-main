package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth // ★追加
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions // ★追加

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
        // layoutManager.stackFromEnd = true
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

                    // ★ 修正: Firestoreに既読日時を保存
                    markAsRead(list.last())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "読み込み失敗: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ★★★ 修正: Firestoreに保存するメソッド ★★★
    private fun markAsRead(latestNotification: Notification) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val date = latestNotification.date ?: return

        val db = FirebaseFirestore.getInstance()

        // "users/{uid}" ドキュメントに "lastSeenNotificationDate" フィールドを保存/更新
        val data = hashMapOf("lastSeenNotificationDate" to date)

        db.collection("users").document(user.uid)
            .set(data, SetOptions.merge()) // 他のデータ（名前など）を消さないようにmerge
            .addOnFailureListener {
                // 失敗しても静かに無視するかログ出す程度でOK
            }
    }
}