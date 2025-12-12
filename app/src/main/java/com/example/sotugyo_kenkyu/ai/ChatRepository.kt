package com.example.sotugyo_kenkyu.ai

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import com.example.sotugyo_kenkyu.recipe.Recipe // ★追加

object ChatRepository {

    private val db get() = Firebase.firestore

    private fun uidOrThrow(): String =
        FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("ログインユーザーがいません")

    /**
     * 新しいチャットセッションを作成し、chatId を返す
     * Firestore: users/{uid}/chats/{chatId}
     */
    suspend fun createNewChatSession(): String {
        val uid = uidOrThrow()
        val docRef = db.collection("users")
            .document(uid)
            .collection("chats")
            .document() // 自動ID

        val now = FieldValue.serverTimestamp()

        docRef.set(
            mapOf(
                "title" to "",
                "createdAt" to now,
                "updatedAt" to now,
                "isArrangeMode" to false // ★明示的にfalse
            )
        ).await()

        return docRef.id
    }
    /**
     * ★追加: アレンジモード用のチャットセッションを作成
     * レシピ情報も一緒に保存します。
     */
    suspend fun createArrangeChatSession(recipe: Recipe,title: String): String {
        val uid = uidOrThrow()
        val docRef = db.collection("users")
            .document(uid)
            .collection("chats")
            .document()

        val now = FieldValue.serverTimestamp()

        // タイトルを「料理名 アレンジ」で初期化
        val initialTitle = "${recipe.recipeTitle} アレンジ"

        // 保存するレシピデータをMapに変換
        // (Recipeクラスの全フィールドではなく、必要なものだけ保存するのが安全です)
        val recipeMap = mapOf(
            "id" to recipe.id,
            "recipeTitle" to recipe.recipeTitle,
            "foodImageUrl" to recipe.foodImageUrl,
            "recipeMaterial" to (recipe.recipeMaterial ?: emptyList<String>()),
            "servingAmounts" to recipe.servingAmounts,
            "recipeSteps" to (recipe.recipeSteps ?: emptyList<String>())
        )

        docRef.set(
            mapOf(
                "title" to title,
                "createdAt" to now,
                "updatedAt" to now,
                "isArrangeMode" to true,
                "isCompleted" to false, // ★完了フラグ初期値
                "sourceRecipe" to recipeMap // ★レシピ情報を埋め込む
            )
        ).await()

        return docRef.id
    }
    /**
     * ★追加: チャットを「完了済み」にする
     */
    suspend fun completeArrangeChat(chatId: String) {
        val uid = uidOrThrow()
        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .update("isCompleted", true)
            .await()
    }
    /**
     * ★追加: チャットの設定情報（メタデータ）を取得する
     * 履歴を開いたときに、ボタンの状態を復元するために使います。
     * 戻り値: Pair<isArrangeMode, isCompleted>
     */
    suspend fun getChatConfig(chatId: String): Pair<Boolean, Boolean> {
        return try {
            val uid = uidOrThrow()
            val snap = db.collection("users")
                .document(uid)
                .collection("chats")
                .document(chatId)
                .get()
                .await()

            val isArrange = snap.getBoolean("isArrangeMode") ?: false
            val isCompleted = snap.getBoolean("isCompleted") ?: false
            Pair(isArrange, isCompleted)
        } catch (e: Exception) {
            Pair(false, false)
        }
    }

    /**
     * メッセージを追加
     * Firestore: users/{uid}/chats/{chatId}/messages/{messageId}
     */
    suspend fun addMessage(chatId: String, role: String, text: String) {
        val uid = uidOrThrow()
        val now = FieldValue.serverTimestamp()

        val chatRef = db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)

        val msgRef = chatRef
            .collection("messages")
            .document()

