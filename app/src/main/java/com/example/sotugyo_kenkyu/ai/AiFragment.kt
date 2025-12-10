package com.example.sotugyo_kenkyu.ai

import android.Manifest
import android.app.Activity
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
import android.widget.EditText
import android.widget.ImageButton
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
import com.example.sotugyo_kenkyu.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
import java.util.Locale

class AiFragment : Fragment(), RecognitionListener {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonChatList: ImageButton
    private lateinit var buttonNewChat: ImageButton
    private lateinit var buttonMic: ImageButton
    private lateinit var layoutAiLoading: View

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // --- 音声認識 (Vosk) ---
    private var model: Model? = null
    private var speechService: SpeechService? = null

    // 話している途中のテキストを一時保存する変数
    private var currentPartialText: String = ""

    // 音声認識の状態
    private enum class VoiceState {
        WAITING_WAKE_WORD,
        LISTENING_COMMAND
    }
    private var voiceState = VoiceState.WAITING_WAKE_WORD

    // 権限リクエスト用ランチャー
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                initModel()
            } else {
                Toast.makeText(requireContext(), "音声認識にはマイク権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }

    // ★ Google音声認識の結果を受け取るランチャー
    private val googleSpeechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 結果の取得
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.get(0)

                if (!spokenText.isNullOrBlank()) {
                    // テキストを送信
                    sendMessage(manualText = spokenText)
                } else {
                    Toast.makeText(requireContext(), "音声が認識できませんでした", Toast.LENGTH_SHORT).show()
                }
            }
            // 処理終了後は onResume が呼ばれ、そこで startWakeWordListening() が走るため
            // ここで明示的にVoskを再開する必要はありません。
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        val currentId = AiChatSessionManager.currentChatId
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                setLoading(true)
                if (currentId == null) {
                    AiChatSessionManager.ensureSessionInitialized()
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                } else {
                    loadExistingChat(currentId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                setLoading(false)
                buttonSend.isEnabled = true
                updateNewChatButtonState()
                checkPendingContextAndSend()
            }
        }

        buttonSend.setOnClickListener {
            hideKeyboard()
            sendMessage()
        }

        buttonChatList.setOnClickListener { openChatList() }

        buttonNewChat.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                setLoading(true)
                try {
                    AiChatSessionManager.startNewSession()
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                    updateNewChatButtonState()
                } finally {
                    setLoading(false)
                }
            }
        }

        // ★★★ マイクボタンの処理を修正: Google音声認識(日本語)を呼び出す ★★★
        buttonMic.setOnClickListener {
            // Voskが動いている場合は一時停止（マイクのリソース競合を防ぐ）
            stopRecognition()

            // Google音声認識のインテントを作成
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                // ★ここを "ja-JP" に指定して日本語を強制します
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "お話しください")
            }

            try {
                googleSpeechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "音声認識機能が見つかりません", Toast.LENGTH_SHORT).show()
                // エラーの場合はVosk待機に戻す
                startWakeWordListening()
            }
        }

        checkPermissionAndInitModel()
    }

    // --- 音声認識の制御ロジック (Vosk) ---

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
        speechService?.shutdown()
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
        voiceState = VoiceState.WAITING_WAKE_WORD
        currentPartialText = "" // リセット
        updateMicIconColor()
        restartRecognizer()
    }

    private fun startCommandListening() {
        if (model == null) return
        voiceState = VoiceState.LISTENING_COMMAND
        currentPartialText = "" // リセット
        updateMicIconColor()
        editTextMessage.hint = "お話しください..."
        Toast.makeText(requireContext(), "聞いています... (Vosk)", Toast.LENGTH_SHORT).show()
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

    private fun updateMicIconColor() {
        if (!::buttonMic.isInitialized) return
        if (voiceState == VoiceState.LISTENING_COMMAND) {
            // Voskで聞き取り中は赤色
            buttonMic.imageTintList = ColorStateList.valueOf(Color.RED)
        } else {
            // 待機中はグレー
            buttonMic.imageTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }

    // --- Vosk Callbacks ---

    override fun onPartialResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)
        if (text.isEmpty()) return

        if (voiceState == VoiceState.WAITING_WAKE_WORD) {
            if (isWakeWordDetected(text)) {
                startCommandListening()
            }
        } else {
            // Voskコマンドモード中の処理
            currentPartialText = text
        }
    }

    override fun onResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)

        if (voiceState == VoiceState.WAITING_WAKE_WORD) {
            if (text.isNotEmpty() && isWakeWordDetected(text)) {
                startCommandListening()
            }
        } else {
            // Voskコマンドモードで結果が確定した場合
            if (text.isNotEmpty()) {
                sendMessage(manualText = text)

                // 送信後は待機モードに戻る
                voiceState = VoiceState.WAITING_WAKE_WORD
                currentPartialText = ""
                editTextMessage.hint = "メッセージを入力..."
                updateMicIconColor()
            }
        }
    }

    override fun onFinalResult(hypothesis: String) {
        // Voskがタイムアウトなどで停止した場合の処理
        val text = parseVoskResult(hypothesis)

        if (voiceState == VoiceState.LISTENING_COMMAND && text.isNotEmpty()) {
            sendMessage(manualText = text)

            voiceState = VoiceState.WAITING_WAKE_WORD
            currentPartialText = ""
            editTextMessage.hint = "メッセージを入力..."
            updateMicIconColor()
            startWakeWordListening()
        }
    }

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
        // ウェイクワードの判定ロジック
        return text.contains("カモン") && (text.contains("マーシー") || text.contains("マシー")) ||
                text.contains("マーシー")
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

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            layoutAiLoading.visibility = View.VISIBLE
            editTextMessage.isEnabled = false
        } else {
            layoutAiLoading.visibility = View.GONE
            editTextMessage.isEnabled = true
        }
    }

    private suspend fun loadExistingChat(chatId: String) {
        val list = ChatRepository.loadMessages(chatId)
        messages.clear()
        messages.addAll(list)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()
        AiChatSessionManager.startSessionWithHistory(list)
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