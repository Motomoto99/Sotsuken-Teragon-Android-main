package com.example.sotugyo_kenkyu

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.FirebaseAI

/**
 * ログイン中の AI チャットセッションをアプリ全体で 1 つ管理するためのシングルトン。
 * - プロンプト読み込み済みの Chat を保持
 * - DB(Firestore) には一切触らない（空チャット問題と分離する）
 */
object AiChatSessionManager {

    private const val MODEL_NAME = "gemini-2.5-flash"

    private val generativeModel by lazy {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app).generativeModel(MODEL_NAME)
    }

    /** Firestore 上のチャットID（存在する場合のみ） */
    var currentChatId: String? = null
        private set

    /** 1通目のユーザーメッセージを送ったかどうか（タイトル生成用） */
    var firstUserMessageSent: Boolean = false
        private set

    /** FirebaseAI の Chat セッション（プロンプト読み込み済み） */
    var chat: Chat? = null
        private set

    // ---------------- プロンプト関連 ----------------

    private suspend fun buildInitialPrompt(): String {
        // Firestore などから長文プロンプトを取得（あなたが作った PromptRepository）
        val systemPrompt = PromptRepository.getSystemPrompt()
        return """
$systemPrompt

以上の方針に従って、今後のチャットに回答してください。
""".trimIndent()
    }

    private suspend fun createChatWithPrompt(): Chat {
        val initialPrompt = buildInitialPrompt()

        // 一時チャットにプロンプトを送る（ユーザーには見せない）
        val tempChat = generativeModel.startChat()
        tempChat.sendMessage(initialPrompt)

        // history を引き継いだ本番チャット
        return generativeModel.startChat(history = tempChat.history)
    }

    // ---------------- セッション操作 ----------------

    /**
     * まだ Chat がなければ新規作成して返す。
     * すでにあればそのまま返す。
     * - ログイン直後や初めて AiFragment を開いたときに呼ぶ想定。
     */
    suspend fun ensureSessionInitialized(): Chat {
        val existing = chat
        if (existing != null) return existing

        val newChat = createChatWithPrompt()
        chat = newChat
        currentChatId = null
        firstUserMessageSent = false
        return newChat
    }

    /**
     * 「新しいチャット」ボタンで明示的に新セッションを開始したいとき用。
     * - AI 側のコンテキストだけ切り替える
     * - DB 上の chatId はまだ作らない（空チャットを残さない）
     */
    suspend fun startNewSession(): Chat {
        val newChat = createChatWithPrompt()
        chat = newChat
        currentChatId = null
        firstUserMessageSent = false
        return newChat
    }

    /**
     * 過去チャットを開くときに、対象の chatId を紐付ける。
     * - DB には既にあるチャットの続き、という前提
     */
    fun attachExistingChat(chatId: String) {
        currentChatId = chatId
        firstUserMessageSent = true
    }

    /**
     * 最初のユーザーメッセージ時に、DBで生成した chatId を紐付ける。
     */
    fun setChatId(chatId: String) {
        currentChatId = chatId
    }

    /**
     * 1通目のユーザーメッセージを送ったフラグを立てる。
     */
    fun markFirstUserMessageSent() {
        firstUserMessageSent = true
    }
}
