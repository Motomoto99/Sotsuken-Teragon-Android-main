package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var textNoData: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        // Firestoreの初期化
        db = FirebaseFirestore.getInstance()

        // Viewの取得
        recyclerView = findViewById(R.id.recyclerViewNotifications)
        textNoData = findViewById(R.id.textNoData)
        val backButton = findViewById<ImageButton>(R.id.buttonBack)

        // レイアウトマネージャーの設定（縦並びリスト）
        recyclerView.layoutManager = LinearLayoutManager(this)

        // WindowInsets調整
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backButton.setOnClickListener { finish() }

        // お知らせデータの取得
        fetchNotifications()
    }

    private fun fetchNotifications() {
        // "notifications" コレクションから、日付(date)の新しい順に取得
        db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val notificationList = ArrayList<Notification>()

                for (document in result) {
                    // FirestoreのドキュメントをNotificationクラスに変換
                    val notification = document.toObject(Notification::class.java)
                    notificationList.add(notification)
                }

                if (notificationList.isEmpty()) {
                    // データがない場合
                    recyclerView.visibility = View.GONE
                    textNoData.visibility = View.VISIBLE
                } else {
                    // データがある場合、アダプターにセットして表示
                    recyclerView.visibility = View.VISIBLE
                    textNoData.visibility = View.GONE

                    val adapter = NotificationAdapter(notificationList)
                    recyclerView.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "データの取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}