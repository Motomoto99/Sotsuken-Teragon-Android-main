package com.example.sotugyo_kenkyu

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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DataLoadingActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textLoading: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_loading)

        progressBar = findViewById(R.id.progressBar)
        textLoading = findViewById(R.id.textLoading)

        // プログレスバーの最大値を100に設定
        progressBar.max = 100

        // 読み込み開始
        startLoadingSequence()
    }

    private fun startLoadingSequence() {
        // コルーチンを使って「データ読み込み」と「演出」を並行して行う
        lifecycleScope.launch {
            // 1. 最初のステップ (0 -> 20%)
            updateProgress(20)
            delay(300) // 少し待つ

            // 2. 並行処理開始
            // jobData: 実際のデータ読み込み（アイコン + お知らせ）
            val jobData = launch {
                loadRealData()
            }

            // 3. 演出用ステップ (40% -> 60% -> 80%)
            // データ読み込みが終わっていても、演出として最低限ここまで見せる
            val steps = listOf(40, 60, 80)
            for (step in steps) {
                updateProgress(step)
                delay(400) // 各ステップで0.4秒ずつ待つ（刻む感じを出す）
            }

            // 4. データ読み込みが終わるのを待つ
            // (演出が先に80%まで行っても、データがまだならここで待機します)
            jobData.join()

            // 5. 完了ステップ (100%)
            updateProgress(100)
            delay(500) // 100%を見せる時間

            // 6. ホーム画面へ
            goToHome()
        }
    }

    // 実際のデータ取得処理
    private suspend fun loadRealData() {
        try {
            // タスク1: アイコン画像 (キャッシュのみ、待機不要)
            val user = FirebaseAuth.getInstance().currentUser
            if (user?.photoUrl != null) {
                Glide.with(this@DataLoadingActivity)
                    .load(user.photoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
            }

            // タスク2: Firestoreのお知らせデータ (待機する)
            val db = FirebaseFirestore.getInstance()
            db.collection("notifications").get().await()

        } catch (e: Exception) {
            e.printStackTrace()
            // エラーでも進む（ホーム画面で再取得などさせるため）
        }
    }

    // プログレスバーをアニメーション付きで更新
    private fun updateProgress(value: Int) {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, value)
        animation.duration = 300 // スムーズに動く時間
        animation.interpolator = DecelerateInterpolator()
        animation.start()

        textLoading.text = "データを読み込んでいます... $value%"
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}