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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

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

        // 1. ユーザーアイコン（アカウント設定画面へ遷移）
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        userIcon.setOnClickListener {
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            startActivity(intent)
        }

        // 2. 通知アイコン（お知らせ画面へ遷移）
        val notificationIcon: ImageButton = view.findViewById(R.id.iconNotification)
        notificationIcon.setOnClickListener {
            val intent = Intent(activity, NotificationActivity::class.java)
            startActivity(intent)
        }

        // ★ 追加: 運営アイコンを読み込んで丸く表示
        loadNotificationIcon()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
        loadUserIcon()
        // onResumeでも呼ぶ必要があればここに追加
        // loadNotificationIcon()
    }

    private fun loadUserIcon() {
        val view = view ?: return
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        val user = FirebaseAuth.getInstance().currentUser

        if (user?.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop()
                .into(userIcon)
        } else {
            Glide.with(this)
                .load(R.drawable.outline_account_circle_24)
                .circleCrop()
                .into(userIcon)
        }
    }

    // ★★★ 追加: 運営アイコンを読み込む処理 (デフォルト画像を丸く表示) ★★★
    private fun loadNotificationIcon() {
        val view = view ?: return
        val notificationIcon: ImageButton = view.findViewById(R.id.iconNotification)

        // 運営アイコンは常に特定のデフォルト画像を使用
        Glide.with(this)
            .load(R.drawable.ic_notifications) // 適切な通知アイコンのDrawableを指定
            .circleCrop() // 丸く切り抜く
            .into(notificationIcon)
    }

    private fun updateNotificationBadge() {
        val view = view ?: return
        val badge: TextView = view.findViewById(R.id.textNotificationBadge)
        val db = FirebaseFirestore.getInstance()

        val prefs = requireContext().getSharedPreferences("prefs_notification", Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

        db.collection("notifications").get()
            .addOnSuccessListener { result ->
                var unreadCount = 0

                for (document in result) {
                    val notification = document.toObject(Notification::class.java)
                    val date = notification.date

                    if (date != null && date.toDate().time > lastSeenTime) {
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
            .addOnFailureListener {
                badge.visibility = View.GONE
            }
    }
}