        db.runBatch { batch ->
            batch.set(
                msgRef,
                mapOf(
                    "role" to role,
                    "text" to text,
                    "createdAt" to now
                )
            )
            batch.update(chatRef, "updatedAt", now)
        }.await()
    }

    /**
     * チャットタイトルを更新
     */
    suspend fun updateChatTitle(chatId: String, title: String) {
        val uid = uidOrThrow()
        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .update("title", title)
            .await()
    }

    /**
     * 過去チャット一覧を取得（タイトル＋日時）
     * ★変更: お気に入り以外のチャットのみ、最新30件を保持し、それより古いものは削除する。
     * お気に入りのチャットは削除せず残す。
     */
    suspend fun loadChatSessions(): List<ChatSession> {
        val uid = uidOrThrow()

        // 1. 全件取得（更新日時降順）
        val snap = db.collection("users")
            .document(uid)
            .collection("chats")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        val allDocs = snap.documents

        // ドキュメントをChatSessionオブジェクトに変換
        val allSessions = allDocs.map { d ->
            ChatSession(
                id = d.id,
                title = d.getString("title") ?: "",
                createdAt = d.getTimestamp("createdAt")?.seconds ?: 0L,
                updatedAt = d.getTimestamp("updatedAt")?.seconds ?: 0L,
                isArrangeMode = d.getBoolean("isArrangeMode") ?: false,
                // ★追加: Firestoreからお気に入り状態を取得
                isFavorite = d.getBoolean("isFavorite") ?: false
            )
        }

        val keepLimit = 30

        // ★変更: お気に入りとそうでないものを分ける
        val (favorites, nonFavorites) = allSessions.partition { it.isFavorite }

        // 2. お気に入りではないチャットが30件を超える場合は削除処理を行う
        if (nonFavorites.size > keepLimit) {
            val docsToDelete = nonFavorites.drop(keepLimit)

            for (session in docsToDelete) {
                try {
                    deleteChat(session.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. お気に入り全件 + お気に入り以外の最新30件 を結合して返す (再度日付順にソート)
        val result = (favorites + nonFavorites.take(keepLimit)).sortedByDescending { it.updatedAt }
        return result
    }

    /**
     * ★追加: チャットのお気に入り状態を更新する
     */
    suspend fun updateChatFavorite(chatId: String, isFavorite: Boolean) {
        val uid = uidOrThrow()
        db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .update("isFavorite", isFavorite)
            .await()
    }

    /**
     * ★追加: 指定したチャットIDに紐付いているレシピ情報を取得する
     * (ポップアップ表示用)
     */
    suspend fun getChatRecipeData(chatId: String): Recipe? {
        val uid = uidOrThrow()
        val snapshot = db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .get()
            .await()

        // sourceRecipeフィールドがあるか確認
        val sourceMap = snapshot.get("sourceRecipe") as? Map<String, Any> ?: return null

        // MapからRecipeオブジェクトを復元
        return try {
            Recipe().apply {
                id = sourceMap["id"] as? String ?: ""
                recipeTitle = sourceMap["recipeTitle"] as? String ?: ""
                foodImageUrl = sourceMap["foodImageUrl"] as? String ?: ""

                @Suppress("UNCHECKED_CAST")
                recipeMaterial = sourceMap["recipeMaterial"] as? List<String>

                @Suppress("UNCHECKED_CAST")
                servingAmounts = (sourceMap["servingAmounts"] as? List<String>) ?: emptyList()

                @Suppress("UNCHECKED_CAST")
                recipeSteps = sourceMap["recipeSteps"] as? List<String>
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * あるチャットのメッセージ一覧を取得（AiFragment の復元用）
     */
    suspend fun loadMessages(chatId: String): List<ChatMessage> {
        val uid = uidOrThrow()
        val snap = db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()

        return snap.documents.map { d ->
            val role = d.getString("role") ?: "user"
            val text = d.getString("text") ?: ""
            ChatMessage(message = text, isUser = (role == "user"))
        }
    }

    /**
     * 指定されたチャットとメッセージを削除
     */
    suspend fun deleteChat(chatId: String) {
        val uid = uidOrThrow()

        val chatRef = db.collection("users")
            .document(uid)
            .collection("chats")
            .document(chatId)

        // messages コレクションの取得
        val messagesSnap = chatRef.collection("messages").get().await()

        // バッチで一括削除
        db.runBatch { batch ->
            // message 全削除
            for (doc in messagesSnap.documents) {
                batch.delete(doc.reference)
            }
            // chat 本体も削除
            batch.delete(chatRef)
        }.await()
    }
}