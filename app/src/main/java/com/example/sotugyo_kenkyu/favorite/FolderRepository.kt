package com.example.sotugyo_kenkyu.favorite

import com.example.sotugyo_kenkyu.recipe.Recipe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object FolderRepository {

    private val db = FirebaseFirestore.getInstance()
    private val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // ユーザーの全フォルダを取得
    suspend fun getFolders(): List<RecipeFolder> {
        val uid = currentUserId ?: return emptyList()
        val snapshot = db.collection("users").document(uid)
            .collection("folders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            doc.toObject(RecipeFolder::class.java)!!.apply { id = doc.id }
        }
    }

    // 新しいフォルダを作成
    suspend fun createFolder(name: String) {
        val uid = currentUserId ?: return
        val folderData = hashMapOf(
            "name" to name,
            "createdAt" to FieldValue.serverTimestamp(),
            "recipeCount" to 0
        )
        db.collection("users").document(uid)
            .collection("folders")
            .add(folderData)
            .await()
    }

    // 指定したフォルダにレシピを追加
    suspend fun addRecipeToFolder(folderId: String, recipe: Recipe) {
        val uid = currentUserId ?: return

        // 1. レシピをサブコレクションに追加
        db.collection("users").document(uid)
            .collection("folders").document(folderId)
            .collection("recipes").document(recipe.id)
            .set(recipe) // レシピデータを丸ごと保存
            .await()

        // 2. フォルダのレシピ数を+1する（原子的な処理）
        db.collection("users").document(uid)
            .collection("folders").document(folderId)
            .update("recipeCount", FieldValue.increment(1))
    }

    // 指定したフォルダからレシピを削除
    suspend fun removeRecipeFromFolder(folderId: String, recipeId: String) {
        val uid = currentUserId ?: return

        db.collection("users").document(uid)
            .collection("folders").document(folderId)
            .collection("recipes").document(recipeId)
            .delete()
            .await()

        // レシピ数を-1
        db.collection("users").document(uid)
            .collection("folders").document(folderId)
            .update("recipeCount", FieldValue.increment(-1))
    }

    // あるレシピがどのフォルダに入っているかチェック（UI表示用）
    suspend fun getContainingFolderIds(recipeId: String): List<String> {
        // ※注意: これは厳密にやると通信量が増えるので、簡易的には
        // 「追加するときにチェックボックスで選ばせる」UIだけで管理するのが一般的です。
        // 今回は実装をシンプルにするため、ここは割愛してUI側で制御します。
        return emptyList()
    }
    // ★追加: 特定のフォルダ内のレシピ一覧を取得
    suspend fun getRecipesInFolder(folderId: String): List<Recipe> {
        val uid = currentUserId ?: return emptyList()

        val snapshot = db.collection("users").document(uid)
            .collection("folders").document(folderId)
            .collection("recipes")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            doc.toObject(Recipe::class.java)!!.apply { id = doc.id }
        }
    }

    // ★追加: 「すべてのお気に入り（デフォルト）」を取得する機能もここにまとめておくと便利
    suspend fun getAllFavorites(): List<Recipe> {
        val uid = currentUserId ?: return emptyList()

        val snapshot = db.collection("users").document(uid)
            .collection("favorites")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            doc.toObject(Recipe::class.java)!!.apply {
                id = doc.id
                isFavorite = true // ここにあるものは確実にお気に入り
            }
        }
    }
    // ★追加: フォルダを削除（中身のレシピも一緒に削除）
    suspend fun deleteFolder(folderId: String) {
        val uid = currentUserId ?: return

        val folderRef = db.collection("users").document(uid)
            .collection("folders").document(folderId)

        // 1. フォルダ内のレシピ一覧を取得
        val recipesSnapshot = folderRef.collection("recipes").get().await()

        // 2. バッチ処理で一括削除（データ整合性のため）
        db.runBatch { batch ->
            // 中身のレシピを全て削除
            for (doc in recipesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            // フォルダ本体を削除
            batch.delete(folderRef)
        }.await()
    }
    // ★追加: メインのお気に入り（すべてのお気に入り）から削除
    suspend fun removeGlobalFavorite(recipeId: String) {
        val uid = currentUserId ?: return
        db.collection("users").document(uid)
            .collection("favorites").document(recipeId)
            .delete()
            .await()
    }
}