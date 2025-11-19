package com.example.sotugyo_kenkyu

import android.content.Context // ★ 追加
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
    }

    override fun onResume() {
        super.onResume()
        updateNotificationBadge()
    }

    private fun updateNotificationBadge() {
        val view = view ?: return
        val badge: TextView = view.findViewById(R.id.textNotificationBadge)
        val db = FirebaseFirestore.getInstance()

        // ★★★ 追加: 最後に見た日時をスマホから取得 ★★★
        val prefs = requireContext().getSharedPreferences("prefs_notification", Context.MODE_PRIVATE)
        val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

        // 全件取得して、日付でフィルタリングする
        db.collection("notifications").get()
            .addOnSuccessListener { result ->
                var unreadCount = 0

                for (document in result) {
                    // Notificationオブジェクトに変換して日付を確認
                    val notification = document.toObject(Notification::class.java)
                    val date = notification.date

                    // 「最後に見た日時」より新しいものだけカウント
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