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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout // ★ 追加
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var textNoData: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // ★ 追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        db = FirebaseFirestore.getInstance()

        recyclerView = findViewById(R.id.recyclerViewNotifications)
        textNoData = findViewById(R.id.textNoData)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout) // ★ 追加
        val backButton = findViewById<ImageButton>(R.id.buttonBack)

        recyclerView.layoutManager = LinearLayoutManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        backButton.setOnClickListener { finish() }

        // ★★★ 追加: 引っ張って更新のリスナー ★★★
        swipeRefreshLayout.setOnRefreshListener {
            fetchNotifications()
        }

        // データ取得
        fetchNotifications()
    }

    private fun fetchNotifications() {
        // 読み込み開始時にグルグルを表示（初回ロード用）
        // swipeRefreshLayout.isRefreshing = true
        // ↑ これを入れると自動で回りますが、初回は不要ならコメントアウトでもOK

        db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                // ★★★ 追加: 読み込み完了したらグルグルを止める ★★★
                swipeRefreshLayout.isRefreshing = false

                val notificationList = ArrayList<Notification>()

                for (document in result) {
                    val notification = document.toObject(Notification::class.java)
                    notificationList.add(notification)
                }

                if (notificationList.isEmpty()) {
                    // データがない場合
                    // ★ 変更: RecyclerViewは消さずに、textNoDataだけ出す
                    // (RecyclerViewを消すと引っ張って更新ができなくなることがあるため)
                    textNoData.visibility = View.VISIBLE

                    // 空リストをセットしてクリア
                    recyclerView.adapter = NotificationAdapter(emptyList())
                } else {
                    // データがある場合
                    textNoData.visibility = View.GONE

                    val adapter = NotificationAdapter(notificationList)
                    recyclerView.adapter = adapter
                }
            }
            .addOnFailureListener { exception ->
                // ★★★ 追加: エラー時もグルグルを止める ★★★
                swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this, "データの取得に失敗しました: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}