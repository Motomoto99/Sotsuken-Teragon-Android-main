package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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

        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // 初期状態では送信不可（セッション準備が終わったら有効にする）
        buttonSend.isEnabled = false

        val argChatId = arguments?.getString("chatId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (argChatId != null) {
                    // ★ 過去チャットの続き
                    loadExistingChat(argChatId)
                    buttonSend.isEnabled = true
                } else {
                    // ★ 新規 or ログイン直後
                    // ログイン時に ensureSessionInitialized() を呼んでおけばここは通らなくてもOKだけど、
                    // 念のためここでも呼んでおくと安心。
                    AiChatSessionManager.ensureSessionInitialized()
                    buttonSend.isEnabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "チャット初期化でエラーが発生しました", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSend.setOnClickListener { sendMessage() }
        buttonChatList.setOnClickListener { openChatList() }

        buttonNewChat.setOnClickListener {
            // 「新しいチャット」押下 → AI セッションだけ切り替え、DB はまだ作らない
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    buttonSend.isEnabled = false
                    AiChatSessionManager.startNewSession()
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                    scrollToBottom()
                    buttonSend.isEnabled = true
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "新しいチャットの準備に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ---------------- 過去チャットの続き ----------------

    private suspend fun loadExistingChat(chatId: String) {
        // 1. Firestore からメッセージ履歴を取得して表示
        val list = ChatRepository.loadMessages(chatId)
        messages.clear()
        messages.addAll(list)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()

        // 2. AI側のコンテキストを再構築
        //   （簡易版：過去の発言を順番に全部送り直す）
        val sessionChat = AiChatSessionManager.ensureSessionInitialized()
        for (m in list) {
            sessionChat.sendMessage(m.message)
        }

        // 3. 「このセッションは chatId に紐付いている」とマネージャに伝える
        AiChatSessionManager.attachExistingChat(chatId)
    }

    // ---------------- 送信処理 ----------------

    private fun sendMessage() {
        val sessionChat = AiChatSessionManager.chat
        if (sessionChat == null) {
            Toast.makeText(requireContext(), "チャットの準備中です…", Toast.LENGTH_SHORT).show()
            return
        }

        val userMessageText = editTextMessage.text.toString().trim()
        if (userMessageText.isEmpty()) return

        // 画面にユーザーメッセージを追加
        addMessage(userMessageText, isUser = true)
        editTextMessage.text.clear()
        scrollToBottom()

        buttonSend.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ★ DB 上の chatId がまだ無ければ、このタイミングで初めて作成
                var chatId = AiChatSessionManager.currentChatId
                if (chatId == null) {
                    chatId = ChatRepository.createNewChatSession()
                    AiChatSessionManager.setChatId(chatId)
                }

                // ユーザーメッセージを Firestore に保存
                ChatRepository.addMessage(chatId, role = "user", text = userMessageText)

                // 1通目ならタイトルを自動生成
                if (!AiChatSessionManager.firstUserMessageSent) {
                    AiChatSessionManager.markFirstUserMessageSent()
                    val title = makeAutoTitleFromFirstMessage(userMessageText)
                    ChatRepository.updateChatTitle(chatId, title)
                }

                // AI 応答
                val response = sessionChat.sendMessage(userMessageText)
                val aiResponseText = response.text ?: "回答がありませんでした"

                // UI に表示
                addMessage(aiResponseText, isUser = false)

                // Firestore に AI メッセージ保存
                ChatRepository.addMessage(chatId, role = "assistant", text = aiResponseText)

            } catch (e: Exception) {
                val errorText = "エラーが発生しました: ${e.localizedMessage ?: e.toString()}"
                addMessage(errorText, isUser = false)
                e.printStackTrace()
            } finally {
                buttonSend.isEnabled = true
                scrollToBottom()
            }
        }
    }

    // ---------------- 画面遷移 ----------------

    private fun openChatList() {
        val fragment = ChatListFragment()
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // ← あなたのコンテナIDに合わせて変更済みだと思う
            .addToBackStack(null)
            .commit()
    }

    // ---------------- UIヘルパー ----------------

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(message = text, isUser = isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
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

/**
 * 1通目のユーザーメッセージからチャットタイトルを自動生成
 */
fun makeAutoTitleFromFirstMessage(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return "新しいチャット"

    val maxLen = 20
    return if (trimmed.length <= maxLen) trimmed else trimmed.take(maxLen) + "…"
}
