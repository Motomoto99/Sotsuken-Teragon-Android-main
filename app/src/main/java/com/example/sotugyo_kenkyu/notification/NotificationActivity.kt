package com.example.sotugyo_kenkyu.notification

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope // 追加
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sotugyo_kenkyu.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers // 追加
import kotlinx.coroutines.async // 追加
import kotlinx.coroutines.awaitAll // 追加
import kotlinx.coroutines.launch // 追加
import kotlinx.coroutines.tasks.await // 追加
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationActivity : AppCompatActivity() {

    private lateinit var textLatestAdminMessage: TextView
    private lateinit var textAdminTime: TextView
    private lateinit var recyclerViewNotifications: RecyclerView
    private lateinit var textNoNotification: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var globalListener: ListenerRegistration? = null
    private var personalListener: ListenerRegistration? = null
    private var globalSnap: QuerySnapshot? = null
    private var personalSnap: QuerySnapshot? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        textLatestAdminMessage = findViewById(R.id.textLatestAdminMessage)
        textAdminTime = findViewById(R.id.textAdminTime)
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications)
        textNoNotification = findViewById(R.id.textNoNotification)
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

        // 運営チャット画面（運営・システム通知の履歴）へ
        buttonAdminChat.setOnClickListener {
            val intent = Intent(this, AdminChatActivity::class.java)
            startActivity(intent)
        }

        recyclerViewNotifications.layoutManager = LinearLayoutManager(this)

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#4CAF50"))
        swipeRefreshLayout.setOnRefreshListener {
            // リスナーを再登録して更新
            stopListeners()
            startListeners()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        startListeners()
    }

    override fun onPause() {
        super.onPause()
        stopListeners()
    }

    private fun startListeners() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. 全体のお知らせ（運営）
        if (globalListener == null) {
            globalListener = db.collection("notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener { snapshots, _ ->
                    globalSnap = snapshots
                    updateUI()
                }
        }

        // 2. 個人宛のお知らせ（いいね＋システム通知）
        if (personalListener == null) {
            personalListener = db.collection("users").document(user.uid)
                .collection("notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener { snapshots, _ ->
                    personalSnap = snapshots
                    updateUI()
                }
        }
    }

    private fun stopListeners() {
        globalListener?.remove()
        globalListener = null
        personalListener?.remove()
        personalListener = null
    }

    // データを「運営・システム」と「いいね」に振り分けて表示
    private fun updateUI() {
        // --- A. 運営・投稿関連（トーク画面行き） ---
        val adminMessages = mutableListOf<Notification>()

        // 全体お知らせ
        globalSnap?.let { snap ->
            adminMessages.addAll(snap.toObjects(Notification::class.java))
        }
        // 個人のうち「いいね」以外（＝投稿警告など）
        personalSnap?.let { snap ->
            val others = snap.toObjects(Notification::class.java).filter {
                !it.title.contains("いいね")
            }
            adminMessages.addAll(others)
        }

        // 最新1件をカードに表示
        val latestAdmin = adminMessages.maxByOrNull { it.date?.toDate()?.time ?: 0 }
        if (latestAdmin != null) {
            textLatestAdminMessage.text = latestAdmin.content
            if (latestAdmin.date != null) {
                val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
                textAdminTime.text = sdf.format(latestAdmin.date.toDate())
            }
        } else {
            textLatestAdminMessage.text = "お知らせはありません"
            textAdminTime.text = ""
        }

        // --- B. いいね通知（リスト表示） ---
        val likeMessages = mutableListOf<Notification>()

        personalSnap?.let { snap ->
            val likes = snap.toObjects(Notification::class.java).filter {
                it.title.contains("いいね")
            }
            likeMessages.addAll(likes)
        }

        if (likeMessages.isNotEmpty()) {
            // 新しい順にソート
            likeMessages.sortByDescending { it.date?.toDate()?.time ?: 0 }

            // ★修正点: ここで送信者のアイコン情報を一括取得する
            lifecycleScope.launch {
                // 1. 通知に含まれる送信者ID(重複なし)をリスト化
                val senderUids = likeMessages.mapNotNull { it.senderUid }.distinct()

                // 2. 非同期で一括取得
                val iconMap = fetchUserIcons(senderUids)

                // 3. マップをAdapterに渡して表示
                recyclerViewNotifications.adapter = NotificationAdapter(likeMessages, iconMap)
                recyclerViewNotifications.visibility = View.VISIBLE
                textNoNotification.visibility = View.GONE
            }

            // いいね通知が最新なら既読更新
            val user = FirebaseAuth.getInstance().currentUser
            val latestDate = likeMessages[0].date
            if (user != null && latestDate != null) {
                val db = FirebaseFirestore.getInstance()
                val updateData = hashMapOf("lastSeenNotificationDate" to latestDate)
                db.collection("users").document(user.uid).set(updateData, SetOptions.merge())
            }
        } else {
            recyclerViewNotifications.visibility = View.GONE
            textNoNotification.visibility = View.VISIBLE
        }
    }

    // ★追加: ユーザーIDのリストからアイコンURLを一括取得する関数
    private suspend fun fetchUserIcons(uids: List<String>): Map<String, String> {
        val db = FirebaseFirestore.getInstance()
        val resultMap = mutableMapOf<String, String>()

        // 複数の通信処理を並列（同時）に実行
        val deferreds = uids.map { uid ->
            lifecycleScope.async(Dispatchers.IO) {
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    val url = doc.getString("photoUrl")
                    if (!url.isNullOrEmpty()) {
                        uid to url // IDとURLのペアを返す
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        // 全ての結果を待機してMapに格納
        deferreds.awaitAll().filterNotNull().forEach { (uid, url) ->
            resultMap[uid] = url
        }
        return resultMap
    }
}