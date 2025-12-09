package com.example.sotugyo_kenkyu.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.account.AccountSettingsActivity
import com.example.sotugyo_kenkyu.notification.Notification
import com.example.sotugyo_kenkyu.notification.NotificationActivity
import com.example.sotugyo_kenkyu.recipe.Recipe
import com.example.sotugyo_kenkyu.recipe.RecipeDetailFragment
import com.example.sotugyo_kenkyu.recipe.SearchInputFragment
import com.example.sotugyo_kenkyu.record.PublicRecordsFragment
import com.example.sotugyo_kenkyu.record.Record
import com.example.sotugyo_kenkyu.record.RecordDetailActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query // 追加
import com.google.firebase.firestore.QuerySnapshot

class HomeFragment : Fragment() {

    private var notificationListener: ListenerRegistration? = null
    private var globalNotificationListener: ListenerRegistration? = null // ★追加: 全体通知用リスナー
    private var userListener: ListenerRegistration? = null

    private var currentSnapshots: QuerySnapshot? = null
    private var globalSnapshots: QuerySnapshot? = null // ★追加: 全体通知データ
    private var lastSeenDate: Timestamp? = null

    // ViewModelの初期化
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- UI初期化 ---
        val topBar = view.findViewById<ConstraintLayout>(R.id.topBar)

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        val searchBar = view.findViewById<TextView>(R.id.searchBar)
        searchBar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fast_fade_in,
                    R.anim.fast_fade_out,
                    R.anim.fast_fade_in,
                    R.anim.fast_fade_out
                )
                .add(R.id.fragment_container, SearchInputFragment())
                .addToBackStack(null)
                .commit()
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

        val textMore: TextView = view.findViewById(R.id.textMore)
        textMore.setOnClickListener {
            (activity as? HomeActivity)?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_record
        }

        val textMorePublic: TextView = view.findViewById(R.id.textMorePublic)
        textMorePublic.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PublicRecordsFragment())
                .addToBackStack(null)
                .commit()
        }

        // --- SwipeRefreshLayoutの設定 ---
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        // 引っ張った時の処理
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refresh()
            recommend_recipe()
        }

        // ViewModelのロード状態を監視して、終わったらくるくるを止める
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            swipeRefreshLayout.isRefreshing = isLoading
        }

        // --- ViewModelのデータ監視 ---

        viewModel.myRecords.observe(viewLifecycleOwner) { records ->
            updateRecentRecordsUI(records)
        }

        viewModel.publicRecords.observe(viewLifecycleOwner) { records ->
            updateEveryoneRecordsUI(records)
        }

        viewModel.userIconUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                Glide.with(this)
                    .load(url)
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

        // 初回データの読み込み
        viewModel.loadDataIfNeeded()

        loadNotificationIcon()
        recommend_recipe()
    }

    override fun onStart() {
        super.onStart()
        startListeners()
    }

    override fun onStop() {
        super.onStop()
        stopListeners()
    }

    private fun updateRecentRecordsUI(records: List<Record>) {
        val view = view ?: return
        val card1 = view.findViewById<CardView>(R.id.cardRecord1)
        val img1 = view.findViewById<ImageView>(R.id.imgRecord1)
        val text1 = view.findViewById<TextView>(R.id.textRecordTitle1)

        val card2 = view.findViewById<CardView>(R.id.cardRecord2)
        val img2 = view.findViewById<ImageView>(R.id.imgRecord2)
        val text2 = view.findViewById<TextView>(R.id.textRecordTitle2)

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

    private fun updateEveryoneRecordsUI(records: List<Record>) {
        val view = view ?: return
        val card1 = view.findViewById<CardView>(R.id.cardPublic1)
        val img1 = view.findViewById<ImageView>(R.id.imgPublic1)
        val text1 = view.findViewById<TextView>(R.id.textPublicTitle1)

        val card2 = view.findViewById<CardView>(R.id.cardPublic2)
        val img2 = view.findViewById<ImageView>(R.id.imgPublic2)
        val text2 = view.findViewById<TextView>(R.id.textPublicTitle2)

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

    private fun loadNotificationIcon() {
        val view = view ?: return
        val notificationIcon: ImageButton = view.findViewById(R.id.iconNotification)
        Glide.with(this).load(R.drawable.ic_notifications).circleCrop().into(notificationIcon)
    }

    private fun startListeners() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. ユーザー情報（既読日時）を監視
        if (userListener == null) {
            userListener = db.collection("users").document(user.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        lastSeenDate = snapshot.getTimestamp("lastSeenNotificationDate")
                        recalculateBadge()
                    }
                }
        }

        // 2. 自分宛ての通知を監視
        if (notificationListener == null) {
            notificationListener = db.collection("users").document(user.uid)
                .collection("notifications")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        currentSnapshots = snapshots
                        recalculateBadge()
                    }
                }
        }

        // 3. ★追加: 全体へのお知らせ（運営通知）を監視
        if (globalNotificationListener == null) {
            globalNotificationListener = db.collection("notifications")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(5) // 最新5件だけ監視（負荷軽減）
                .addSnapshotListener { snapshots, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshots != null) {
                        globalSnapshots = snapshots
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
        globalNotificationListener?.remove() // ★追加: 解除
        globalNotificationListener = null
    }

    // ★修正: 個人宛てと全体宛ての両方をチェック
    private fun recalculateBadge() {
        val view = view ?: return
        // IDを textNotificationBadge に戻したため、それに合わせて取得
        val badge: TextView = view.findViewById(R.id.textNotificationBadge)

        val threshold = lastSeenDate?.toDate()?.time ?: 0L
        var unreadCount = 0

        // A. 個人通知のカウント
        currentSnapshots?.let { snapshots ->
            for (document in snapshots) {
                val notification = document.toObject(Notification::class.java)
                val date = notification.date
                if (date != null && date.toDate().time > threshold) {
                    unreadCount++
                }
            }
        }

        // B. 全体通知のカウント
        globalSnapshots?.let { snapshots ->
            for (document in snapshots) {
                val notification = document.toObject(Notification::class.java)
                val date = notification.date
                if (date != null && date.toDate().time > threshold) {
                    unreadCount++
                }
            }
        }

        // 表示更新
        if (unreadCount > 0) {
            badge.visibility = View.VISIBLE
            // 99件を超える場合は "99+" と表示
            badge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
        } else {
            badge.visibility = View.GONE
        }
    }

    private fun recommend_recipe() {
        val view = view ?: return
        val recommend_image: ImageView = view.findViewById(R.id.imageRecommended)
        val recommend_text: TextView = view.findViewById(R.id.textRecommendedTitle)
        val recommend_card: CardView = view.findViewById(R.id.cardRecommended)

        val db = FirebaseFirestore.getInstance()
        db.collection("recommend")
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                if (!queryDocumentSnapshots.isEmpty) {
                    val size = queryDocumentSnapshots.size()
                    val randomIndex = (0 until size).random()
                    val doc = queryDocumentSnapshots.documents[randomIndex]

                    val recipe = doc.toObject(Recipe::class.java)

                    if (recipe != null) {
                        recipe.id = doc.id
                        recommend_text.text = recipe.recipeTitle
                        if (recipe.foodImageUrl.isNotEmpty()) {
                            Glide.with(this).load(recipe.foodImageUrl).centerCrop().into(recommend_image)
                        } else {
                            recommend_image.setImageResource(R.drawable.funa_smile)
                        }

                        recommend_card.setOnClickListener {
                            val fragment = RecipeDetailFragment()
                            val args = Bundle()
                            args.putSerializable("RECIPE_DATA", recipe)
                            fragment.arguments = args
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit()
                        }
                    }
                }
            }
            .addOnFailureListener {
                // エラー処理
            }
    }
}