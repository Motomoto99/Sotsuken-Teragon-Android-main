package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AiFragment : Fragment() {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonChatList: ImageButton
    private lateinit var buttonNewChat: ImageButton

    // --- 表示用メッセージ ---
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    /**
     * メッセージ数に応じて「新しいチャット」ボタンの状態を更新
     * - メッセージ 0 件: 押せない
     * - メッセージ 1 件以上: 押せる
     */
    private fun updateNewChatButtonState() {
        // メッセージが1件もないときは「新しいチャット」を押せないようにする
        if (::buttonNewChat.isInitialized) {
            buttonNewChat.isEnabled = messages.isNotEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        val header = view.findViewById<View>(R.id.header)

        // ★ WindowInsets設定 (ヘッダーにパディング適用)
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // XMLのpaddingVerticalを16dpとして扱う
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()

            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // ★ 初期状態（メッセージ0件）では新規チャットボタンは無効
        updateNewChatButtonState()

        // 初期状態では送信不可
        buttonSend.isEnabled = false

        val argChatId = arguments?.getString("chatId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (argChatId != null) {
                    // 過去チャット
                    loadExistingChat(argChatId)
                    buttonSend.isEnabled = true
                } else {
                    // 新規 or ログイン直後
                    AiChatSessionManager.ensureSessionInitialized()
                    buttonSend.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "チャット初期化エラー", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSend.setOnClickListener { sendMessage() }
        buttonChatList.setOnClickListener { openChatList() }

        buttonNewChat.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    buttonSend.isEnabled = false

                    // AI セッションだけリセット（DBにはまだ書かない）
                    AiChatSessionManager.startNewSession()

                    // 画面上のメッセージをクリア
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                    scrollToBottom()

                    // ★ メッセージ0件になったので新規チャットボタンを無効化
                    updateNewChatButtonState()

                    buttonSend.isEnabled = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "新規チャット作成失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun loadExistingChat(chatId: String) {
        val list = ChatRepository.loadMessages(chatId)
        messages.clear()
        messages.addAll(list)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()

        val sessionChat = AiChatSessionManager.ensureSessionInitialized()
        for (m in list) {
            sessionChat.sendMessage(m.message)
        }
        AiChatSessionManager.attachExistingChat(chatId)

        // ★ 過去チャットなので基本的にメッセージあり → 新規チャットボタン有効
        updateNewChatButtonState()
    }

    private fun sendMessage() {
        val sessionChat = AiChatSessionManager.chat
        if (sessionChat == null) {
            Toast.makeText(requireContext(), "準備中です…", Toast.LENGTH_SHORT).show()
            return
        }

        val userMessageText = editTextMessage.text.toString().trim()
        if (userMessageText.isEmpty()) return

        addMessage(userMessageText, isUser = true)
        editTextMessage.text.clear()
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
                    val title = makeAutoTitleFromFirstMessage(userMessageText)
                    ChatRepository.updateChatTitle(chatId, title)
                }

                val response = sessionChat.sendMessage(userMessageText)
                val aiResponseText = response.text ?: "回答がありません"

                addMessage(aiResponseText, isUser = false)
                ChatRepository.addMessage(chatId, role = "assistant", text = aiResponseText)

            } catch (e: Exception) {
                val errorText = "エラー: ${e.localizedMessage}"
                addMessage(errorText, isUser = false)
                e.printStackTrace()
            } finally {
                buttonSend.isEnabled = true
                scrollToBottom()
            }
        }
    }

    private fun openChatList() {
        val fragment = ChatListFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(message = text, isUser = isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        // ★ メッセージが1件以上になったので新規チャットボタンを有効化
        updateNewChatButtonState()
    }

    private fun scrollToBottom() {
        if (messages.isNotEmpty()) {
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
    }
}

fun makeAutoTitleFromFirstMessage(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return "新しいチャット"
    val maxLen = 20
    return if (trimmed.length <= maxLen) trimmed else trimmed.take(maxLen) + "…"
}
