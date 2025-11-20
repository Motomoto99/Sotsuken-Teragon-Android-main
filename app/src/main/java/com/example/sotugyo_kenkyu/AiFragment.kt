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
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.FirebaseAI
import kotlinx.coroutines.launch
import kotlin.text.replace

class AiFragment : Fragment() {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton

    private lateinit var buttonChatList: ImageButton


    // --- Chat 表示用 ---
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    // ★使うモデル名（1.5系は廃止なので2.x系）
    private val MODEL_NAME = "gemini-2.5-flash"

    // Firebase AI モデル
    private val generativeModel by lazy {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app).generativeModel(MODEL_NAME)
    }

    // 実際に会話に使う Chat インスタンス
    private var chat: Chat? = null

    // Firestore 上のチャットID（users/{uid}/chats/{chatId}）
    private var currentChatId: String? = null

    // 最初のユーザーメッセージを送ったかどうか（タイトル自動設定用）
    private var firstUserMessageSent = false

    // -------------------- Lifecycle --------------------

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
        buttonChatList.setOnClickListener {
            openChatList()
        }


        recyclerView.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // 引数に chatId が渡されている場合 → 過去チャットを開く
        val argChatId = arguments?.getString("chatId")

        // 初期化が終わるまで送信ボタンを無効
        buttonSend.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (argChatId == null) {
                    // 新しいチャット
                    startNewChat()
                } else {
                    // 既存チャットを読み込み
                    loadExistingChat(argChatId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "チャット初期化でエラーが発生しました", Toast.LENGTH_SHORT).show()
            } finally {
                buttonSend.isEnabled = true
            }
        }

        buttonSend.setOnClickListener {
            sendMessage()
        }
    }

    // -------------------- プロンプト関連 --------------------

    /**
     * Firestore から取得した長文プロンプトを組み立てる
     */
    private suspend fun buildInitialPrompt(): String {
        val systemPrompt = PromptRepository.getSystemPrompt()
        return """
$systemPrompt

以上の方針に従って、今後のチャットに回答してください。
""".trimIndent()
    }

    /**
     * 一時チャットに初期プロンプトだけ送って、
     * その history を引き継いだ Chat を返す。
     * → busy エラーを避ける構成。
     */
    private suspend fun createChatWithPrompt(): Chat {
        val initialPrompt = buildInitialPrompt()

        // 一時チャット
        val tempChat = generativeModel.startChat()
        tempChat.sendMessage(initialPrompt)

        // history を引き継いだ本番チャット
        return generativeModel.startChat(
            history = tempChat.history
        )
    }

    // -------------------- 新規チャット / 既存チャット --------------------

    /**
     * 新しいチャットを開始する。
     * - Firestore に chat セッションを作成
     * - ローカルのメッセージをクリア
     * - 初期プロンプト適用済み Chat を生成
     */
    private suspend fun startNewChat() {
        // Firestore に新しいチャットドキュメント作成
        currentChatId = ChatRepository.createNewChatSession()
        firstUserMessageSent = false

        messages.clear()
        chatAdapter.notifyDataSetChanged()

        chat = createChatWithPrompt()
    }

    /**
     * 既存チャットを読み込む（過去チャット閲覧＋続き）
     */
    private suspend fun loadExistingChat(chatId: String) {
        currentChatId = chatId
        firstUserMessageSent = true // 既存チャットなのでタイトルは既にある前提

        // Firestore からメッセージ履歴取得 → 画面に表示
        val list = ChatRepository.loadMessages(chatId)
        messages.clear()
        messages.addAll(list)
        chatAdapter.notifyDataSetChanged()
        scrollToBottom()

        // AI 側のコンテキストも復元（簡易版：全文を順に送り直す）
        val tempChat = generativeModel.startChat()
        for (m in list) {
            tempChat.sendMessage(m.message)
        }
        chat = tempChat
    }

    // -------------------- 送信処理 --------------------

    /**
     * 送信ボタンタップ時
     */
    private fun sendMessage() {
        val currentChat = chat
        val chatId = currentChatId

        if (currentChat == null || chatId == null) {
            Toast.makeText(requireContext(), "チャットの準備中です…", Toast.LENGTH_SHORT).show()
            return
        }

        val userMessageText = editTextMessage.text.toString().trim()
        if (userMessageText.isEmpty()) return

        // 画面にユーザーメッセージを追加
        addMessage(userMessageText, isUser = true)
        editTextMessage.text.clear()
        scrollToBottom()

        // Firestore にユーザーメッセージ保存 ＋ タイトル自動設定
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                ChatRepository.addMessage(chatId, role = "user", text = userMessageText)

                if (!firstUserMessageSent) {
                    firstUserMessageSent = true
                    val title = makeAutoTitleFromFirstMessage(userMessageText)
                    ChatRepository.updateChatTitle(chatId, title)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 連打防止
        buttonSend.isEnabled = false

        // AI への問い合わせ
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = currentChat.sendMessage(userMessageText)
                val aiResponseText = response.text ?: "回答がありませんでした"

                // 画面に AI メッセージを追加
                addMessage(aiResponseText, isUser = false)

                // Firestore に AI メッセージ保存
                val id = currentChatId
                if (id != null) {
                    ChatRepository.addMessage(id, role = "assistant", text = aiResponseText)
                }
            } catch (e: Exception) {
                val errorText = "エラーが発生しました: ${e.localizedMessage ?: e.toString()}"
                addMessage(errorText, isUser = false)
                Toast.makeText(requireContext(), "チャットでエラーが発生しました", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            } finally {
                buttonSend.isEnabled = true
                scrollToBottom()
            }
        }
    }

    // -------------------- UIヘルパー --------------------

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
    private fun openChatList() {
        val fragment = ChatListFragment()

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // ← ここはあなたのActivity側のコンテナIDに合わせて変更
            .addToBackStack(null)
            .commit()
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

