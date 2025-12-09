// app/src/main/java/com/example/sotugyo_kenkyu/record/RecordInputActivity.kt
package com.example.sotugyo_kenkyu.record

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.ai.PromptRepository
import com.example.sotugyo_kenkyu.recipe.Recipe
import com.example.sotugyo_kenkyu.recipe.RecipeSelectActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class RecordInputActivity : AppCompatActivity() {

    private lateinit var imagePhoto: ImageView
    private var selectedImageUri: Uri? = null
    private var photoUri: Uri? = null
    private val calendar = Calendar.getInstance()

    private var isEditMode = false
    private var editRecordId: String? = null
    private var originalImageUrl: String = ""
    private var currentRating: Float = 0f
    private var originalPostedAt: Timestamp? = null
    private var selectedRecipe: Recipe? = null

    // 画像判定用AI
    private val imageJudgeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    // アドバイス生成用AI
    private val textGenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                Glide.with(this).load(uri).centerCrop().into(imagePhoto)
                findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
            }
        }

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess && photoUri != null) {
                selectedImageUri = photoUri
                Glide.with(this).load(photoUri).centerCrop().into(imagePhoto)
                findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
            }
        }

    private val recipeSelectLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val recipe = result.data?.getSerializableExtra("SELECTED_RECIPE") as? Recipe
                if (recipe != null) {
                    selectedRecipe = recipe
                    updateRecipeUi()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_record_input)

        checkEditMode()

        val header = findViewById<View>(R.id.header)
        val buttonCancel = findViewById<TextView>(R.id.buttonCancel)
        val buttonSave = findViewById<TextView>(R.id.buttonSave)
        val textTitle = findViewById<TextView>(R.id.textTitle)
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

        if (isEditMode) {
            textTitle.text = "記録の編集"
            inputMenuName.setText(intent.getStringExtra("MENU_NAME"))
            inputMemo.setText(intent.getStringExtra("MEMO"))
            switchPublic.isChecked = intent.getBooleanExtra("IS_PUBLIC", false)
            val timestamp = intent.getLongExtra("DATE_TIMESTAMP", 0)
            if (timestamp > 0) calendar.time = Date(timestamp)
            if (originalImageUrl.isNotEmpty()) {
                Glide.with(this).load(originalImageUrl).centerCrop().into(imagePhoto)
                findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
            }
            val rId = intent.getStringExtra("RECIPE_ID")
            if (!rId.isNullOrEmpty()) {
                selectedRecipe = Recipe(
                    id = rId,
                    recipeTitle = intent.getStringExtra("RECIPE_TITLE") ?: "",
                    recipeUrl = intent.getStringExtra("RECIPE_URL") ?: "",
                    foodImageUrl = intent.getStringExtra("RECIPE_IMAGE_URL") ?: ""
                )
            }
        }

        updateRecipeUi()
        updateSwitchText(switchPublic, switchPublic.isChecked)
        switchPublic.setOnCheckedChangeListener { buttonView, isChecked -> updateSwitchText(buttonView, isChecked) }
        updateDateText(textDate)

        buttonCancel.setOnClickListener { showDiscardConfirmationDialog() }
        cardPhoto.setOnClickListener { showImageSourceDialog() }
        containerDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(year, month, day)
                updateDateText(textDate)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        containerRecipe.setOnClickListener {
            val intent = Intent(this, RecipeSelectActivity::class.java)
            recipeSelectLauncher.launch(intent)
        }

        buttonSave.setOnClickListener {
            val menuName = inputMenuName.text.toString().trim()
            if (menuName.isEmpty()) {
                Toast.makeText(this, "料理名を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isEditMode && selectedImageUri == null) {
                Toast.makeText(this, "写真を追加してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener

            lifecycleScope.launch {
                buttonSave.isEnabled = false
                Toast.makeText(this@RecordInputActivity, "保存中...", Toast.LENGTH_SHORT).show()

                if (selectedImageUri != null) {
                    val bitmap = loadBitmapFromUri(selectedImageUri!!)
                    if (bitmap == null) {
                        Toast.makeText(this@RecordInputActivity, "画像の読み込みに失敗しました", Toast.LENGTH_SHORT).show()
                        buttonSave.isEnabled = true
                        return@launch
                    }
                    val isFood = try { judgeImageIsFood(bitmap) } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this@RecordInputActivity, "画像の判定に失敗しました", Toast.LENGTH_SHORT).show()
                        buttonSave.isEnabled = true
                        return@launch
                    }
                    if (!isFood) {
                        AlertDialog.Builder(this@RecordInputActivity)
                            .setTitle("画像を確認してください")
                            .setMessage("料理の写真ではない可能性があります。")
                            .setPositiveButton("OK", null)
                            .show()
                        buttonSave.isEnabled = true
                        return@launch
                    }
                }

                val memo = inputMemo.text.toString()
                val isPublic = switchPublic.isChecked

                if (selectedImageUri != null) {
                    uploadImageAndSave(user.uid, menuName, memo, isPublic)
                } else if (isEditMode) {
                    saveRecordToFirestore(user.uid, menuName, memo, isPublic, originalImageUrl)
                }
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("カメラで撮影", "アルバムから選択")
        AlertDialog.Builder(this).setTitle("写真を追加").setItems(options) { _, which ->
            when (which) {
                0 -> startCamera()
                1 -> pickImageLauncher.launch("image/*")
            }
        }.show()
    }

    private fun startCamera() {
        val photoFile = File(externalCacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        photoUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun updateRecipeUi() {
        val textRecipeName = findViewById<TextView>(R.id.textRecipeName)
        if (selectedRecipe != null) {
            textRecipeName.text = selectedRecipe!!.recipeTitle
            textRecipeName.setTextColor(getColor(R.color.black))
        } else {
            textRecipeName.text = "レシピを選択"
            textRecipeName.setTextColor(getColor(android.R.color.darker_gray))
        }
    }

    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try { contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
    }

    private suspend fun judgeImageIsFood(bitmap: Bitmap): Boolean {
        val prompt = PromptRepository.getImageJudgePrompt()
        val input = content { image(bitmap); text(prompt) }
        val response = imageJudgeModel.generateContent(input)
        return (response.text?.trim()?.lowercase(Locale.getDefault()) ?: "no") == "yes"
    }

    private fun checkEditMode() {
        val id = intent.getStringExtra("RECORD_ID")
        if (id != null) {
            isEditMode = true
            editRecordId = id
            originalImageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
            currentRating = intent.getFloatExtra("RATING", 0f)
            val postedTime = intent.getLongExtra("POSTED_TIMESTAMP", 0)
            if (postedTime > 0) originalPostedAt = Timestamp(Date(postedTime))
        }
    }

    private fun updateSwitchText(view: CompoundButton, isChecked: Boolean) {
        view.text = if (isChecked) "公開中" else "非公開"
    }

    private fun showDiscardConfirmationDialog() {
        AlertDialog.Builder(this).setTitle("確認").setMessage("破棄しますか？").setPositiveButton("破棄") { _, _ -> finish() }.setNegativeButton("キャンセル", null).show()
    }

    private fun updateDateText(view: TextView) {
        view.text = String.format(Locale.getDefault(), "%d/%02d/%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
    }

    private fun uploadImageAndSave(uid: String, menuName: String, memo: String, isPublic: Boolean) {
        val storageRef = FirebaseStorage.getInstance().reference.child("records/$uid/${UUID.randomUUID()}.jpg")
        storageRef.putFile(selectedImageUri!!).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri -> saveRecordToFirestore(uid, menuName, memo, isPublic, uri.toString()) }
        }.addOnFailureListener {
            Toast.makeText(this, "アップロード失敗", Toast.LENGTH_SHORT).show()
            findViewById<View>(R.id.buttonSave).isEnabled = true
        }
    }

    private fun saveRecordToFirestore(uid: String, menuName: String, memo: String, isPublic: Boolean, imageUrl: String) {
        val db = FirebaseFirestore.getInstance()
        val userRecordsRef = db.collection("users").document(uid).collection("my_records")
        val recordDate = Timestamp(calendar.time)
        val postedAtDate = if (isPublic) Timestamp.now() else null

        val data = hashMapOf<String, Any>(
            "userId" to uid, "menuName" to menuName, "date" to recordDate, "memo" to memo,
            "imageUrl" to imageUrl, "isPublic" to isPublic, "rating" to currentRating,
            "recipeId" to (selectedRecipe?.id ?: ""), "recipeTitle" to (selectedRecipe?.recipeTitle ?: ""),
            "recipeUrl" to (selectedRecipe?.recipeUrl ?: ""), "recipeImageUrl" to (selectedRecipe?.foodImageUrl ?: "")
        )
        if (postedAtDate != null) data["postedAt"] = postedAtDate

        val task = if (isEditMode && editRecordId != null) {
            userRecordsRef.document(editRecordId!!).update(data)
        } else {
            userRecordsRef.add(data).addOnSuccessListener { it.update("id", it.id) }
        }

        task.addOnSuccessListener {
            // ★保存完了後、AIアドバイスを生成・保存してから終了
            generateAndSaveAiAdvice(uid) {
                if (!isEditMode) showSuccessDialog(uid, (task.result as? com.google.firebase.firestore.DocumentReference)?.id ?: "")
                else { Toast.makeText(this, "更新しました", Toast.LENGTH_SHORT).show(); finish() }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "保存失敗: ${it.message}", Toast.LENGTH_SHORT).show()
            findViewById<View>(R.id.buttonSave).isEnabled = true
        }
    }

    // ★追加: AIアドバイスを生成して保存する処理（最新2件使用）
    private fun generateAndSaveAiAdvice(uid: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@RecordInputActivity, "AIがアドバイスを作成中...", Toast.LENGTH_SHORT).show()
                val db = FirebaseFirestore.getInstance()

                // 直近2件（今保存したのも含む）を取得
                val recordsSnapshot = db.collection("users").document(uid).collection("my_records")
                    .orderBy("date", Query.Direction.DESCENDING).limit(2).get().await()
                val recentMenus = recordsSnapshot.documents.mapNotNull { it.getString("menuName") }

                val userSnapshot = db.collection("users").document(uid).get().await()
                val allergies = userSnapshot.get("allergies") as? List<String> ?: emptyList()
                val allergyText = if (allergies.isNotEmpty()) "（ユーザーのアレルギー: ${allergies.joinToString(", ")}）" else ""

                val prompt = """
                   あなたは親しみやすい栄養管理のパートナーキャラクターです。
                    ユーザーの直近の食事記録を見て、40文字以内で短く、次の料理のおすすめを提案してください。
                   栄養バランスへのアドバイスや、褒め言葉を含めてください。
                    
                    【直近の食事】
                    ${recentMenus.joinToString("\n")}
                    
                    $allergyText
                    
                    口調の例：
                    「野菜も摂れていて偉いですね！この調子！」
                    「お肉が続いてますね、次はお魚はいかが？」
                    「美味しそう！でもちょっとカロリー高めかも？」
                """.trimIndent()

                val response = textGenerativeModel.generateContent(prompt)
                val comment = response.text?.trim() ?: "今日も良い食事を！"

                db.collection("users").document(uid).update("latestAiAdvice", comment).await()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }

    private fun showSuccessDialog(uid: String, recordId: String) {
        if (isFinishing || isDestroyed) return
        val dialogView = layoutInflater.inflate(R.layout.dialog_record_success, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.findViewById<View>(R.id.buttonCloseDialog).setOnClickListener {
            if (ratingBar.rating > 0) FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("my_records").document(recordId).update("rating", ratingBar.rating)
            dialog.dismiss()
            finish()
        }
        dialog.show()
    }
}