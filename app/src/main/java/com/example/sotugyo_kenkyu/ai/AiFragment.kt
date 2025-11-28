package com.example.sotugyo_kenkyu.ai

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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

class AiFragment : Fragment() {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var buttonChatList: ImageButton
    private lateinit var buttonNewChat: ImageButton

    // ローディング画面
    private lateinit var layoutAiLoading: View

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

        // ローディングViewの取得
        layoutAiLoading = view.findViewById(R.id.layoutAiLoading)

        //AIチャットで入力欄を動的に移動させるときに追加
        val layoutInput = view.findViewById<View>(R.id.layoutInput)

        // ステータスバー分をヘッダーに足す
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val isImeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val bottomNav = requireActivity().findViewById<View>(R.id.bottomNavigation)
            val bottomNavHeight = bottomNav?.height ?: 0

            val targetBottomMargin = if (isImeVisible) {
                (imeHeight - bottomNavHeight).coerceAtLeast(0)
            } else {
                0
            }

            layoutInput.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.bottomMargin = targetBottomMargin
            }

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
                // 処理開始前にローディングを表示して画面を隠す
                setLoading(true)

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
                // 本当に失敗したときだけログに出す
                e.printStackTrace()
            } finally {
                // 準備完了（またはエラー）したらローディングを消す
                setLoading(false)

                buttonSend.isEnabled = true
                updateNewChatButtonState()

                // ★追加: 記録画面などから飛んできた場合、預かっていたメッセージを自動送信
                checkPendingContextAndSend()
            }
        }

        // 送信
        buttonSend.setOnClickListener {
            hideKeyboard()
            sendMessage() // 引数なし＝入力欄の内容を送信
        }

        // チャット一覧へ
        buttonChatList.setOnClickListener { openChatList() }

        // 新しいチャット開始
        buttonNewChat.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    setLoading(true)
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
                    setLoading(false)
                    buttonSend.isEnabled = true
                }
            }
        }
    }

    /**
     * ★追加: タブ切り替えなどで「非表示→表示」になったときに呼ばれる
     * RecordFragmentから遷移してきた場合、onViewCreatedではなくここが呼ばれることがあるため
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // 表示されたタイミングで「預かりメッセージ」があるかチェック
            checkPendingContextAndSend()
        }
    }

    /** ★追加: AiChatSessionManagerに保留中のメッセージがあれば送信する */
    private fun checkPendingContextAndSend() {
        val pendingMsg = AiChatSessionManager.pendingContext
        // チャットセッション準備完了＆メッセージがある場合のみ実行
        if (pendingMsg != null && AiChatSessionManager.chat != null) {
            // 二重送信防止のためクリア
            AiChatSessionManager.pendingContext = null
            // 自動送信を実行
            sendMessage(manualText = pendingMsg)
        }
    }

    // ローディングの表示切り替え関数
    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            layoutAiLoading.visibility = View.VISIBLE
            editTextMessage.isEnabled = false
        } else {
            layoutAiLoading.visibility = View.GONE
            editTextMessage.isEnabled = true
        }
    }

    /** Firestoreから履歴を読み込み、AIセッションにも流し込む */
    private suspend fun loadExistingChat(chatId: String) {
        val list = ChatRepository.loadMessages(chatId)

        messages.clear()
        messages.addAll(list)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()

        AiChatSessionManager.startSessionWithHistory(list)
    }

    /**
     * メッセージ送信処理
     * @param manualText 自動送信などでテキストを直接指定する場合に使用。nullの場合は入力欄を使用。
     */
    private fun sendMessage(manualText: String? = null) {
        val sessionChat = AiChatSessionManager.chat
        if (sessionChat == null) {
            Toast.makeText(requireContext(), "準備中です…", Toast.LENGTH_SHORT).show()
            return
        }

        // 引数があればそれを、なければ入力欄を使用
        val userMessageText = manualText ?: editTextMessage.text.toString().trim()
        if (userMessageText.isEmpty()) return

        addMessage(userMessageText, isUser = true)

        // 入力欄からの送信だった場合のみクリアする（自動送信の場合は消さない）
        if (manualText == null) {
            editTextMessage.text.clear()
        }
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
            } catch (ce: CancellationException) {
                // キャンセル時は何もしない
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

    // 送信後にキーボードを閉じる
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = activity?.currentFocus ?: view
        currentFocus?.let {
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        editTextMessage.clearFocus()
    }
}