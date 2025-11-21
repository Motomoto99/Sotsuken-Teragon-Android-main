package com.example.sotugyo_kenkyu

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.FirebaseAI
// import com.google.firebase.ai.content // ←この行は削除してください
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
        // Firestore などから長文プロンプトを取得
        val systemPrompt = PromptRepository.getSystemPrompt()

        // ユーザーのアレルギー情報を取得してプロンプトに組み込む
        val allergyInfo = getUserAllergiesPrompt()

        return """
$systemPrompt

$allergyInfo

以上の方針に従って、今後のチャットに回答してください。
""".trimIndent()
    }

    // Firestoreからアレルギー情報を取得し、AIへの指示文を作成する
    private suspend fun getUserAllergiesPrompt(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return ""

        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .await()

            // AllergySettingsActivityで保存したフィールド名 "allergies" を取得
            val allergies = snapshot.get("allergies") as? List<String> ?: emptyList()

            if (allergies.isNotEmpty()) {
                """
                【重要：ユーザーのアレルギー情報】
                このユーザーは以下の食材に対してアレルギーを持っています。
                レシピの提案や食材の確認を行う際は、以下の食材が含まれないように細心の注意を払ってください。
                代替食材の提案が必要な場合は、アレルギー食材を含まないものを提案してください。
                
                対象アレルギー食材: ${allergies.joinToString("、")}
                """.trimIndent()
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "" // エラー時はアレルギー情報なしとして続行
        }
    }

    // メッセージ内容からタイトルを自動生成する関数
    suspend fun generateTitleFromMessage(message: String): String {
        return try {
            // タイトル生成用のプロンプト
            val prompt = """
                以下のユーザーのメッセージを、履歴として一覧表示した際に分かりやすい
                20文字以内の短いタイトルに要約してください。
                
                ・「〜について教えて」などの語尾はカットする
                ・鍵括弧「」などの記号は不要
                ・料理名や食材名が含まれる場合はそれを優先する
                
                メッセージ:
                $message
            """.trimIndent()

            // 履歴を持たない単発のリクエストとして送信
            val response = generativeModel.generateContent(prompt)

            // 結果を返す（失敗したら元のメッセージを適当に短くして返す）
            response.text?.trim() ?: message.take(20)
        } catch (e: Exception) {
            e.printStackTrace()
            // エラー時は元のメッセージをそのまま使う
            message.take(20)
        }
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