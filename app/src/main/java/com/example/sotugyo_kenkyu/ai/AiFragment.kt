package com.example.sotugyo_kenkyu.ai

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class AiFragment : Fragment(), RecognitionListener {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonChatList: ImageButton
    private lateinit var buttonNewChat: ImageButton
    private lateinit var buttonMic: ImageButton
    private lateinit var layoutAiLoading: View

    private lateinit var textAiLoading: TextView
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var buttonCheckRecipe: Button
    private lateinit var buttonCompleteArrange: Button
    private lateinit var actionContainer: View

    // --- 音声認識 (Vosk) ---
    private var model: Model? = null
    private var speechService: SpeechService? = null

    private var isProcessingWakeWord = false

    private val wakeWordTriggers = listOf(
        "どらごん","ドラゴン","dragon",
        "とーく","トーク","talk",
        "ふな","フナ",
        "かもん","カモン","家紋"
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                initModel()
            } else {
                Toast.makeText(requireContext(), "音声認識にはマイク権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }

    private val googleSpeechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.get(0)

                if (!spokenText.isNullOrBlank()) {
                    sendMessage(manualText = spokenText)
                } else {
                    Toast.makeText(requireContext(), "音声が認識できませんでした", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun launchGoogleSpeech() {
        if (isProcessingWakeWord) return
        isProcessingWakeWord = true
        stopRecognition()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "お話しください")
        }

        try {
            googleSpeechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "音声認識機能が見つかりません", Toast.LENGTH_SHORT).show()
            startWakeWordListening()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ★追加: 画面生成時に、まずメモリ上の古いアレンジデータをリセットする
        AiChatSessionManager.clearArrangeData()

        val header = view.findViewById<View>(R.id.header)
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        recyclerView = view.findViewById(R.id.recyclerViewChat)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        buttonChatList = view.findViewById(R.id.buttonChatList)
        buttonNewChat = view.findViewById(R.id.buttonNewChat)
        buttonMic = view.findViewById(R.id.buttonMic)
        layoutAiLoading = view.findViewById(R.id.layoutAiLoading)
        buttonCheckRecipe = view.findViewById(R.id.buttonCheckRecipe)
        buttonCompleteArrange = view.findViewById(R.id.buttonCompleteArrange)
        textAiLoading = view.findViewById(R.id.textAiLoading)
        actionContainer = view.findViewById(R.id.actionContainer)

        val layoutInput = view.findViewById<View>(R.id.layoutInput)

        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomNav = requireActivity().findViewById<View>(R.id.bottomNavigation)
            val bottomNavHeight = bottomNav?.height ?: 0
            val targetBottomMargin = if (isImeVisible) (imeHeight - bottomNavHeight).coerceAtLeast(0) else 0

            layoutInput.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.bottomMargin = targetBottomMargin
            }
            insets
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        buttonSend.isEnabled = false
        updateNewChatButtonState()
        updateArrangeButtons()

        // 「レシピを確認」ボタン
        buttonCheckRecipe.setOnClickListener {
            // 現在のマネージャーが保持している情報を確認して表示
            if (AiChatSessionManager.isCompleted && AiChatSessionManager.arrangedMenuName != null) {
                // 完了済みならアレンジ結果を表示
                showArrangeResultDialog(
                    AiChatSessionManager.arrangedMenuName ?: "アレンジ料理",
                    AiChatSessionManager.arrangedMemo ?: "",
                    AiChatSessionManager.arrangedMaterials ?: "",
                    AiChatSessionManager.arrangedSteps ?: ""
                )
            } else {
                // 未完了なら元のレシピを表示
                showRecipeCheckDialog()
            }
        }

        // 「完了」ボタン
        buttonCompleteArrange.setOnClickListener {
            if (AiChatSessionManager.isCompleted) {
                navigateToRecordInput()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    setLoading(true, "AIがアレンジレシピを作成中...")

                    val result = AiChatSessionManager.generateArrangeSummary()

                    val chatId = AiChatSessionManager.currentChatId
                    if (chatId != null) {
                        // ★修正: アレンジ結果(result)を渡してDBに保存
                        ChatRepository.completeArrangeChat(chatId, result)
                    }

                    // 結果を保存
                    AiChatSessionManager.arrangedMenuName = result.title
                    AiChatSessionManager.arrangedMemo = result.memo
                    AiChatSessionManager.arrangedMaterials = result.materials
                    AiChatSessionManager.arrangedSteps = result.steps

                    AiChatSessionManager.markAsCompleted()

                    updateArrangeButtons()

                    // ポップアップを表示
                    showArrangeResultDialog(result.title, result.memo, result.materials, result.steps)

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "エラーが発生しました", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                setLoading(true, "データを読み込んでいます...")

                val currentId = AiChatSessionManager.currentChatId
                val pendingRecipe = AiChatSessionManager.pendingArrangeRecipe

                if (pendingRecipe != null) {
                    setLoading(true, "AIとアレンジを準備中です...")
                    AiChatSessionManager.pendingArrangeRecipe = null
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()

                    val firstMessage = AiChatSessionManager.startArrangeSession(pendingRecipe)
                    addMessage(firstMessage, isUser = false)
                    scrollToBottom()

                    updateArrangeButtons()
                    updateNewChatButtonState()

                } else if (currentId == null) {
                    AiChatSessionManager.ensureSessionInitialized()
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                    updateArrangeButtons()
                } else {
                    loadExistingChat(currentId)
                    updateArrangeButtons()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "エラーが発生しました: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                setLoading(false, "データを読み込んでいます...")
                buttonSend.isEnabled = true
                updateNewChatButtonState()
            }
        }

        buttonSend.setOnClickListener {
            hideKeyboard()
            sendMessage()
        }

        buttonChatList.setOnClickListener { openChatList() }

        buttonNewChat.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                setLoading(true, "データを読み込んでいます...")
                try {
                    AiChatSessionManager.startNewSession()
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                    updateNewChatButtonState()
                    updateArrangeButtons()
                } finally {
                    setLoading(false, "データを読み込んでいます...")
                }
            }
        }

        buttonMic.setOnClickListener {
            launchGoogleSpeech()
        }

        buttonMic.imageTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        checkPermissionAndInitModel()
    }

    private fun navigateToRecordInput() {
        val menuName = AiChatSessionManager.arrangedMenuName ?: "アレンジ料理"
        val memo = AiChatSessionManager.arrangedMemo ?: ""

        viewLifecycleOwner.lifecycleScope.launch {
            val chatId = AiChatSessionManager.currentChatId
            var recipe: com.example.sotugyo_kenkyu.recipe.Recipe? = null
            if (chatId != null) {
                recipe = ChatRepository.getChatRecipeData(chatId)
            }

            val intent = Intent(requireContext(), com.example.sotugyo_kenkyu.record.RecordInputActivity::class.java)
            intent.putExtra("ARRANGE_MENU_NAME", menuName)
            intent.putExtra("ARRANGE_MEMO", memo)
            if (recipe != null) {
                intent.putExtra("SELECTED_RECIPE", recipe)
            }
            startActivity(intent)
        }
    }

    private fun updateArrangeButtons() {
        val isArrange = AiChatSessionManager.isArrangeMode
        val isCompleted = AiChatSessionManager.isCompleted

        if (isArrange) {
            actionContainer.visibility = View.VISIBLE
            buttonCheckRecipe.visibility = View.VISIBLE
            buttonCompleteArrange.visibility = View.VISIBLE

            if (isCompleted) {
                buttonCompleteArrange.text = "調理完了"
                buttonCompleteArrange.isEnabled = true
                buttonCompleteArrange.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF5722"))
            } else {
                buttonCompleteArrange.text = "カスタマイズ完了"
                buttonCompleteArrange.isEnabled = true
                buttonCompleteArrange.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            }
        } else {
            actionContainer.visibility = View.GONE
            buttonCheckRecipe.visibility = View.GONE
            buttonCompleteArrange.visibility = View.GONE
        }
    }

    private fun showRecipeCheckDialog() {
        val chatId = AiChatSessionManager.currentChatId ?: return

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_recipe_check)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )

        val imgFood = dialog.findViewById<ImageView>(R.id.imageFoodDialog)
        val txtTitle = dialog.findViewById<TextView>(R.id.textRecipeTitleDialog)
        val txtMaterial = dialog.findViewById<TextView>(R.id.textMaterialDialog)
        val txtSteps = dialog.findViewById<TextView>(R.id.textStepsDialog)
        val btnClose = dialog.findViewById<View>(R.id.buttonCloseDialog)

        btnClose.setOnClickListener { dialog.dismiss() }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val recipe = ChatRepository.getChatRecipeData(chatId)

                if (recipe != null) {
                    txtTitle.text = recipe.recipeTitle

                    val materials = recipe.recipeMaterial ?: emptyList()
                    val amounts = recipe.servingAmounts
                    val matBuilder = StringBuilder()
                    for (i in materials.indices) {
                        val amount = if (i < amounts.size) " ... ${amounts[i]}" else ""
                        matBuilder.append("・ ${materials[i]}$amount\n")
                    }
                    txtMaterial.text =
                        if (matBuilder.isNotEmpty()) matBuilder.toString().trim() else "情報なし"

                    val steps = recipe.recipeSteps ?: emptyList()
                    if (steps.isNotEmpty()) {
                        txtSteps.text =
                            steps.mapIndexed { i, s -> "${i + 1}. $s" }.joinToString("\n\n")
                    } else {
                        txtSteps.text = "情報なし"
                    }

                    Glide.with(this@AiFragment)
                        .load(recipe.foodImageUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(imgFood)

                    dialog.show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showArrangeResultDialog(menuName: String, memo: String, materials: String, steps: String) {
        val chatId = AiChatSessionManager.currentChatId ?: return

        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_recipe_check)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            (resources.displayMetrics.heightPixels * 0.8).toInt()
        )

        val textDialogTitle = dialog.findViewById<TextView>(R.id.textDialogTitle)
        val imgFood = dialog.findViewById<ImageView>(R.id.imageFoodDialog)
        val txtTitle = dialog.findViewById<TextView>(R.id.textRecipeTitleDialog)
        val txtMaterial = dialog.findViewById<TextView>(R.id.textMaterialDialog)
        val txtSteps = dialog.findViewById<TextView>(R.id.textStepsDialog)
        val btnClose = dialog.findViewById<View>(R.id.buttonCloseDialog)

        // XMLに元からある静的な「【材料】」や「【作り方】」のテキストを確実に探して消す
        val root = textDialogTitle.parent as? ViewGroup

        var scrollView: ScrollView? = null
        if (root != null) {
            for (i in 0 until root.childCount) {
                val v = root.getChildAt(i)
                if (v is ScrollView) {
                    scrollView = v
                    break
                }
            }
        }

        val linearLayout = scrollView?.getChildAt(0) as? LinearLayout

        if (linearLayout != null) {
            for (i in 0 until linearLayout.childCount) {
                val v = linearLayout.getChildAt(i)
                if (v is TextView) {
                    val text = v.text.toString()
                    if (text.contains("【材料】") || text.contains("【作り方】")) {
                        v.visibility = View.GONE
                    }
                }
            }
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        // 表示内容のセット
        textDialogTitle.text = "アレンジ完成！"
        txtTitle.text = menuName

        // 【メモ】→【材料】の順で結合
        val combinedMaterialText = buildString {
            if (memo.isNotBlank()) {
                append("【メモ】\n")
                append(memo)
                append("\n\n")
            }
            append("【材料】\n")
            append(materials)
        }
        txtMaterial.text = combinedMaterialText

        // 作り方の行間調整 (改行を増やす)
        val formattedSteps = steps
            .replace(Regex("\n+"), "\n") // 正規化
            .replace("\n", "\n\n")       // 行間を空ける

        txtSteps.text = "【作り方】\n$formattedSteps"

        // 画像表示
        viewLifecycleOwner.lifecycleScope.launch {
            val recipe = ChatRepository.getChatRecipeData(chatId)
            if (recipe != null) {
                Glide.with(this@AiFragment)
                    .load(recipe.foodImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imgFood)
            }
        }

        dialog.show()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            stopRecognition()
        } else {
            if (model != null) {
                startWakeWordListening()
            }
            checkPendingContextAndSend()
        }
    }

    override fun onPause() {
        super.onPause()
        stopRecognition()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden && model != null) {
            startWakeWordListening()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechService?.stop()
        speechService = null
    }

    private fun stopRecognition() {
        speechService?.stop()
        speechService = null
    }

    private fun checkPermissionAndInitModel() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            initModel()
        }
    }

    private fun initModel() {
        StorageService.unpack(requireContext(), "vosk-model-small-ja-0.22", "model",
            { model: Model ->
                this.model = model
                if (isVisible) {
                    startWakeWordListening()
                }
            },
            { exception: IOException ->
                Toast.makeText(requireContext(), "モデルエラー: ${exception.message}", Toast.LENGTH_LONG).show()
            })
    }

    private fun startWakeWordListening() {
        if (model == null) return
        isProcessingWakeWord = false
        restartRecognizer()
    }


    private fun restartRecognizer() {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService?.stop()
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onPartialResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)
        if (text.isEmpty()) return

        if (isWakeWordDetected(text)) {
            launchGoogleSpeech()
        }
    }

    override fun onResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)

        if (text.isNotEmpty() && isWakeWordDetected(text)) {
            launchGoogleSpeech()
        }
    }

    override fun onFinalResult(hypothesis: String) {}

    override fun onError(exception: Exception) {}
    override fun onTimeout() {
        editTextMessage.hint = "メッセージを入力..."
        startWakeWordListening()
    }

    private fun parseVoskResult(jsonString: String): String {
        try {
            val jsonObject = JSONObject(jsonString)
            return if (jsonObject.has("partial")) jsonObject.getString("partial")
            else if (jsonObject.has("text")) jsonObject.getString("text")
            else ""
        } catch (e: Exception) {
            return ""
        }
    }

    private fun isWakeWordDetected(text: String): Boolean {
        val cleanText = text.replace(" ", "")
        return wakeWordTriggers.any { cleanText.contains(it) }
    }

    private fun updateNewChatButtonState() {
        if (::buttonNewChat.isInitialized) buttonNewChat.isEnabled = messages.isNotEmpty()
    }

    private fun checkPendingContextAndSend() {
        val pendingMsg = AiChatSessionManager.pendingContext
        if (pendingMsg != null && AiChatSessionManager.chat != null) {
            AiChatSessionManager.pendingContext = null
            sendMessage(manualText = pendingMsg)
        }
    }

    private fun setLoading(isLoading: Boolean, message: String? = null) {
        if (isLoading) {
            layoutAiLoading.visibility = View.VISIBLE
            editTextMessage.isEnabled = false
            if (message != null) {
                textAiLoading.text = message
            } else {
                textAiLoading.text = "読み込み中..."
            }
        } else {
            layoutAiLoading.visibility = View.GONE
            editTextMessage.isEnabled = true
        }
    }

    private suspend fun loadExistingChat(chatId: String) {
        val list = ChatRepository.loadMessages(chatId)
        val (isArrange, isCompleted) = ChatRepository.getChatConfig(chatId)

        AiChatSessionManager.startSessionWithHistory(list)
        AiChatSessionManager.setChatConfig(isArrange, isCompleted)

        // ★追加: 完了済みの場合、DBからアレンジ結果を取得してManagerにセットする
        if (isCompleted) {
            try {
                val arrangedData = ChatRepository.getArrangedRecipe(chatId)
                if (arrangedData != null) {
                    AiChatSessionManager.arrangedMenuName = arrangedData.title
                    AiChatSessionManager.arrangedMemo = arrangedData.memo
                    AiChatSessionManager.arrangedMaterials = arrangedData.materials
                    AiChatSessionManager.arrangedSteps = arrangedData.steps
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        messages.clear()
        messages.addAll(list)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()
    }

    private fun sendMessage(manualText: String? = null) {
        val sessionChat = AiChatSessionManager.chat
        if (sessionChat == null) {
            Toast.makeText(requireContext(), "準備中です…", Toast.LENGTH_SHORT).show()
            return
        }

        val userMessageText = manualText ?: editTextMessage.text.toString().trim()
        if (userMessageText.isEmpty()) return

        addMessage(userMessageText, isUser = true)
        if (manualText == null) editTextMessage.text.clear()
        scrollToBottom()

        buttonSend.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var chatId = AiChatSessionManager.currentChatId
                if (chatId == null) {
                    chatId = ChatRepository.createNewChatSession()
                    AiChatSessionManager.setChatId(chatId)
                }

                ChatRepository.addMessage(chatId, role = "user", text = userMessageText)

                if (!AiChatSessionManager.firstUserMessageSent) {
                    AiChatSessionManager.markFirstUserMessageSent()
                    launch {
                        val newTitle = AiChatSessionManager.generateTitleFromMessage(userMessageText)
                        ChatRepository.updateChatTitle(chatId, newTitle)
                    }
                }

                val response = sessionChat.sendMessage(userMessageText)
                val aiResponseText = response.text ?: "回答がありません"

                addMessage(aiResponseText, isUser = false)
                ChatRepository.addMessage(chatId, role = "assistant", text = aiResponseText)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                buttonSend.isEnabled = true
                scrollToBottom()
            }
        }
    }

    private fun openChatList() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ChatListFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(message = text, isUser = isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        updateNewChatButtonState()
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = activity?.currentFocus ?: view
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        editTextMessage.clearFocus()
    }
}