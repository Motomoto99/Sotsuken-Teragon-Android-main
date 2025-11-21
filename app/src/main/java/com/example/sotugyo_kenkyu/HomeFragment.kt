package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.Timestamp // ★追加
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot // ★追加

class HomeFragment : Fragment() {

    // 2つのリスナーを保持
    private var notificationListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    // データ保持用
    private var currentSnapshots: QuerySnapshot? = null
    private var lastSeenDate: Timestamp? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topBar = view.findViewById<ConstraintLayout>(R.id.topBar)

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        userIcon.setOnClickListener {
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            startActivity(intent)
        }

        val notificationIcon: ImageButton = view.findViewById(R.id.iconNotification)
        notificationIcon.setOnClickListener {
            val intent = Intent(activity, NotificationActivity::class.java)
            startActivity(intent)
        }

        // 初期表示（DataLoadingActivityからの引数があれば使う）
        // ※リアルタイム監視を入れるので、ここではアイコンロードだけでOK
        loadUserIcon()
        loadNotificationIcon()
    }

    override fun onStart() {
        super.onStart()
        startListeners() // ★ 監視開始
    }

    override fun onStop() {
        super.onStop()
        stopListeners() // ★ 監視終了
    }

    override fun onResume() {
        super.onResume()
        loadUserIcon()
    }

    private fun loadUserIcon() {
        val view = view ?: return
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        val user = FirebaseAuth.getInstance().currentUser

        if (user?.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(userIcon)
        } else {
            Glide.with(this)
                .load(R.drawable.outline_account_circle_24)
                .circleCrop()
                .into(userIcon)
        }
    }

    private fun loadNotificationIcon() {
        val view = view ?: return
        val notificationIcon: ImageButton = view.findViewById(R.id.iconNotification)

        Glide.with(this)
            .load(R.drawable.ic_notifications)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(notificationIcon)
    }

    // ★★★ 2つのデータを監視する処理 ★★★
    private fun startListeners() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. ユーザー情報の監視 (既読日時の変更を検知)
        if (userListener == null) {
            userListener = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        lastSeenDate = snapshot.getTimestamp("lastSeenNotificationDate")
                        recalculateBadge() // 再計算
                    }
                }
        }

        // 2. 通知データの監視 (新しいお知らせを検知)
        if (notificationListener == null) {
            notificationListener = db.collection("notifications")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        currentSnapshots = snapshots
                        recalculateBadge() // 再計算
                    }
                }
        }
    }

    private fun stopListeners() {
        userListener?.remove()
        userListener = null
        notificationListener?.remove()
        notificationListener = null
    }

    // ★★★ バッジの再計算処理 ★★★
    private fun recalculateBadge() {
        val view = view ?: return
        val badge: TextView = view.findViewById(R.id.textNotificationBadge)
        val snapshots = currentSnapshots ?: return // データがまだなければ何もしない

        // 基準となる日時 (未設定なら0=1970年)
        val threshold = lastSeenDate?.toDate()?.time ?: 0L

        var unreadCount = 0
        for (document in snapshots) {
            val notification = document.toObject(Notification::class.java)
            val date = notification.date

            // Firestoreに保存された日時より新しいものをカウント
            if (date != null && date.toDate().time > threshold) {
                unreadCount++
            }
        }

        if (unreadCount > 0) {
            badge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }
}