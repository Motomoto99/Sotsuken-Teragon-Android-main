package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot

class HomeFragment : Fragment() {

    private var notificationListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
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

        // ★ 追加: 「もっと見る」ボタンで記録タブへ移動
        val textMore: TextView = view.findViewById(R.id.textMore)
        textMore.setOnClickListener {
            // HomeActivityのBottomNavigationを操作して切り替える
            (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_record
        }

        loadUserIcon()
        loadNotificationIcon()
    }

    override fun onStart() {
        super.onStart()
        startListeners()
    }

    override fun onStop() {
        super.onStop()
        stopListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserIcon()
        // ★ 追加: 画面に戻ってきたときに最新の記録を読み込む
        loadRecentRecords()
    }

    // ★ 追加: Firestoreから最新2件の記録を取得する
    private fun loadRecentRecords() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).collection("my_records")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { documents ->
                val records = documents.toObjects(Record::class.java)
                updateRecentRecordsUI(records)
            }
            .addOnFailureListener {
                // 読み込み失敗時はUI更新しない（非表示のまま）
            }
    }

    // ★ 追加: 取得した記録をUIに反映する
    private fun updateRecentRecordsUI(records: List<Record>) {
        val view = view ?: return
        val card1 = view.findViewById<CardView>(R.id.cardRecord1)
        val img1 = view.findViewById<ImageView>(R.id.imgRecord1)
        val text1 = view.findViewById<TextView>(R.id.textRecordTitle1)

        val card2 = view.findViewById<CardView>(R.id.cardRecord2)
        val img2 = view.findViewById<ImageView>(R.id.imgRecord2)
        val text2 = view.findViewById<TextView>(R.id.textRecordTitle2)

        // 1件目
        if (records.isNotEmpty()) {
            val r1 = records[0]
            card1.visibility = View.VISIBLE
            text1.text = r1.menuName
            if (r1.imageUrl.isNotEmpty()) {
                Glide.with(this).load(r1.imageUrl).centerCrop().into(img1)
            } else {
                Glide.with(this).load(R.drawable.background_with_logo).centerCrop().into(img1)
            }

            card1.setOnClickListener { openRecordDetail(r1) }
        } else {
            card1.visibility = View.INVISIBLE
        }

        // 2件目
        if (records.size >= 2) {
            val r2 = records[1]
            card2.visibility = View.VISIBLE
            text2.text = r2.menuName
            if (r2.imageUrl.isNotEmpty()) {
                Glide.with(this).load(r2.imageUrl).centerCrop().into(img2)
            } else {
                Glide.with(this).load(R.drawable.background_with_logo).centerCrop().into(img2)
            }

            card2.setOnClickListener { openRecordDetail(r2) }
        } else {
            card2.visibility = View.INVISIBLE
        }
    }

    // ★ 追加: 詳細画面を開くヘルパー関数
    private fun openRecordDetail(item: Record) {
        val context = requireContext()
        val intent = Intent(context, RecordDetailActivity::class.java)

        intent.putExtra("RECORD_ID", item.id)
        intent.putExtra("USER_ID", item.userId)
        intent.putExtra("MENU_NAME", item.menuName)
        intent.putExtra("MEMO", item.memo)
        intent.putExtra("IMAGE_URL", item.imageUrl)
        intent.putExtra("IS_PUBLIC", item.isPublic)
        intent.putExtra("RATING", item.rating)
        if (item.date != null) {
            intent.putExtra("DATE_TIMESTAMP", item.date.toDate().time)
        }

        context.startActivity(intent)
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

    private fun startListeners() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        if (userListener == null) {
            userListener = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        lastSeenDate = snapshot.getTimestamp("lastSeenNotificationDate")
                        recalculateBadge()
                    }
                }
        }

        if (notificationListener == null) {
            notificationListener = db.collection("notifications")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        currentSnapshots = snapshots
                        recalculateBadge()
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

    private fun recalculateBadge() {
        val view = view ?: return
        val badge: TextView = view.findViewById(R.id.textNotificationBadge)
        val snapshots = currentSnapshots ?: return

        val threshold = lastSeenDate?.toDate()?.time ?: 0L

        var unreadCount = 0
        for (document in snapshots) {
            val notification = document.toObject(Notification::class.java)
            val date = notification.date

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