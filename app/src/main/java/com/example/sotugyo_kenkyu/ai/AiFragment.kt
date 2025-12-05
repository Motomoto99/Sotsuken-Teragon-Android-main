package com.example.sotugyo_kenkyu.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
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

class AiFragment : Fragment(), RecognitionListener {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonChatList: ImageButton
    private lateinit var buttonNewChat: ImageButton
    private lateinit var buttonMic: ImageButton // マイクボタン
    private lateinit var layoutAiLoading: View

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // --- 音声認識 (Vosk) ---
    private var model: Model? = null
    private var speechService: SpeechService? = null

    // 音声認識の状態
    private enum class VoiceState {
        WAITING_WAKE_WORD, // 「カモンマーシー」待ち
        LISTENING_COMMAND  // 命令聞き取り中
    }
    private var voiceState = VoiceState.WAITING_WAKE_WORD

    // 権限リクエスト
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                initModel()
            } else {
                Toast.makeText(requireContext(), "音声認識にはマイク権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // UI初期化
        recyclerView = view.findViewById(R.id.recyclerViewChat)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)
        buttonChatList = view.findViewById(R.id.buttonChatList)
        buttonNewChat = view.findViewById(R.id.buttonNewChat)
        buttonMic = view.findViewById(R.id.buttonMic)
        layoutAiLoading = view.findViewById(R.id.layoutAiLoading)

        val layoutInput = view.findViewById<View>(R.id.layoutInput)
        val header = view.findViewById<View>(R.id.header) // ヘッダーを取得

        // 元々のパディングを保持（XMLで設定した16dpなど）
        val originalHeaderPaddingTop = header.paddingTop

        // キーボード表示時やステータスバーのレイアウト調整
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            // システムバー（ステータスバーなど）のインセットを取得
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomNav = requireActivity().findViewById<View>(R.id.bottomNavigation)
            val bottomNavHeight = bottomNav?.height ?: 0

            // ★修正ポイント: ヘッダーの上部にステータスバー分の余白を追加
            header.updatePadding(top = originalHeaderPaddingTop + systemBars.top)

            // 下部の入力エリアの調整
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

        // チャット履歴読み込み
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

        // --- ボタン動作 ---
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

        // マイクボタン: 手動で「聞き取りモード」を開始
        buttonMic.setOnClickListener {
            if (model != null) {
                startCommandListening()
            } else {
                Toast.makeText(requireContext(), "音声モデル準備中...", Toast.LENGTH_SHORT).show()
            }
        }

        // 音声認識の初期化開始
        checkPermissionAndInitModel()
    }

    // --- 音声認識ロジック ---

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
                startWakeWordListening() // 初期状態はウェイクワード待機
            },
            { exception: IOException ->
                Toast.makeText(requireContext(), "モデルエラー: ${exception.message}", Toast.LENGTH_LONG).show()
            })
    }

    private fun startWakeWordListening() {
        if (model == null) return
        voiceState = VoiceState.WAITING_WAKE_WORD
        updateMicIconColor()
        restartRecognizer()
    }

    private fun startCommandListening() {
        if (model == null) return
        voiceState = VoiceState.LISTENING_COMMAND
        updateMicIconColor()
        editTextMessage.hint = "お話しください..."
        Toast.makeText(requireContext(), "聞いています...", Toast.LENGTH_SHORT).show()
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
            buttonMic.imageTintList = ColorStateList.valueOf(Color.RED)
        } else {
            buttonMic.imageTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }

    // Vosk Listener
    override fun onPartialResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)
        if (text.isEmpty()) return

        if (voiceState == VoiceState.WAITING_WAKE_WORD) {
            if (isWakeWordDetected(text)) {
                startCommandListening()
            }
        }
    }

    override fun onResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)
        if (text.isEmpty()) return

        if (voiceState == VoiceState.WAITING_WAKE_WORD) {
            if (isWakeWordDetected(text)) {
                startCommandListening()
            }
        } else {
            // 聞き取り完了 -> 送信
            sendMessage(manualText = text)
            editTextMessage.hint = "メッセージを入力..."
            startWakeWordListening()
        }
    }

    override fun onFinalResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)
        if (voiceState == VoiceState.LISTENING_COMMAND && text.isNotEmpty()) {
            sendMessage(manualText = text)
            editTextMessage.hint = "メッセージを入力..."
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
        // 「カモン！マーシー！」判定
        return text.contains("カモン") || (text.contains("マーシー") || text.contains("マシー")) ||
                text.contains("マーシー")
    }

    // --- ライフサイクル ---
    override fun onPause() {
        super.onPause()
        speechService?.setPause(true)
    }
    override fun onResume() {
        super.onResume()
        speechService?.setPause(false)
    }
    override fun onDestroyView() {
        super.onDestroyView()
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    // --- 以下、チャット用ヘルパーメソッド ---
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