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
    private lateinit var buttonMic: ImageButton
    private lateinit var layoutAiLoading: View

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    // --- 音声認識 (Vosk) ---
    private var model: Model? = null
    private var speechService: SpeechService? = null

    // 話している途中のテキストを一時保存する変数（★追加）
    private var currentPartialText: String = ""

    // 音声認識の状態
    private enum class VoiceState {
        WAITING_WAKE_WORD,
        LISTENING_COMMAND
    }
    private var voiceState = VoiceState.WAITING_WAKE_WORD

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

        // ★★★ マイクボタンの処理を修正 ★★★
        buttonMic.setOnClickListener {
            if (model == null) {
                Toast.makeText(requireContext(), "音声モデル準備中...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (voiceState == VoiceState.LISTENING_COMMAND) {
                // すでに聞き取り中の場合 → 手動で停止して送信
                manualStopAndSend()
            } else {
                // 待機中の場合 → 聞き取り開始
                startCommandListening()
            }
        }

        checkPermissionAndInitModel()
    }

    // --- 音声認識の制御ロジック ---

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
        Toast.makeText(requireContext(), "聞いています...", Toast.LENGTH_SHORT).show()
        restartRecognizer()
    }

    // ★★★ 手動で停止して送信する関数 ★★★
    private fun manualStopAndSend() {
        // 音声認識を止める
        speechService?.stop()

        // 状態を待機モードに戻す（これで onFinalResult が二重送信するのを防ぐ）
        voiceState = VoiceState.WAITING_WAKE_WORD

        if (currentPartialText.isNotBlank()) {
            sendMessage(manualText = currentPartialText)
        } else {
            Toast.makeText(requireContext(), "音声が聞き取れませんでした", Toast.LENGTH_SHORT).show()
        }

        // リセット処理
        currentPartialText = ""
        editTextMessage.hint = "メッセージを入力..."
        updateMicIconColor()

        // ウェイクワード待機を再開
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
            // 聞き取り中は赤色
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
            // ★重要: 聞き取り中は、話している内容を常に変数に保存しておく
            currentPartialText = text
            // オプション: EditTextにリアルタイム表示したければここで setText する
        }
    }

    override fun onResult(hypothesis: String) {
        val text = parseVoskResult(hypothesis)

        if (voiceState == VoiceState.WAITING_WAKE_WORD) {
            if (text.isNotEmpty() && isWakeWordDetected(text)) {
                startCommandListening()
            }
        } else {
            // コマンドモードで結果が確定した場合
            if (text.isNotEmpty()) {
                sendMessage(manualText = text)

                // 送信後は待機モードに戻る
                voiceState = VoiceState.WAITING_WAKE_WORD
                currentPartialText = ""
                editTextMessage.hint = "メッセージを入力..."
                updateMicIconColor()
                // restartRecognizer() は onResultの仕組み上、自動で継続されるので不要な場合もあるが
                // 状態リセットのために明示的に呼んでも良い。ここではVoskの仕様に任せる。
            }
        }
    }

    override fun onFinalResult(hypothesis: String) {
        // 手動停止した場合などはここに来る
        val text = parseVoskResult(hypothesis)

        // もし「聞き取り中」のままここに来た（＝自動停止やタイムアウト）場合のみ送信
        // manualStopAndSend() を呼んだ場合はすでに WAITING になっているのでここは無視される
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