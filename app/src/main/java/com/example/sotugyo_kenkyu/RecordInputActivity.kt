package com.example.sotugyo_kenkyu

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class RecordInputActivity : AppCompatActivity() {

    private lateinit var imagePhoto: ImageView
    private var selectedImageUri: Uri? = null

    private val calendar = Calendar.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(imagePhoto)

            findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_input)

        val header = findViewById<View>(R.id.header)
        val buttonCancel = findViewById<View>(R.id.buttonCancel)
        val buttonSave = findViewById<View>(R.id.buttonSave)

        val textDate = findViewById<TextView>(R.id.textDate)
        val containerDate = findViewById<View>(R.id.containerDate)

        val cardPhoto = findViewById<View>(R.id.cardPhoto)
        imagePhoto = findViewById(R.id.imagePhoto)

        val inputMenuName = findViewById<EditText>(R.id.inputMenuName)
        val inputMemo = findViewById<EditText>(R.id.inputMemo)
        val switchPublic = findViewById<MaterialSwitch>(R.id.switchPublic)
        val containerRecipe = findViewById<View>(R.id.containerRecipe)

        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (12 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        updateDateText(textDate)

        // ★★★ 修正: キャンセルボタンの処理 ★★★
        buttonCancel.setOnClickListener {
            // 入力内容があるかチェック
            val hasInput = inputMenuName.text.isNotEmpty() ||
                    inputMemo.text.isNotEmpty() ||
                    selectedImageUri != null

            if (hasInput) {
                // 入力がある場合は確認ダイアログを表示
                showDiscardConfirmationDialog()
            } else {
                // 何も入力されていない場合はそのまま閉じる
                finish()
            }
        }

        cardPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        containerDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    updateDateText(textDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        containerRecipe.setOnClickListener {
            Toast.makeText(this, "レシピ選択は未実装です", Toast.LENGTH_SHORT).show()
        }

        buttonSave.setOnClickListener {
            val menuName = inputMenuName.text.toString().trim()

            if (menuName.isEmpty()) {
                Toast.makeText(this, "料理名を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri == null) {
                Toast.makeText(this, "写真を追加してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                finish()
                return@setOnClickListener
            }

            buttonSave.isEnabled = false
            Toast.makeText(this, "保存中...", Toast.LENGTH_SHORT).show()

            uploadImageAndSave(user.uid, menuName, inputMemo.text.toString(), switchPublic.isChecked)
        }
    }

    // ★★★ 追加: 破棄確認ダイアログ ★★★
    private fun showDiscardConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("確認")
            .setMessage("入力したデータはすべて破棄されます。\nよろしいですか？")
            .setPositiveButton("破棄する") { _, _ ->
                finish() // 画面を閉じる
            }
            .setNegativeButton("キャンセル", null) // ダイアログを閉じるだけ
            .show()
    }

    private fun updateDateText(view: TextView) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        view.text = String.format(Locale.getDefault(), "%d/%02d/%02d", year, month, day)
    }

    private fun uploadImageAndSave(uid: String, menuName: String, memo: String, isPublic: Boolean) {
        val storageRef = FirebaseStorage.getInstance().reference
        val filename = UUID.randomUUID().toString()
        val imageRef = storageRef.child("records/$uid/$filename.jpg")

        imageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveRecordToFirestore(uid, menuName, memo, isPublic, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "画像のアップロードに失敗しました", Toast.LENGTH_SHORT).show()
                findViewById<View>(R.id.buttonSave).isEnabled = true
            }
    }

    private fun saveRecordToFirestore(uid: String, menuName: String, memo: String, isPublic: Boolean, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()

        val newRecord = Record(
            userId = uid,
            menuName = menuName,
            date = Timestamp(calendar.time),
            memo = memo,
            imageUrl = imageUrl,
            isPublic = isPublic,
            rating = 0f
        )

        db.collection("users").document(uid).collection("my_records")
            .add(newRecord)
            .addOnSuccessListener { documentReference ->
                documentReference.update("id", documentReference.id)
                showSuccessDialog(uid, documentReference.id)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "保存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                findViewById<View>(R.id.buttonSave).isEnabled = true
            }
    }

    private fun showSuccessDialog(uid: String, recordId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_record_success, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogView.findViewById<View>(R.id.buttonCloseDialog).setOnClickListener {
            val rating = ratingBar.rating
            if (rating > 0) {
                FirebaseFirestore.getInstance()
                    .collection("users").document(uid)
                    .collection("my_records").document(recordId)
                    .update("rating", rating)
            }
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }
}