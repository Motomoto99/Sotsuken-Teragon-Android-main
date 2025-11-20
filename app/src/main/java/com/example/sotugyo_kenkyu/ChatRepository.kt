package com.example.sotugyo_kenkyu

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

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
                "updatedAt" to now
            )
        ).await()

        return docRef.id
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
     * ※今はまだ一覧画面で使っていなくてもOK（将来用）
     */
    suspend fun loadChatSessions(): List<ChatSession> {
        val uid = uidOrThrow()
        val snap = db.collection("users")
            .document(uid)
            .collection("chats")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snap.documents.map { d ->
            ChatSession(
                id = d.id,
                title = d.getString("title") ?: "",
                createdAt = d.getTimestamp("createdAt")?.seconds ?: 0L,
                updatedAt = d.getTimestamp("updatedAt")?.seconds ?: 0L
            )
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
}
