package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.View
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_detail)

        // Intentからデータを受け取る
        recordId = intent.getStringExtra("RECORD_ID")
        userId = intent.getStringExtra("USER_ID") // 自分の記録かどうかの確認用などに
        val menuName = intent.getStringExtra("MENU_NAME")
        val memo = intent.getStringExtra("MEMO")
        val imageUrl = intent.getStringExtra("IMAGE_URL")
        val isPublic = intent.getBooleanExtra("IS_PUBLIC", false)
        val rating = intent.getFloatExtra("RATING", 0f)
        val timestamp = intent.getLongExtra("DATE_TIMESTAMP", 0)

        // UI取得
        val header = findViewById<View>(R.id.header)
        val buttonBack = findViewById<ImageButton>(R.id.buttonBack)
        val textHeaderDate = findViewById<TextView>(R.id.textHeaderDate)
        val imageFood = findViewById<ImageView>(R.id.imageFood)
        val textMenuName = findViewById<TextView>(R.id.textMenuName)
        val switchPublic = findViewById<MaterialSwitch>(R.id.switchPublic)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val textTime = findViewById<TextView>(R.id.textTime)
        val textMemo = findViewById<TextView>(R.id.textMemo)
        val buttonEdit = findViewById<Button>(R.id.buttonEdit)
        val buttonDelete = findViewById<Button>(R.id.buttonDelete)

        // WindowInsets
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

        // データをViewに反映
        textMenuName.text = menuName
        textMemo.text = memo
        switchPublic.isChecked = isPublic
        ratingBar.rating = rating

        if (timestamp > 0) {
            val date = Date(timestamp)
            textHeaderDate.text = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN).format(date)
            textTime.text = SimpleDateFormat("HH:mm", Locale.JAPAN).format(date)
        }

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this).load(imageUrl).centerCrop().into(imageFood)
        } else {
            Glide.with(this).load(R.drawable.background_with_logo).centerCrop().into(imageFood)
        }

        // クリックリスナー
        buttonBack.setOnClickListener { finish() }

        buttonEdit.setOnClickListener {
            Toast.makeText(this, "編集機能は未実装です", Toast.LENGTH_SHORT).show()
            // ここにRecordInputActivityへ遷移して編集する処理を追加できます
        }

        buttonDelete.setOnClickListener {
            showDeleteConfirmation()
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
                finish() // 画面を閉じて一覧に戻る
            }
            .addOnFailureListener {
                Toast.makeText(this, "削除に失敗しました", Toast.LENGTH_SHORT).show()
            }
    }
}