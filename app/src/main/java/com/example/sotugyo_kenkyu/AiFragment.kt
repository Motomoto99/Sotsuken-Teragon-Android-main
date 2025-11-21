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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AiFragment : Fragment() {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonChatList: ImageButton
    private lateinit var buttonNewChat: ImageButton

    // --- 表示用メッセージ ---
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    /** メッセージ数に応じて「新しいチャット」ボタンの活性/非活性を切り替える */
    private fun updateNewChatButtonState() {
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

        // ステータスバー分をヘッダーに足す
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // 初期状態
        buttonSend.isEnabled = false
        updateNewChatButtonState()

        // 「今どのチャットを開いているか」は SessionManager に一元管理させる
        val currentId = AiChatSessionManager.currentChatId

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (currentId == null) {
                    // 新規チャット状態（まだ履歴なし）
                    AiChatSessionManager.ensureSessionInitialized()
                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                } else {
                    // 現在のチャットIDに紐づく履歴を表示・復元
                    loadExistingChat(currentId)
                }
            } catch (ce: CancellationException) {
                // 画面から離れたときなどの正常なキャンセル → 何もしない
            } catch (e: Exception) {
                // 本当に失敗したときだけログに出す（ユーザーには通知しない）
                e.printStackTrace()
            } finally {
                buttonSend.isEnabled = true
                updateNewChatButtonState()
            }
        }

        // 送信
        buttonSend.setOnClickListener { sendMessage() }

        // チャット一覧へ
        buttonChatList.setOnClickListener { openChatList() }

        // 新しいチャット開始
        buttonNewChat.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    buttonSend.isEnabled = false
                    AiChatSessionManager.startNewSession()

                    messages.clear()
                    chatAdapter.notifyDataSetChanged()
                    scrollToBottom()
                    updateNewChatButtonState()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "新規チャット作成失敗", Toast.LENGTH_SHORT).show()
                } finally {
                    buttonSend.isEnabled = true
                }
            }
        }
    }

    /** Firestoreから履歴を読み込み、AIセッションにも流し込む */
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
    }

    /** メッセージ送信処理 */
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

                // ★変更: 初回メッセージの場合、AIにタイトル生成を依頼する
                if (!AiChatSessionManager.firstUserMessageSent) {
                    AiChatSessionManager.markFirstUserMessageSent()

                    // 並行してタイトル生成を実行（チャットの応答を待たずに裏で動かす）
                    launch {
                        val newTitle = AiChatSessionManager.generateTitleFromMessage(userMessageText)
                        ChatRepository.updateChatTitle(chatId, newTitle)
                    }
                }

                val response = sessionChat.sendMessage(userMessageText)
                val aiResponseText = response.text ?: "回答がありません"

                addMessage(aiResponseText, isUser = false)
                ChatRepository.addMessage(chatId, role = "assistant", text = aiResponseText)
            } catch (ce: CancellationException) {
                // 送信中に画面を離れたなど → 何もしない
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
        if (messages.isNotEmpty()) {
            recyclerView.scrollToPosition(messages.size - 1)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView.adapter = null
    }
}

// ★削除: 以前の makeAutoTitleFromFirstMessage 関数は不要になったため削除しました