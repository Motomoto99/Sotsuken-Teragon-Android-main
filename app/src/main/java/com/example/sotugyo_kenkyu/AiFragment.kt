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
import com.google.firebase.ai.FirebaseAI
import kotlinx.coroutines.launch

class AiFragment : Fragment() {

    // --- UI ---
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton

    // --- Chat 表示用 ---
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    // FirebaeAI のモデル
    private val generativeModel by lazy {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app)
            .generativeModel("gemini-2.5-flash") // プロジェクト設定に合わせて変更
    }

    // 会話コンテキストを持つ chat。新しいチャット開始時に作り直すので var
    private var chat = generativeModel.startChat()

    // Firestore から取得した systemPrompt を保持
    private var systemPrompt: String? = null

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

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        recyclerView.layoutManager = layoutManager

        chatAdapter = ChatAdapter(messages)
        recyclerView.adapter = chatAdapter

        // ▼ ここで最初にプロンプトを1回だけ取得しておく
        viewLifecycleOwner.lifecycleScope.launch {
            systemPrompt = PromptRepository.getSystemPrompt()
            // 最初のチャットセッション用にプロンプトを流し込む
            applySystemPromptToCurrentChat()
        }

        buttonSend.setOnClickListener {
            sendMessage()
        }

        // 「新しいチャット」ボタンを付けた場合は、そこから startNewChat() を呼ぶ
        // 例: view.findViewById<Button>(R.id.buttonNewChat).setOnClickListener { startNewChat() }
    }

    /**
     * 現在の chat インスタンスに systemPrompt を送る。
     * ユーザー側の画面には表示しない。
     */
    private fun applySystemPromptToCurrentChat() {
        val prompt = systemPrompt ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ★ ここで落ちている
                chat.sendMessage(prompt)
            } catch (e: Exception) {
                // 画面にも内容を出す（あとでコピーして教えてほしい）
                val msg = "初期プロンプト送信エラー: ${e.localizedMessage ?: e.toString()}"
                addMessage(msg, isUser = false)

                Toast.makeText(requireContext(), "プロンプト適用時にエラーが発生しました", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }


    /**
     * 新しいチャットを開始する。
     * - メッセージ履歴を消す
     * - chat を作り直す
     * - systemPrompt を再度送る
     */
    private fun startNewChat() {
        messages.clear()
        chatAdapter.notifyDataSetChanged()

        chat = generativeModel.startChat()
        applySystemPromptToCurrentChat()
    }

    /**
     * 送信ボタンタップ時の処理
     */
    private fun sendMessage() {
        val userMessageText = editTextMessage.text.toString().trim()
        if (userMessageText.isEmpty()) return

        addMessage(userMessageText, isUser = true)
        editTextMessage.text.clear()
        scrollToBottom()

        buttonSend.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // ここでは userMessageText だけを送る。
                // 役割・スタイルは最初に流した systemPrompt でチューニングされている状態。
                val response = chat.sendMessage(userMessageText)
                val aiResponseText = response.text ?: "回答がありませんでした"
                addMessage(aiResponseText, isUser = false)
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

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
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
