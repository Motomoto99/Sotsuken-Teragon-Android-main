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

    // カメラ撮影用の一時URIを保持する変数
    private var photoUri: Uri? = null

    private val calendar = Calendar.getInstance()

    // 編集モード用変数
    private var isEditMode = false
    private var editRecordId: String? = null
    private var originalImageUrl: String = ""
    private var currentRating: Float = 0f
    private var originalPostedAt: Timestamp? = null

    // 選択されたレシピを保持する変数
    private var selectedRecipe: Recipe? = null
    private lateinit var loadingOverlay: View
    private lateinit var inputMenuName: EditText
    private lateinit var inputMemo: EditText
    private lateinit var switchPublic: MaterialSwitch
    private lateinit var containerDate: View
    private lateinit var containerRecipe: View
    private lateinit var cardPhoto: View
    private lateinit var buttonSave: TextView
    private lateinit var buttonCancel: TextView
    private lateinit var textDate: TextView
    private lateinit var textTitle: TextView

    // 画像判定用 Firebase AI Logic モデル
    private val imageJudgeModel: GenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    // ★追加: テキスト生成用（アドバイス用）のAIモデル
    private val textGenerativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    // アルバムから選択するランチャー
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(imagePhoto)

                findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
            }
        }

    // カメラ撮影ランチャー
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
            if (isSuccess && photoUri != null) {
                // 撮影成功時の処理
                selectedImageUri = photoUri
                Glide.with(this)
                    .load(photoUri)
                    .centerCrop()
                    .into(imagePhoto)
                findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
            }
        }

    // レシピ選択画面へのランチャー
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

        // --- ビューの取得 (ローカル変数からメンバ変数への代入に変更) ---
        loadingOverlay = findViewById(R.id.loadingOverlay) // 追加
        buttonCancel = findViewById(R.id.buttonCancel)
        buttonSave = findViewById(R.id.buttonSave)
        textTitle = findViewById(R.id.textTitle)
        textDate = findViewById(R.id.textDate)
        containerDate = findViewById(R.id.containerDate)
        cardPhoto = findViewById(R.id.cardPhoto)
        imagePhoto = findViewById(R.id.imagePhoto)
        inputMenuName = findViewById(R.id.inputMenuName)
        inputMemo = findViewById(R.id.inputMemo)
        switchPublic = findViewById(R.id.switchPublic)
        containerRecipe = findViewById(R.id.containerRecipe)

        // 編集モードかどうか判定
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

        // WindowInsets設定
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

        // 編集モードならUIを更新（データのプレフィル）
        if (isEditMode) {
            textTitle.text = "記録の編集"
            inputMenuName.setText(intent.getStringExtra("MENU_NAME"))
            inputMemo.setText(intent.getStringExtra("MEMO"))

            // 既存の公開設定を反映
            val isPublic = intent.getBooleanExtra("IS_PUBLIC", false)
            switchPublic.isChecked = isPublic

            val timestamp = intent.getLongExtra("DATE_TIMESTAMP", 0)
            if (timestamp > 0) {
                calendar.time = Date(timestamp)
            }

            // 画像の表示
            if (originalImageUrl.isNotEmpty()) {
                Glide.with(this).load(originalImageUrl).centerCrop().into(imagePhoto)
                findViewById<View>(R.id.layoutPhotoPlaceholder).visibility = View.GONE
            }

            // 既存のレシピ情報の復元
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

        // レシピ表示の更新（初期表示）
        updateRecipeUi()

        // 初期状態のスイッチテキストを設定
        updateSwitchText(switchPublic, switchPublic.isChecked)

        // スイッチ切り替え時のリスナー
        switchPublic.setOnCheckedChangeListener { buttonView, isChecked ->
            updateSwitchText(buttonView, isChecked)
        }

        // 日付表示更新
        updateDateText(textDate)

        // キャンセルボタン
        buttonCancel.setOnClickListener {
            showDiscardConfirmationDialog()
        }

        // 写真追加ボタン
        cardPhoto.setOnClickListener {
            showImageSourceDialog()
        }

        // 日付選択
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

        // レシピ紐づけ
        containerRecipe.setOnClickListener {
            val intent = Intent(this, RecipeSelectActivity::class.java)
            recipeSelectLauncher.launch(intent)
        }

        // 保存ボタン
        buttonSave.setOnClickListener {
            val menuName = inputMenuName.text.toString().trim()

            if (menuName.isEmpty()) {
                Toast.makeText(this, "料理名を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 新規作成時のみ画像必須
            if (!isEditMode && selectedImageUri == null) {
                Toast.makeText(this, "写真を追加してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                finish()
                return@setOnClickListener
            }

            // AI 判定 → 保存処理
            lifecycleScope.launch {
                Toast.makeText(
                    this@RecordInputActivity,
                    "保存中...",
                    Toast.LENGTH_SHORT
                ).show()
                setLoadingMode(true)

                // 新しい画像が選択されている場合のみ判定する
                if (selectedImageUri != null) {
                    val bitmap = loadBitmapFromUri(selectedImageUri!!)
                    if (bitmap == null) {
                        Toast.makeText(
                            this@RecordInputActivity,
                            "画像の読み込みに失敗しました",
                            Toast.LENGTH_SHORT
                        ).show()
                        setLoadingMode(false)
                        return@launch
                    }

                    val isFood = try {
                        judgeImageIsFood(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@RecordInputActivity,
                            "画像の判定に失敗しました。ネットワーク状態を確認してください。",
                            Toast.LENGTH_SHORT
                        ).show()
                        setLoadingMode(false)
                        return@launch
                    }

                    if (!isFood) {
                        AlertDialog.Builder(this@RecordInputActivity)
                            .setTitle("画像を確認してください")
                            .setMessage("料理や食べ物の写真ではない可能性があります。\n料理の写真に変更してから保存してください。")
                            .setPositiveButton("OK", null)
                            .show()

                        setLoadingMode(false)
                        return@launch
                    }
                }

                // 保存処理へ
                val memo = inputMemo.text.toString()
                val isPublic = switchPublic.isChecked

                if (selectedImageUri != null) {
                    uploadImageAndSave(user.uid, menuName, memo, isPublic)
                } else {
                    if (isEditMode) {
                        saveRecordToFirestore(
                            user.uid,
                            menuName,
                            memo,
                            isPublic,
                            originalImageUrl
                        )
                    } else {
                        setLoadingMode(false)
                    }
                }
            }
        }
    }

    // ★追加: ローディング状態の切り替えメソッド
    private fun setLoadingMode(isLoading: Boolean) {
        // オーバーレイの表示・非表示
        loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE

        // 各UIパーツの有効・無効切り替え
        inputMenuName.isEnabled = !isLoading
        inputMemo.isEnabled = !isLoading
        switchPublic.isEnabled = !isLoading
        containerDate.isEnabled = !isLoading
        containerRecipe.isEnabled = !isLoading
        cardPhoto.isEnabled = !isLoading
        buttonSave.isEnabled = !isLoading
        buttonCancel.isEnabled = !isLoading

        // オーバーレイ表示中はクリックイベントも拾うようにXMLで clickable="true" にしていますが、
        // 念のため背後のビューが反応しないように制御しています。
    }

    // 画像の選択方法を選ぶダイアログを表示
    private fun showImageSourceDialog() {
        val options = arrayOf("カメラで撮影", "アルバムから選択")
        AlertDialog.Builder(this)
            .setTitle("写真を追加")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startCamera() // カメラ起動
                    1 -> pickImageLauncher.launch("image/*") // アルバム起動
                }
            }
            .show()
    }

    // カメラを起動する処理
    private fun startCamera() {
        // 1. 一時保存用のファイルを作成
        val photoFile = File(externalCacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

        // 2. FileProviderを使ってURIを取得
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )

        // クラス変数のphotoUriにも保存しておく（撮影後の処理で使うため）
        photoUri = uri

        // 3. カメラ起動
        takePictureLauncher.launch(uri)
    }

    // レシピ選択状態をUIに反映
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

    // 画像 URI → Bitmap 変換
    private suspend fun loadBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Firebase AI Logic で「料理画像かどうか」を判定
    private suspend fun judgeImageIsFood(bitmap: Bitmap): Boolean {
        val prompt = PromptRepository.getImageJudgePrompt()

        val input = content {
            image(bitmap)
            text(prompt)
        }

        val response = imageJudgeModel.generateContent(input)
        val result = response.text?.trim()?.lowercase(Locale.getDefault()) ?: "no"

        return result == "yes"
    }

    private fun checkEditMode() {
        val id = intent.getStringExtra("RECORD_ID")
        if (id != null) {
            isEditMode = true
            editRecordId = id
            originalImageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
            currentRating = intent.getFloatExtra("RATING", 0f)

            val postedTime = intent.getLongExtra("POSTED_TIMESTAMP", 0)
            if (postedTime > 0) {
                originalPostedAt = Timestamp(Date(postedTime))
            }
        }
    }

    private fun updateSwitchText(view: CompoundButton, isChecked: Boolean) {
        view.text = if (isChecked) "公開中" else "非公開"
    }

    private fun showDiscardConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("確認")
            .setMessage("入力内容は破棄されます。\nよろしいですか？")
            .setPositiveButton("破棄する") { _, _ ->
                finish()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun updateDateText(view: TextView) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        view.text = String.format(Locale.getDefault(), "%d/%02d/%02d", year, month, day)
    }

    private fun uploadImageAndSave(
        uid: String,
        menuName: String,
        memo: String,
        isPublic: Boolean
    ) {
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
                setLoadingMode(false)
            }
    }

    private fun saveRecordToFirestore(
        uid: String,
        menuName: String,
        memo: String,
        isPublic: Boolean,
        imageUrl: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val userRecordsRef = db.collection("users").document(uid).collection("my_records")

        val originalIsPublic = intent.getBooleanExtra("IS_PUBLIC", false)
        val recordDate = Timestamp(calendar.time)

        val postedAtDate = if (isPublic) {
            if (!isEditMode) {
                Timestamp.now()
            } else if (!originalIsPublic) {
                Timestamp.now()
            } else {
                originalPostedAt ?: recordDate
            }
        } else {
            null
        }

        if (isEditMode && editRecordId != null) {
            // 編集時の処理
            val updateData = hashMapOf<String, Any>(
                "menuName" to menuName,
                "date" to recordDate,
                "memo" to memo,
                "imageUrl" to imageUrl,
                "isPublic" to isPublic,
                "rating" to currentRating,
                "recipeId" to (selectedRecipe?.id ?: ""),
                "recipeTitle" to (selectedRecipe?.recipeTitle ?: ""),
                "recipeUrl" to (selectedRecipe?.recipeUrl ?: ""),
                "recipeImageUrl" to (selectedRecipe?.foodImageUrl ?: "")
            )

            if (postedAtDate != null) {
                updateData["postedAt"] = postedAtDate
            }

            userRecordsRef.document(editRecordId!!)
                .update(updateData)
                .addOnSuccessListener {
                    // ★編集後もAIアドバイスを更新する
                    generateAndSaveAiAdvice(uid) {
                        Toast.makeText(this, "更新しました", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    handleSaveError(e)
                }

        } else {
            // 新規登録時の処理
            val newRecord = Record(
                userId = uid,
                menuName = menuName,
                date = recordDate,
                postedAt = postedAtDate,
                memo = memo,
                imageUrl = imageUrl,
                isPublic = isPublic,
                rating = 0f,
                recipeId = selectedRecipe?.id ?: "",
                recipeTitle = selectedRecipe?.recipeTitle ?: "",
                recipeUrl = selectedRecipe?.recipeUrl ?: "",
                recipeImageUrl = selectedRecipe?.foodImageUrl ?: ""
            )

            userRecordsRef.add(newRecord)
                .addOnSuccessListener { documentReference ->
                    documentReference.update("id", documentReference.id)

                    // ★追加: 保存成功後にAIアドバイスを生成して保存
                    generateAndSaveAiAdvice(uid) {
                        // AI生成完了後に成功ダイアログを表示
                        showSuccessDialog(uid, documentReference.id)
                    }
                }
                .addOnFailureListener { e ->
                    handleSaveError(e)
                }
        }
    }

    // ★追加: AIアドバイスを生成してユーザー情報に保存する処理
    private fun generateAndSaveAiAdvice(uid: String, onComplete: () -> Unit) {
        lifecycleScope.launch {
            try {
                // UI表示更新
                Toast.makeText(this@RecordInputActivity, "AIがアドバイスを作成中...", Toast.LENGTH_SHORT).show()

                val db = FirebaseFirestore.getInstance()

                // 1. 直近の食事記録を取得 (最新2件)
                val recordsSnapshot = db.collection("users").document(uid)
                    .collection("my_records")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(2) // 2件のみ参照
                    .get()
                    .await()

                val recentMenus = recordsSnapshot.documents.mapNotNull { doc ->
                    doc.getString("menuName")
                }

                // 2. アレルギー情報を取得
                val userSnapshot = db.collection("users").document(uid).get().await()
                val allergies = userSnapshot.get("allergies") as? List<String> ?: emptyList()

                val allergyText = if (allergies.isNotEmpty()) {
                    "（ユーザーのアレルギー: ${allergies.joinToString(", ")}）"
                } else {
                    ""
                }

                // 3. AIへのプロンプト作成
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

                // 4. AI生成実行
                val response = textGenerativeModel.generateContent(prompt)
                val comment = response.text?.trim() ?: "今日も良い食事を！"

                // 5. 生成されたコメントをユーザー情報として保存
                db.collection("users").document(uid)
                    .update("latestAiAdvice", comment)
                    .await()

            } catch (e: Exception) {
                e.printStackTrace()
                // エラー時はユーザーには通知せず、処理を続行（ログのみ）
            } finally {
                // 成功・失敗に関わらず完了コールバックを呼ぶ
                onComplete()
            }
        }
    }

    private fun handleSaveError(e: Exception) {
        Toast.makeText(this, "保存失敗: ${e.message}", Toast.LENGTH_SHORT).show()
        setLoadingMode(false)
    }

    private fun showSuccessDialog(uid: String, recordId: String) {
        // Activityが終了している場合はダイアログを出さない
        if (isFinishing || isDestroyed) return

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