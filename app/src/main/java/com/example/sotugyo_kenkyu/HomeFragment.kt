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
import com.bumptech.glide.Glide // ★ 追加
import com.google.firebase.auth.FirebaseAuth // ★ 追加
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_home.xmlレイアウトを読み込む
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // topBarがステータスバーと重ならないようにパディングを調整
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
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
        loadUserIcon() // ★ 追加: 画面に戻ってきたときにアイコンを更新
    }

    // ★★★ 追加: ユーザーアイコンを読み込む処理 ★★★
    private fun loadUserIcon() {
        val view = view ?: return
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        val user = FirebaseAuth.getInstance().currentUser

        if (user?.photoUrl != null) {
            // 設定された画像がある場合
            Glide.with(this)
                .load(user.photoUrl)
                .circleCrop() // 丸く切り抜く
                .into(userIcon)

            // ※必要であればパディングを調整（画像が小さく見える場合など）
            // userIcon.setPadding(0, 0, 0, 0)
        } else {
            // 画像がない場合はデフォルトアイコン
            Glide.with(this)
                .load(R.drawable.outline_account_circle_24)
                .circleCrop()
                .into(userIcon)
        }
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