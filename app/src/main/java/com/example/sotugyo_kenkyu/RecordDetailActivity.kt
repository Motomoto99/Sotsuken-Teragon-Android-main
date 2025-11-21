package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
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
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordDetailActivity : AppCompatActivity() {

    private var recordId: String? = null
    private var userId: String? = null

    private lateinit var textHeaderTitle: TextView
    private lateinit var imageFood: ImageView
    private lateinit var textMenuName: TextView
    private lateinit var switchPublic: MaterialSwitch
    private lateinit var ratingBar: RatingBar
    private lateinit var textDate: TextView
    private lateinit var textTime: TextView
    private lateinit var textMemo: TextView

    // ★追加: 投稿者情報のView
    private lateinit var imageAuthorIcon: ImageView
    private lateinit var textAuthorName: TextView

    private var currentRecord: Record? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_detail)

        recordId = intent.getStringExtra("RECORD_ID")
        userId = intent.getStringExtra("USER_ID")

        val header = findViewById<View>(R.id.header)
        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        textHeaderTitle = findViewById(R.id.textHeaderTitle)

        imageFood = findViewById(R.id.imageFood)
        textMenuName = findViewById(R.id.textMenuName)
        switchPublic = findViewById(R.id.switchPublic)
        ratingBar = findViewById(R.id.ratingBar)

        textDate = findViewById(R.id.textDate)
        textTime = findViewById(R.id.textTime)
        textMemo = findViewById(R.id.textMemo)

        // ★追加: View取得
        imageAuthorIcon = findViewById(R.id.imageAuthorIcon)
        textAuthorName = findViewById(R.id.textAuthorName)

        val buttonEdit = findViewById<ImageButton>(R.id.buttonEdit)
        val buttonDelete = findViewById<ImageButton>(R.id.buttonDelete)

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
        switchPublic.isClickable = false

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && userId != null && currentUser.uid != userId) {
            // 自分以外の投稿の場合
            buttonEdit.visibility = View.GONE
            buttonDelete.visibility = View.GONE
            // ★追加: 投稿者情報をロード
            loadAuthorInfo(userId!!)
        } else {
            // 自分の投稿の場合
            // 必要なら自分の情報を表示しても良いが、ここでは「あなた」とするか、ロードする
            loadAuthorInfo(currentUser?.uid ?: "")
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
                startActivity(intent)
            }
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    // ★追加: 投稿者の情報をFirestoreから取得
    private fun loadAuthorInfo(authorId: String) {
        if (authorId.isEmpty()) return

        FirebaseFirestore.getInstance().collection("users").document(authorId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("username") ?: "名称未設定"
                    val photoUrl = document.getString("photoUrl")

                    textAuthorName.text = name
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .into(imageAuthorIcon)
                    } else {
                        Glide.with(this)
                            .load(R.drawable.outline_account_circle_24)
                            .circleCrop()
                            .into(imageAuthorIcon)
                    }
                } else {
                    textAuthorName.text = "不明なユーザー"
                }
            }
            .addOnFailureListener {
                textAuthorName.text = "読み込みエラー"
            }
    }

    override fun onResume() {
        super.onResume()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && userId != null && currentUser.uid == userId) {
            fetchLatestData()
        }
    }

    private fun displayInitialData() {
        val menuName = intent.getStringExtra("MENU_NAME") ?: ""
        val memo = intent.getStringExtra("MEMO") ?: ""
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        val isPublic = intent.getBooleanExtra("IS_PUBLIC", false)
        val rating = intent.getFloatExtra("RATING", 0f)
        val timestamp = intent.getLongExtra("DATE_TIMESTAMP", 0)

        currentRecord = Record(
            id = recordId ?: "",
            userId = userId ?: "",
            menuName = menuName,
            memo = memo,
            imageUrl = imageUrl,
            isPublic = isPublic,
            rating = rating
        )
        updateUI(menuName, memo, isPublic, rating, timestamp, imageUrl)
    }

    private fun fetchLatestData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
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
        switchPublic.isChecked = isPublic
        switchPublic.text = if (isPublic) "公開中" else "非公開"
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
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("記録を削除")
            .setMessage("本当にこの記録を削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                deleteRecord()
            }
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