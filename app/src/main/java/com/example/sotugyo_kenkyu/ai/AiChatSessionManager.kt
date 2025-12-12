package com.example.sotugyo_kenkyu.ai

import com.example.sotugyo_kenkyu.recipe.Recipe
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.tasks.await

object AiChatSessionManager {

    private const val MODEL_NAME = "gemini-2.5-flash"

    private val generativeModel by lazy {
        val app = FirebaseApp.getInstance()
        FirebaseAI.getInstance(app).generativeModel(MODEL_NAME)
    }

    var currentChatId: String? = null
        private set

    var firstUserMessageSent: Boolean = false
        private set

    var chat: Chat? = null
        private set

    var isArrangeMode: Boolean = false
        private set

    var pendingContext: String? = null
    var pendingArrangeRecipe: Recipe? = null

    var isCompleted: Boolean = false
        private set

    // ---------------- プロンプト関連のメソッドは削除し、Repositoryへ委譲 ----------------
    suspend fun generateTitleFromMessage(message: String): String {
        return try {
            val prompt = "以下のメッセージを20文字以内のタイトルに要約せよ: $message"
            val response = generativeModel.generateContent(prompt)
            response.text?.trim() ?: message.take(20)
        } catch (e: Exception) {
            message.take(20)
        }
    }
    // ★追加: レシピ名を短く要約する (修正3)
    private suspend fun summarizeRecipeTitle(originalTitle: String): String {
        return try {
            val prompt = """
                以下の料理名を、元の料理が何かわかるように10文字以内で要約してください。
                語尾や装飾は不要です。名詞だけで答えてください。
                
                例：
                入力：子供が大好き！甘辛たれのふんわり鶏つくね
                出力：甘辛鶏つくね
                
                入力：$originalTitle
            """.trimIndent()

            // チャット履歴を使わない単発のリクエスト
            val response = generativeModel.generateContent(prompt)
            response.text?.trim() ?: originalTitle.take(10)
        } catch (e: Exception) {
            originalTitle.take(10)
        }
    }

    // ---------------- セッション操作 ----------------

    suspend fun ensureSessionInitialized(): Chat {
        val existing = chat
        if (existing != null) return existing
        return startNewSession()
    }

    suspend fun startNewSession(): Chat {
        // ★修正: Repositoryからプロンプトを取得
        val initialPrompt = PromptRepository.getInitialPrompt()

        val tempChat = generativeModel.startChat()
        tempChat.sendMessage(initialPrompt)

        chat = generativeModel.startChat(history = tempChat.history)
        currentChatId = null
        firstUserMessageSent = false
        isArrangeMode = false
        isCompleted = false // ★リセット
        return chat!!
    }

    suspend fun startArrangeSession(recipe: Recipe): String {

        // 1. タイトルの要約を作成
        val shortTitle = summarizeRecipeTitle(recipe.recipeTitle)
        val chatTitle = "$shortTitle アレンジ"

        // 2. DB作成時にタイトルを指定 (ここでエラーが消えます)
        val newChatId = ChatRepository.createArrangeChatSession(recipe, chatTitle)

        currentChatId = newChatId

        // プロンプトの取得と送信
        val arrangePrompt = PromptRepository.getArrangePrompt(recipe)
        val tempChat = generativeModel.startChat()
        val response = tempChat.sendMessage(arrangePrompt)
        val firstAiMessage = response.text ?: "アレンジの相談を始めましょう！"

        chat = generativeModel.startChat(history = tempChat.history)
        isArrangeMode = true
        isCompleted = false
        firstUserMessageSent = true

        ChatRepository.addMessage(newChatId, "assistant", firstAiMessage)

        return firstAiMessage
    }

    suspend fun startSessionWithHistory(messages: List<ChatMessage>) {
        val systemPrompt = PromptRepository.getSystemPrompt()

        val history = mutableListOf<Content>()
        history.add(content(role = "user") { text(systemPrompt) })
        history.add(content(role = "model") { text("はい、承知いたしました。") })
        messages.forEach { msg ->
            val role = if (msg.isUser) "user" else "model"
            history.add(content(role = role) { text(msg.message) })
        }
        chat = generativeModel.startChat(history = history)
        firstUserMessageSent = true
        isArrangeMode = false
    }
    suspend fun generateArrangeSummary(): Pair<String, String> {
        val currentChat = chat ?: return Pair("", "")

        // プロンプトは変更なし
        val prompt = """
            これまでの会話の内容を元に、決定した「アレンジ料理名」と、
            アレンジのポイントや変更点をまとめた「短いメモ（150文字以内）」を作成してください。
            アレルギーでレシピから抜いた食材がある場合は抜いた理由をちゃんと書いてください。
            
            以下のフォーマットで出力してください。余計な挨拶は不要です。
            
            料理名：{ここに料理名}
            メモ：{ここにメモの内容}
        """.trimIndent()

        return try {
            val response = currentChat.sendMessage(prompt)
            val text = response.text ?: ""

            // ★修正: 解析ロジックを強化（太字やスペースに対応）
            var name = ""
            var memo = ""

            text.lines().forEach { line ->
                // マークダウンの記号(*など)や空白を除去して判定しやすくする
                val cleanLine = line.replace("*", "").trim()

                if (cleanLine.startsWith("料理名") && (cleanLine.contains(":") || cleanLine.contains("："))) {
                    // "：" または ":" の後ろを取得
                    name = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
                else if (cleanLine.startsWith("メモ") && (cleanLine.contains(":") || cleanLine.contains("："))) {
                    memo = cleanLine.substringAfter("：").substringAfter(":").trim()
                }
            }

            // うまく取れなかった場合の保険（テキスト全体をメモにするなど）
            if (name.isEmpty()) name = "アレンジ料理"
            if (memo.isEmpty()) memo = text.take(100) // 最初の100文字を入れる

            Pair(name, memo)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("アレンジ料理", "AI生成エラー")
        }
    }
    // ★追加: 外部からチャットの状態をセットする
    fun setChatConfig(isArrange: Boolean, completed: Boolean) {
        isArrangeMode = isArrange
        isCompleted = completed
    }

    // ★追加: 現在のチャットを完了済みにする
    fun markAsCompleted() {
        isCompleted = true
    }

    fun resetSession() {
        chat = null
        currentChatId = null
        firstUserMessageSent = false
        isArrangeMode = false
        isCompleted = false // ★リセット
    }

    fun attachExistingChat(chatId: String) {
        currentChatId = chatId
        firstUserMessageSent = true
    }

    fun setChatId(chatId: String) {
        currentChatId = chatId
    }

    fun markFirstUserMessageSent() {
        firstUserMessageSent = true
    }
}