package com.example.sotugyo_kenkyu.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RecordViewModel : ViewModel() {

    // AIのコメントを保持するLiveData
    private val _aiComment = MutableLiveData<String>()
    val aiComment: LiveData<String> = _aiComment

    // 初期化フラグ（何度も生成しないため）
    private var isInitialized = false

    // AIモデルの初期化
    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    // 画面生成時に呼ばれる関数
    fun loadInitialComment() {
        if (isInitialized) return // すでに生成済みなら何もしない（保持したデータを表示）

        isInitialized = true
        fetchDataAndGenerateComment()
    }

    // 再読み込み用（必要なら呼ぶ）
    fun refreshComment() {
        fetchDataAndGenerateComment()
    }

    private fun fetchDataAndGenerateComment() {
        _aiComment.value = "食事記録を確認しています..."

        viewModelScope.launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    _aiComment.value = "ログインしてくださいね！"
                    return@launch
                }

                val db = FirebaseFirestore.getInstance()

                // 1. 直近の食事記録を取得 (最新3件)
                val recordsSnapshot = db.collection("users").document(user.uid)
                    .collection("my_records")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()

                val recentMenus = recordsSnapshot.documents.mapNotNull { doc ->
                    doc.getString("menuName")
                }

                // 記録がなければ終了
                if (recentMenus.isEmpty()) {
                    _aiComment.value = "こんにちは！右下の＋ボタンから、初めての食事を記録してみましょう！"
                    return@launch
                }

                // 2. アレルギー情報を取得
                val userSnapshot = db.collection("users").document(user.uid).get().await()
                val allergies = userSnapshot.get("allergies") as? List<String> ?: emptyList()

                val allergyText = if (allergies.isNotEmpty()) {
                    "（ユーザーのアレルギー: ${allergies.joinToString(", ")}）"
                } else {
                    ""
                }

                // 3. AIへのプロンプト作成
                val prompt = """
                    あなたは親しみやすい栄養管理のパートナーキャラクターです。
                    ユーザーの直近の食事記録を見て、40文字以内で短く、元気が出るコメントをしてください。
                    栄養バランスへのアドバイスや、褒め言葉を含めてください。
                    
                    【直近の食事】
                    ${recentMenus.joinToString("\n")}
                    
                    $allergyText
                    
                    口調の例：
                    「野菜も摂れていて偉いですね！この調子！」
                    「お肉が続いてますね、次はお魚はいかが？」
                    「美味しそう！でもちょっとカロリー高めかも？」
                """.trimIndent()

                // 4. AI生成実行
                val response = generativeModel.generateContent(prompt)
                val comment = response.text?.trim() ?: "今日も良い食事を！"

                _aiComment.value = comment

            } catch (e: Exception) {
                e.printStackTrace()
                _aiComment.value = "元気ですか？今日も美味しく食べましょう！"
            }
        }
    }
}