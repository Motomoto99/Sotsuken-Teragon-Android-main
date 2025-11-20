package com.example.sotugyo_kenkyu

import android.content.Context
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {

    private var notificationListener: ListenerRegistration? = null

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

        // ★★★ 修正: 即時表示処理 ★★★

        // 1. アイコンの表示 (DataLoadingActivityでキャッシュ済みなので即出るはず)
        loadUserIcon()
        loadNotificationIcon()

        // 2. 通知バッジの即時表示 (Intentから受け取る)
        val initialCount = requireActivity().intent.getIntExtra("INITIAL_UNREAD_COUNT", -1)
        if (initialCount >= 0) {
            updateBadgeUI(initialCount)
        }
    }

    override fun onStart() {
        super.onStart()
        startNotificationListener()
    }

    override fun onStop() {
        super.onStop()
        stopNotificationListener()
    }

    override fun onResume() {
        super.onResume()
        // 設定変更からの戻りなどを考慮して再ロード
        loadUserIcon()
    }

    private fun loadUserIcon() {
        val view = view ?: return
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        val user = FirebaseAuth.getInstance().currentUser

        if (user?.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop() // DataLoadingActivityと同じ加工
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

    // バッジの表示を更新する共通メソッド
    private fun updateBadgeUI(count: Int) {
        val view = view ?: return
        val badge: TextView = view.findViewById(R.id.textNotificationBadge)

        if (count > 0) {
            badge.text = if (count > 99) "99+" else count.toString()
            badge.visibility = View.VISIBLE
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun startNotificationListener() {
        if (notificationListener != null) return

        val db = FirebaseFirestore.getInstance()
        val prefs = requireContext().getSharedPreferences("prefs_notification", Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

        notificationListener = db.collection("notifications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                if (snapshots != null) {
                    var unreadCount = 0
                    for (document in snapshots) {
                        val notification = document.toObject(Notification::class.java)
                        val date = notification.date
                        if (date != null && date.toDate().time > lastSeenTime) {
                            unreadCount++
                        }
                    }
                    // リアルタイム更新の結果を反映
                    updateBadgeUI(unreadCount)
                }
            }
    }

    private fun stopNotificationListener() {
        notificationListener?.remove()
        notificationListener = null
    }
}