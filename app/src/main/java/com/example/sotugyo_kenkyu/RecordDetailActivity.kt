package com.example.sotugyo_kenkyu

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordDetailActivity : AppCompatActivity() {

    private var recordId: String? = null
    private var userId: String? = null

    private lateinit var textMenuName: TextView
    private lateinit var textPublicStatus: TextView
    private lateinit var ratingBar: RatingBar
    private lateinit var textDate: TextView
    private lateinit var textTime: TextView
    private lateinit var textMemo: TextView
    private lateinit var imageFood: ImageView
    private lateinit var imageAuthorIcon: ImageView
    private lateinit var textAuthorName: TextView

    // いいね用UI
    private lateinit var layoutDetailLike: LinearLayout
    private lateinit var iconDetailLike: ImageView
    private lateinit var textDetailLikeCount: TextView
    private var currentLikedUserIds = mutableListOf<String>()

    private var currentRecord: Record? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_detail)

        recordId = intent.getStringExtra("RECORD_ID")
        userId = intent.getStringExtra("USER_ID")

        // View取得
        val header = findViewById<View>(R.id.header)
        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderTitle = findViewById<TextView>(R.id.textHeaderTitle)
        val buttonEdit = findViewById<ImageButton>(R.id.buttonEdit)
        val buttonDelete = findViewById<ImageButton>(R.id.buttonDelete)

        imageFood = findViewById(R.id.imageFood)
        textMenuName = findViewById(R.id.textMenuName)
        textPublicStatus = findViewById(R.id.textPublicStatus)
        ratingBar = findViewById(R.id.ratingBar)
        textDate = findViewById(R.id.textDate)
        textTime = findViewById(R.id.textTime)
        textMemo = findViewById(R.id.textMemo)
        imageAuthorIcon = findViewById(R.id.imageAuthorIcon)
        textAuthorName = findViewById(R.id.textAuthorName)

        layoutDetailLike = findViewById(R.id.layoutDetailLike)
        iconDetailLike = findViewById(R.id.iconDetailLike)
        textDetailLikeCount = findViewById(R.id.textDetailLikeCount)

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

        textHeaderTitle.text = "記録の詳細"

        val currentUser = FirebaseAuth.getInstance().currentUser
        val myUid = currentUser?.uid ?: ""

        if (currentUser != null && userId != null && currentUser.uid != userId) {
            // 他人の投稿
            buttonEdit.visibility = View.GONE
            buttonDelete.visibility = View.GONE
            loadAuthorInfo(userId!!)
        } else {
            // 自分の投稿
            loadAuthorInfo(myUid)
        }

        // いいねボタンは常に表示
        layoutDetailLike.visibility = View.VISIBLE
        layoutDetailLike.setOnClickListener {
            toggleLike(myUid)
        }

        displayInitialData()

        buttonBack.setOnClickListener { finish() }

        buttonEdit.setOnClickListener {
            if (currentRecord != null) {
                val intent = Intent(this, RecordInputActivity::class.java)
                intent.putExtra("RECORD_ID", currentRecord!!.id)
                intent.putExtra("MENU_NAME", currentRecord!!.menuName)
                intent.putExtra("MEMO", currentRecord!!.memo)
                intent.putExtra("IMAGE_URL", currentRecord!!.imageUrl)
                intent.putExtra("IS_PUBLIC", currentRecord!!.isPublic)
                intent.putExtra("RATING", currentRecord!!.rating)
                if (currentRecord!!.date != null) {
                    intent.putExtra("DATE_TIMESTAMP", currentRecord!!.date!!.toDate().time)
                }
                if (currentRecord!!.postedAt != null) {
                    intent.putExtra("POSTED_TIMESTAMP", currentRecord!!.postedAt!!.toDate().time)
                }
                startActivity(intent)
            }
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    // ★★★ いいね切り替え処理（通知重複防止版） ★★★
    private fun toggleLike(myUid: String) {
        if (myUid.isEmpty() || recordId == null || userId == null) return

        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(userId!!)
            .collection("my_records").document(recordId!!)

        val isLiked = currentLikedUserIds.contains(myUid)

        if (isLiked) {
            // いいね解除（通知リストは変更しない＝一度送ったら送り直さない）
            currentLikedUserIds.remove(myUid)
            updateLikeUI(myUid)
            docRef.update("likedUserIds", FieldValue.arrayRemove(myUid))
        } else {
            // いいね追加
            currentLikedUserIds.add(myUid)
            updateLikeUI(myUid)

            docRef.update("likedUserIds", FieldValue.arrayUnion(myUid))
                .addOnSuccessListener {
                    // ★修正: まだ通知していない場合のみ通知を送る
                    val alreadyNotified = currentRecord?.notifiedUserIds?.contains(myUid) == true

                    if (!alreadyNotified) {
                        sendLikeNotification(myUid)

                        // 通知済みリストに追加して、二度と送らないようにする
                        docRef.update("notifiedUserIds", FieldValue.arrayUnion(myUid))

                        // ローカルデータも更新（連打防止のため）
                        currentRecord?.let {
                            val newNotified = it.notifiedUserIds.toMutableList().apply { add(myUid) }
                            currentRecord = it.copy(notifiedUserIds = newNotified)
                        }
                    }
                }
        }
    }

    // ★★★ 通知送信メソッド ★★★
    private fun sendLikeNotification(myUid: String) {
        val targetUserId = userId ?: return
        // 自分自身の投稿へのいいねは通知しない
        if (targetUserId == myUid) return

        val db = FirebaseFirestore.getInstance()

        // 1. 自分の名前を取得
        db.collection("users").document(myUid).get()
            .addOnSuccessListener { document ->
                // 名前が設定されていなければ「誰か」とする
                val myName = document.getString("username") ?: "誰か"
                val menuName = currentRecord?.menuName ?: "料理"

                // 2. 相手の通知ボックスにメッセージを追加
                // ★アイコン表示のために senderUid を追加
                val notificationData = hashMapOf(
                    "title" to "いいね！",
                    "content" to "${myName}さんが「${menuName}」にいいねしました！",
                    "date" to Timestamp.now(),
                    "senderUid" to myUid
                )

                db.collection("users").document(targetUserId)
                    .collection("notifications")
                    .add(notificationData)
            }
    }

    private fun updateLikeUI(myUid: String) {
        textDetailLikeCount.text = currentLikedUserIds.size.toString()
        if (currentLikedUserIds.contains(myUid)) {
            // いいね済み: 塗りつぶしハート(赤)
            iconDetailLike.setImageResource(R.drawable.ic_heart_filled)
            iconDetailLike.setColorFilter(Color.RED)
        } else {
            // 未いいね: 枠線ハート(グレー)
            iconDetailLike.setImageResource(R.drawable.ic_heart_outline)
            iconDetailLike.setColorFilter(Color.parseColor("#CCCCCC"))
        }
    }

    private fun displayInitialData() {
        val menuName = intent.getStringExtra("MENU_NAME") ?: ""
        val memo = intent.getStringExtra("MEMO") ?: ""
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        val isPublic = intent.getBooleanExtra("IS_PUBLIC", false)
        val rating = intent.getFloatExtra("RATING", 0f)
        val timestamp = intent.getLongExtra("DATE_TIMESTAMP", 0)

        val likedIds = intent.getStringArrayListExtra("LIKED_USER_IDS")
        if (likedIds != null) {
            currentLikedUserIds.clear()
            currentLikedUserIds.addAll(likedIds)
        }

        currentRecord = Record(
            id = recordId ?: "",
            userId = userId ?: "",
            menuName = menuName,
            memo = memo,
            imageUrl = imageUrl,
            isPublic = isPublic,
            rating = rating,
            likedUserIds = likedIds?.toList() ?: emptyList()
        )
        updateUI(menuName, memo, isPublic, rating, timestamp, imageUrl)
    }

    override fun onResume() {
        super.onResume()
        fetchLatestData()
    }

    private fun fetchLatestData() {
        val uid = userId ?: return
        val rId = recordId ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .collection("my_records").document(rId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val record = document.toObject(Record::class.java)
                    if (record != null) {
                        currentRecord = record

                        // データ同期
                        currentLikedUserIds.clear()
                        currentLikedUserIds.addAll(record.likedUserIds)

                        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        updateLikeUI(myUid)

                        val timestamp = record.date?.toDate()?.time ?: 0L
                        updateUI(
                            record.menuName,
                            record.memo,
                            record.isPublic,
                            record.rating,
                            timestamp,
                            record.imageUrl
                        )
                    }
                }
            }
    }

    private fun loadAuthorInfo(authorId: String) {
        if (authorId.isEmpty()) return
        FirebaseFirestore.getInstance().collection("users").document(authorId)
            .get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("username") ?: "名称未設定"
                    val photoUrl = document.getString("photoUrl")
                    textAuthorName.text = name
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this).load(photoUrl).circleCrop().into(imageAuthorIcon)
                    } else {
                        Glide.with(this).load(R.drawable.outline_account_circle_24).circleCrop().into(imageAuthorIcon)
                    }
                } else {
                    textAuthorName.text = "不明なユーザー"
                }
            }
    }

    private fun updateUI(
        menuName: String,
        memo: String,
        isPublic: Boolean,
        rating: Float,
        timestamp: Long,
        imageUrl: String
    ) {
        textMenuName.text = menuName
        textMemo.text = memo

        if (isPublic) {
            textPublicStatus.text = "公開中"
            textPublicStatus.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            textPublicStatus.text = "非公開"
            textPublicStatus.setTextColor(Color.parseColor("#888888"))
        }

        ratingBar.rating = rating

        if (timestamp > 0) {
            val date = Date(timestamp)
            textDate.text = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(date)
            textTime.text = SimpleDateFormat("HH:mm", Locale.JAPAN).format(date)
        } else {
            textDate.text = ""
            textTime.text = ""
        }

        if (imageUrl.isNotEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(imageFood)
        } else {
            Glide.with(this).load(R.drawable.background_with_logo).centerCrop().into(imageFood)
        }

        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        updateLikeUI(myUid)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("記録を削除")
            .setMessage("本当にこの記録を削除しますか？")
            .setPositiveButton("削除") { _, _ -> deleteRecord() }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun deleteRecord() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || recordId == null) return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid)
            .collection("my_records").document(recordId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "削除に失敗しました", Toast.LENGTH_SHORT).show()
            }
    }
}