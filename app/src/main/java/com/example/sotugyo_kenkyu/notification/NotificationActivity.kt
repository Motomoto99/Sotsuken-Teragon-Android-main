package com.example.sotugyo_kenkyu.notification

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.sotugyo_kenkyu.R
import com.google.android.material.tabs.TabLayout
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
import java.util.Date

class NotificationActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerViewNotifications: RecyclerView
    private lateinit var textNoNotification: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private var globalListener: ListenerRegistration? = null
    private var personalListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null // ★追加: ユーザー情報監視用

    private var globalSnap: QuerySnapshot? = null
    private var personalSnap: QuerySnapshot? = null

    // データを保持するリスト
    private var adminMessages = listOf<Notification>()
    private var likeMessages = listOf<Notification>()
    private var iconMap = mapOf<String, String>()

    // ★追加: 既読判定用の日時
    private var lastSeenDate: Date? = null
    private var isFirstLoad = true

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

        // 区切り線
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        val drawable = ContextCompat.getDrawable(this, R.drawable.divider_line) // 以前作成したものがあれば
        if (drawable != null) itemDecoration.setDrawable(drawable)
        recyclerViewNotifications.addItemDecoration(itemDecoration)

        swipeRefreshLayout.setColorSchemeColors(Color.parseColor("#4CAF50"))
        swipeRefreshLayout.setOnRefreshListener {
            stopListeners()
            startListeners()
            swipeRefreshLayout.isRefreshing = false
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateRecyclerView()
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
        // ★重要: 画面を閉じる（離れる）タイミングで既読状態を更新する
        updateLastSeenDateToFirestore()
    }

    private fun startListeners() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. ユーザー自身の情報（既読日時）を取得
        if (userListener == null) {
            userListener = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && isFirstLoad) {
                        // 初回だけロードして、あとはローカルで保持する（そうしないと開いた瞬間に未読が消える可能性があるため）
                        // あるいは常に最新を取得して比較に使っても良いが、今回は「開いた時点での未読」を表示するため初回取得値を基準にする
                        val date = snapshot.getTimestamp("lastSeenNotificationDate")?.toDate()
                        if (date != null) {
                            lastSeenDate = date
                            // データが既にロード済みならUI再更新
                            if (globalSnap != null || personalSnap != null) {
                                processDataAndUpdateUI()
                            }
                        }
                        isFirstLoad = false
                    }
                }
        }

        // 2. 全体のお知らせ（運営）
        if (globalListener == null) {
            globalListener = db.collection("notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(20)
                .addSnapshotListener { snapshots, _ ->
                    globalSnap = snapshots
                    processDataAndUpdateUI()
                }
        }

        // 3. 個人宛のお知らせ
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
        userListener?.remove()
        userListener = null
    }

    private fun processDataAndUpdateUI() {
        val tempAdmin = mutableListOf<Notification>()
        val tempLikes = mutableListOf<Notification>()

        globalSnap?.let { snap -> tempAdmin.addAll(snap.toObjects(Notification::class.java)) }
        personalSnap?.let { snap ->
            val allPersonal = snap.toObjects(Notification::class.java)
            allPersonal.forEach { item ->
                if (item.title.contains("いいね")) tempLikes.add(item)
                else tempAdmin.add(item)
            }
        }

        adminMessages = tempAdmin.sortedByDescending { it.date?.toDate()?.time ?: 0 }
        likeMessages = tempLikes.sortedByDescending { it.date?.toDate()?.time ?: 0 }

        // ★未読数を計算してタブに表示
        updateTabBadges()

        val senderUids = (adminMessages + likeMessages).mapNotNull { it.senderUid }.distinct()
        lifecycleScope.launch {
            if (senderUids.isNotEmpty()) {
                iconMap = fetchUserIcons(senderUids)
            }
            updateRecyclerView()
            // ここでの updateLastSeenDate 呼び出しは削除しました
        }
    }

    // ★タブに未読数を表示
    private fun updateTabBadges() {
        val threshold = lastSeenDate?.time ?: 0L

        // 未読数をカウント
        val adminUnreadCount = adminMessages.count { (it.date?.toDate()?.time ?: 0) > threshold }
        val likeUnreadCount = likeMessages.count { (it.date?.toDate()?.time ?: 0) > threshold }

        // タブのテキストを更新
        val tabAdmin = tabLayout.getTabAt(0)
        val tabLike = tabLayout.getTabAt(1)

        tabAdmin?.text = if (adminUnreadCount > 0) "お知らせ ($adminUnreadCount)" else "お知らせ"
        tabLike?.text = if (likeUnreadCount > 0) "いいね ($likeUnreadCount)" else "いいね"
    }

    private fun updateRecyclerView() {
        val currentTab = tabLayout.selectedTabPosition
        val targetList = if (currentTab == 0) adminMessages else likeMessages

        if (targetList.isNotEmpty()) {
            // ★ Adapterに lastSeenDate を渡す
            recyclerViewNotifications.adapter = NotificationAdapter(targetList, iconMap, lastSeenDate)
            recyclerViewNotifications.visibility = View.VISIBLE
            textNoNotification.visibility = View.GONE
        } else {
            recyclerViewNotifications.visibility = View.GONE
            textNoNotification.visibility = View.VISIBLE
            textNoNotification.text = if (currentTab == 0) "お知らせはありません" else "まだいいねされていません"
        }
    }

    // ★画面を閉じる時にFirestoreを更新
    private fun updateLastSeenDateToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val allNotifications = adminMessages + likeMessages
        val latestNotification = allNotifications.maxByOrNull { it.date?.toDate()?.time ?: 0 }
        val latestDate = latestNotification?.date

        // 現在のlastSeenDateより新しい通知がある場合のみ更新
        val currentThreshold = lastSeenDate?.time ?: 0L
        if (latestDate != null && latestDate.toDate().time > currentThreshold) {
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
                    if (!url.isNullOrEmpty()) uid to url else null
                } catch (e: Exception) { null }
            }
        }
        deferreds.awaitAll().filterNotNull().forEach { (uid, url) -> resultMap[uid] = url }
        return resultMap
    }
}