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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DataLoadingActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textLoading: TextView

    // ホーム画面に渡す未読数
    private var initialUnreadCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_loading)

        progressBar = findViewById(R.id.progressBar)
        textLoading = findViewById(R.id.textLoading)

        // 中央のイラスト画像は、XML (activity_data_loading.xml) の android:src で
        // 指定されているため、ここでは特別な処理は不要です（四角いまま表示されます）。

        progressBar.max = 100

        // 読み込みシーケンス開始
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
            val steps = listOf(40, 60, 80)
            for (step in steps) {
                updateProgress(step)
                delay(400) // 各ステップで少し待機して「読み込んでる感」を出す
            }

            // 4. データ読み込みの完了を待つ
            // (演出が80%まで進んでも、データ取得が終わっていなければここで待機)
            jobData.join()

            // 5. 完了演出 (100%)
            updateProgress(100)
            delay(500) // 100%の状態を少し見せる

            // 6. ホーム画面へ遷移
            goToHome()
        }
    }

    // 実際のデータ取得・キャッシュ処理
    private suspend fun loadRealData() {
        try {
            val user = FirebaseAuth.getInstance().currentUser

            // --- タスク1: アイコン画像の先読み (キャッシュ) ---
            // ホーム画面で丸く表示するため、ここでも circleCrop() を適用してキャッシュさせる
            if (user?.photoUrl != null) {
                Glide.with(this@DataLoadingActivity)
                    .load(user.photoUrl)
                    .circleCrop() // ホーム画面の設定に合わせる
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
            }

            // 運営アイコン(ベル)も先読み
            Glide.with(this@DataLoadingActivity)
                .load(R.drawable.ic_notifications)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()

            // --- タスク2: 未読数の計算 ---
            val db = FirebaseFirestore.getInstance()
            val prefs = getSharedPreferences("prefs_notification", MODE_PRIVATE)
            val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

            // 通知データを取得
            val snapshot = db.collection("notifications").get().await()

            // 未読数をカウント
            var count = 0
            for (doc in snapshot.documents) {
                val notification = doc.toObject(Notification::class.java)
                val date = notification?.date
                // 最後に見た時間より新しいものをカウント
                if (date != null && date.toDate().time > lastSeenTime) {
                    count++
                }
            }
            initialUnreadCount = count // 結果を保持

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

        textLoading.text = "データを読み込んでいます... $value%"
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // 計算した未読数を渡す（ホーム画面で即座にバッジを表示するため）
        intent.putExtra("INITIAL_UNREAD_COUNT", initialUnreadCount)

        // 戻るボタンで読み込み画面に戻らないように履歴を削除
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

        // フェードイン・アウトのアニメーション
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}