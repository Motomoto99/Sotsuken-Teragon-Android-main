package com.example.sotugyo_kenkyu.common

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.home.HomeActivity
import com.example.sotugyo_kenkyu.notification.Notification
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query // 追加
import kotlinx.coroutines.Dispatchers // 追加
import kotlinx.coroutines.async // 追加
import kotlinx.coroutines.awaitAll // 追加
import kotlinx.coroutines.coroutineScope // 追加
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext // 追加

class DataLoadingActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textLoading: TextView

    private var skipDataLoad: Boolean = false

    private var initialUnreadCount: Int = 0
    private var loadingMessage: String = "データを読み込んでいます..."

    // ホーム画面に渡す未読数
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_loading)

        progressBar = findViewById(R.id.progressBar)
        textLoading = findViewById(R.id.textLoading)

        val customMessage = intent.getStringExtra("EXTRA_LOADING_MESSAGE")
        if (!customMessage.isNullOrEmpty()) {
            loadingMessage = customMessage
        }

        // ★追加: スキップフラグを受け取る
        skipDataLoad = intent.getBooleanExtra("EXTRA_SKIP_DATA_LOAD", false)

        progressBar.max = 100

        startLoadingSequence()
    }

    private fun startLoadingSequence() {
        lifecycleScope.launch {
            // 1. 開始演出 (0 -> 20%)
            updateProgress(20)
            delay(300)

            // 2. 裏でデータ読み込みを開始
            val jobData = launch {
                loadRealData()
            }

            // 3. 読み込み中もゲージを少しずつ進める演出 (40% -> 60% -> 80%)
            // 処理が重くなる可能性があるので、少しゆっくり進める
            val steps = listOf(40, 55, 70, 85)
            for (step in steps) {
                if (jobData.isActive) { // まだ終わっていなければ進める
                    updateProgress(step)
                    delay(500)
                }
            }

            // 4. データ読み込みの完了を待つ
            jobData.join()

            // 5. 完了演出 (100%)
            updateProgress(100)
            delay(200)

            // 6. ホーム画面へ遷移
            goToHome()
        }
    }

    // 実際のデータ取得・キャッシュ処理
    private suspend fun loadRealData() = coroutineScope {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            val db = FirebaseFirestore.getInstance()

            // --- タスク1: 自分のアイコン画像の先読み ---
            if (user?.photoUrl != null) {
                // メインスレッドでGlideを呼ぶ必要がある場合のためwithContextを使う（念のため）
                withContext(Dispatchers.Main) {
                    Glide.with(this@DataLoadingActivity)
                        .load(user.photoUrl)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .preload()
                }
            }

            // 運営アイコン(ベル)も先読み
            withContext(Dispatchers.Main) {
                Glide.with(this@DataLoadingActivity)
                    .load(R.drawable.ic_notifications)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
            }

            if (user != null) {
                // --- タスク2: 未読数の計算 ---
                val prefs = getSharedPreferences("prefs_notification", MODE_PRIVATE)
                val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

                // 通知データを取得（自分宛て）
                // ★ここで少し多めに取得してキャッシュさせておく
                val notificationsSnapshot = db.collection("users").document(user.uid)
                    .collection("notifications")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(30) // 最新30件を取得
                    .get()
                    .await()

                // 未読数をカウント
                var count = 0
                for (doc in notificationsSnapshot.documents) {
                    val notification = doc.toObject(Notification::class.java)
                    val date = notification?.date
                    if (date != null && date.toDate().time > lastSeenTime) {
                        count++
                    }
                }
                initialUnreadCount = count

                // --- ★追加タスク: 通知送信者のアイコンを事前読み込み (N+1対策) ---
                // 通知に含まれる senderUid をリストアップ（重複なし）
                val senderUids = notificationsSnapshot.documents
                    .mapNotNull { it.getString("senderUid") }
                    .distinct()

                // 送信者のユーザー情報を並列で取得して画像をプリロード
                // (async/awaitAll で一気に処理する)
                senderUids.map { uid ->
                    async(Dispatchers.IO) {
                        try {
                            // ユーザー情報を取得 (Firestoreのキャッシュも効くようになる)
                            val userDoc = db.collection("users").document(uid).get().await()
                            val photoUrl = userDoc.getString("photoUrl")

                            if (!photoUrl.isNullOrEmpty()) {
                                // 画像URLがあればGlideでプリロード（キャッシュに保存）
                                withContext(Dispatchers.Main) {
                                    Glide.with(this@DataLoadingActivity)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                                        .preload()
                                }
                            }
                        } catch (e: Exception) {
                            // 個別の読み込み失敗は無視して進む
                            e.printStackTrace()
                        }
                    }
                }.awaitAll() // 全員の処理が終わるのを待つ
            }

        } catch (e: Exception) {
            e.printStackTrace()
            // エラーが発生しても進行を止めず、ホーム画面で再取得させる
        }
    }

    // プログレスバーを滑らかに動かす
    private fun updateProgress(value: Int) {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, value)
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        animation.start()

        textLoading.text = "$loadingMessage $value%"
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra("INITIAL_UNREAD_COUNT", initialUnreadCount)

        // ★追加: レシピ遷移の情報があれば引き継ぐ
        if (getIntent().hasExtra("EXTRA_DESTINATION")) {
            val destination = getIntent().getStringExtra("EXTRA_DESTINATION")
            val recipeData = getIntent().getSerializableExtra("EXTRA_RECIPE_DATA")

            intent.putExtra("EXTRA_DESTINATION", destination)
            intent.putExtra("EXTRA_RECIPE_DATA", recipeData)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}