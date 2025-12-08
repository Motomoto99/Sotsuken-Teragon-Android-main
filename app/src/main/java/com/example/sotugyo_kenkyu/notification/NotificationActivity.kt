package com.example.sotugyo_kenkyu.notification

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sotugyo_kenkyu.R
import com.google.android.material.tabs.TabLayout // 追加
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.core.content.ContextCompat

class NotificationActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerViewNotifications: RecyclerView
    private lateinit var textNoNotification: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var globalListener: ListenerRegistration? = null
    private var personalListener: ListenerRegistration? = null
    private var globalSnap: QuerySnapshot? = null
    private var personalSnap: QuerySnapshot? = null

    // データを保持するリスト
    private var adminMessages = listOf<Notification>()
    private var likeMessages = listOf<Notification>()
    private var iconMap = mapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)

        tabLayout = findViewById(R.id.tabLayout)
        recyclerViewNotifications = findViewById(R.id.recyclerViewNotifications)
        textNoNotification = findViewById(R.id.textNoNotification)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        val backButton = findViewById<ImageButton>(R.id.buttonBack)
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

        recyclerViewNotifications.layoutManager = LinearLayoutManager(this)

        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)

        // 先ほど作った drawable をセット（null安全のために !! をつけています）
        val drawable = ContextCompat.getDrawable(this, R.drawable.divider_line)
        if (drawable != null) {
            itemDecoration.setDrawable(drawable)
        }

        recyclerViewNotifications.addItemDecoration(itemDecoration)

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#4CAF50"))
        swipeRefreshLayout.setOnRefreshListener {
            stopListeners()
            startListeners()
            swipeRefreshLayout.isRefreshing = false
        }

        // ★タブ切り替えリスナー
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateRecyclerView() // タブが選ばれたらリストを更新
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
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
                .limit(20) // リスト表示用に少し多めに取得
                .addSnapshotListener { snapshots, _ ->
                    globalSnap = snapshots
                    processDataAndUpdateUI()
                }
        }

        // 2. 個人宛のお知らせ（いいね＋システム通知）
        if (personalListener == null) {
            personalListener = db.collection("users").document(user.uid)
                .collection("notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshots, _ ->
                    personalSnap = snapshots
                    processDataAndUpdateUI()
                }
        }
    }

    private fun stopListeners() {
        globalListener?.remove()
        globalListener = null
        personalListener?.remove()
        personalListener = null
    }

    // データを分類・加工して保持し、UI更新を呼ぶ
    private fun processDataAndUpdateUI() {
        val tempAdmin = mutableListOf<Notification>()
        val tempLikes = mutableListOf<Notification>()

        // A. 運営・システム通知
        globalSnap?.let { snap ->
            tempAdmin.addAll(snap.toObjects(Notification::class.java))
        }
        personalSnap?.let { snap ->
            val allPersonal = snap.toObjects(Notification::class.java)

            // "いいね" を含むものはLikeリストへ、それ以外はAdminリストへ
            allPersonal.forEach { item ->
                if (item.title.contains("いいね")) {
                    tempLikes.add(item)
                } else {
                    tempAdmin.add(item)
                }
            }
        }

        // 日付順にソート
        adminMessages = tempAdmin.sortedByDescending { it.date?.toDate()?.time ?: 0 }
        likeMessages = tempLikes.sortedByDescending { it.date?.toDate()?.time ?: 0 }

        // アイコン取得が必要なユーザーIDをリスト化（Like通知の送信者など）
        val senderUids = (adminMessages + likeMessages).mapNotNull { it.senderUid }.distinct()

        lifecycleScope.launch {
            if (senderUids.isNotEmpty()) {
                iconMap = fetchUserIcons(senderUids)
            }
            // データの準備ができたら表示更新
            updateRecyclerView()

            // 既読更新処理
            updateLastSeenDate(adminMessages, likeMessages)
        }
    }

    // ★現在のタブに合わせてリストを表示する
    private fun updateRecyclerView() {
        val currentTab = tabLayout.selectedTabPosition
        val targetList = if (currentTab == 0) adminMessages else likeMessages

        if (targetList.isNotEmpty()) {
            recyclerViewNotifications.adapter = NotificationAdapter(targetList, iconMap)
            recyclerViewNotifications.visibility = View.VISIBLE
            textNoNotification.visibility = View.GONE
        } else {
            recyclerViewNotifications.visibility = View.GONE
            textNoNotification.visibility = View.VISIBLE
            // メッセージをタブごとに変える
            textNoNotification.text = if (currentTab == 0) "お知らせはありません" else "まだいいねされていません"
        }
    }

    private fun updateLastSeenDate(adminList: List<Notification>, likeList: List<Notification>) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val allNotifications = adminList + likeList
        val latestNotification = allNotifications.maxByOrNull { it.date?.toDate()?.time ?: 0 }
        val latestDate = latestNotification?.date

        if (latestDate != null) {
            val db = FirebaseFirestore.getInstance()
            val updateData = hashMapOf("lastSeenNotificationDate" to latestDate)
            db.collection("users").document(user.uid).set(updateData, SetOptions.merge())
        }
    }

    private suspend fun fetchUserIcons(uids: List<String>): Map<String, String> {
        val db = FirebaseFirestore.getInstance()
        val resultMap = mutableMapOf<String, String>()

        val deferreds = uids.map { uid ->
            lifecycleScope.async(Dispatchers.IO) {
                try {
                    val doc = db.collection("users").document(uid).get().await()
                    val url = doc.getString("photoUrl")
                    if (!url.isNullOrEmpty()) {
                        uid to url
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
        deferreds.awaitAll().filterNotNull().forEach { (uid, url) ->
            resultMap[uid] = url
        }
        return resultMap
    }
}