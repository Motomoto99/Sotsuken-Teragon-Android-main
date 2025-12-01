package com.example.sotugyo_kenkyu.home

import com.example.sotugyo_kenkyu.recipe.Recipe
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sotugyo_kenkyu.account.AccountSettingsActivity
import com.example.sotugyo_kenkyu.notification.Notification
import com.example.sotugyo_kenkyu.notification.NotificationActivity
import com.example.sotugyo_kenkyu.record.PublicRecordsFragment
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.recipe.RecipeDetailFragment
import com.example.sotugyo_kenkyu.record.Record
import com.example.sotugyo_kenkyu.record.RecordDetailActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.example.sotugyo_kenkyu.recipe.SearchInputFragment

class HomeFragment : Fragment() {

    private var notificationListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null
    private var currentSnapshots: QuerySnapshot? = null
    private var lastSeenDate: Timestamp? = null

    private var myRecordList: List<Record> = emptyList()
    private var publicRecordList: List<Record> = emptyList()

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

        // XMLにある検索バー(TextView)を取得してクリックイベントを設定
        val searchBar = view.findViewById<TextView>(R.id.searchBar)
        searchBar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                // アニメーション設定（フェードイン・アウトでヌルッと切り替え）
                .setCustomAnimations(
                    R.anim.fast_fade_in,
                    R.anim.fast_fade_out,
                    R.anim.fast_fade_in,
                    R.anim.fast_fade_out
                )
                // コンテナIDは activity_home.xml のFrameLayoutのIDに合わせてね！
                // たぶん R.id.fragment_container だと思うけど確認して。
                .add(R.id.fragment_container, SearchInputFragment())
                .addToBackStack(null) // 戻れるように履歴に追加
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

        if (myRecordList.isNotEmpty()) {
            updateRecentRecordsUI(myRecordList)
        }
        if (publicRecordList.isNotEmpty()) {
            updateEveryoneRecordsUI(publicRecordList)
        }

        loadUserIcon()
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

    override fun onResume() {
        super.onResume()
        loadUserIcon()
        loadRecentRecords()
        loadEveryoneRecords()
    }

    private fun loadRecentRecords() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid).collection("my_records")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { documents ->
                val records = documents.toObjects(Record::class.java)
                myRecordList = records
                updateRecentRecordsUI(records)
            }
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

    private fun loadEveryoneRecords() {
        val db = FirebaseFirestore.getInstance()

        db.collectionGroup("my_records")
            .whereEqualTo("isPublic", true)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(2)
            .get()
            .addOnSuccessListener { documents ->
                val records = documents.toObjects(Record::class.java)
                publicRecordList = records
                updateEveryoneRecordsUI(records)
            }
            .addOnFailureListener { e ->
                Log.e("HomeFragment", "Error loading everyone records", e)
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

    // ★★★ 修正: FirestoreからアイコンURLを取得して表示する ★★★
    private fun loadUserIcon() {
        val view = view ?: return
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // まずはデフォルトアイコンを表示しておく（読み込み中のチラつき防止）
        Glide.with(this)
            .load(R.drawable.outline_account_circle_24)
            .circleCrop()
            .into(userIcon)

        // Firestoreから最新情報を取得
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val photoUrl = document.getString("photoUrl")
                    // FirestoreにURLが保存されており、かつ空文字でない場合のみ読み込む
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(userIcon)
                    }
                    // 空文字の場合はデフォルトのまま（初期ユーザー状態）
                }
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

    private fun recommend_recipe() {
        val view = view ?: return
        val recommend_image: ImageView = view.findViewById(R.id.imageRecommended)
        val recommend_text: TextView = view.findViewById(R.id.textRecommendedTitle)

        val db = FirebaseFirestore.getInstance()
        db.collection("recommend") // recommendコレクションを参照
            .get()
            .addOnSuccessListener { queryDocumentSnapshots ->
                // ドキュメントが空でないかチェック
                if (!queryDocumentSnapshots.isEmpty) {
                    val size = queryDocumentSnapshots.size()
                    // 0 から size-1 の範囲でランダムなインデックスを生成
                    val randomIndex = (0 until size).random()
                    val doc = queryDocumentSnapshots.documents[randomIndex]

                    // ★重要: ドキュメントをRecipeクラスのオブジェクトに変換
                    val recipe = doc.toObject(Recipe::class.java)

                    if (recipe != null) {
                        // ドキュメントIDをセット（詳細画面でのお気に入り登録などで使うため）
                        recipe.id = doc.id

                        // UIへの表示
                        recommend_text.text = recipe.recipeTitle
                        if (recipe.foodImageUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(recipe.foodImageUrl)
                                .into(recommend_image)
                        } else {
                            recommend_image.setImageResource(R.drawable.funa_smile)
                        }

                        // ★クリックリスナーの実装（データ取得後にセットする）
                        recommend_image.setOnClickListener {
                            // 詳細画面のフラグメントを作成
                            val fragment = RecipeDetailFragment()

                            // データを渡すためのBundleを作成
                            val args = Bundle()
                            args.putSerializable("RECIPE_DATA", recipe)
                            fragment.arguments = args

                            // 画面遷移を実行
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null) // 戻るボタンで戻れるように履歴に追加
                                .commit()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "データ取得に失敗しました", Toast.LENGTH_SHORT).show()
            }
    }
}