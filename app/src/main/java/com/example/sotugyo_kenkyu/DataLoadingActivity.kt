package com.example.sotugyo_kenkyu

import android.animation.ObjectAnimator
import android.content.Context
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

    // 未読数を保持する変数
    private var initialUnreadCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_loading)

        progressBar = findViewById(R.id.progressBar)
        textLoading = findViewById(R.id.textLoading)

        // XMLで指定した画像を表示しているので、ここのGlide処理は不要（前回のまま削除状態でOK）
        // ※起動画面のイラストの話ではなく、ユーザーアイコンの「裏での先読み」の話です

        progressBar.max = 100
        startLoadingSequence()
    }

    private fun startLoadingSequence() {
        lifecycleScope.launch {
            updateProgress(20)
            delay(300)

            // データ読み込み実行
            val jobData = launch {
                loadRealData()
            }

            val steps = listOf(40, 60, 80)
            for (step in steps) {
                updateProgress(step)
                delay(400)
            }

            jobData.join()

            updateProgress(100)
            delay(500)

            goToHome()
        }
    }

    private suspend fun loadRealData() {
        try {
            // ★★★ 修正1: アイコン先読み（circleCropを追加） ★★★
            // これでホーム画面の設定と完全に一致し、キャッシュがヒットするようになります
            val user = FirebaseAuth.getInstance().currentUser
            if (user?.photoUrl != null) {
                Glide.with(this@DataLoadingActivity)
                    .load(user.photoUrl)
                    .circleCrop() // ★重要: ホーム画面と合わせる
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .preload()
            }
            // 運営アイコンも先読み
            Glide.with(this@DataLoadingActivity)
                .load(R.drawable.ic_notifications)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()

            // ★★★ 修正2: 未読数をここで計算する ★★★
            val db = FirebaseFirestore.getInstance()
            val prefs = getSharedPreferences("prefs_notification", Context.MODE_PRIVATE)
            val lastSeenTime = prefs.getLong("last_seen_timestamp", 0L)

            val snapshot = db.collection("notifications").get().await()

            // 未読カウント
            var count = 0
            for (doc in snapshot.documents) {
                val notification = doc.toObject(Notification::class.java)
                val date = notification?.date
                if (date != null && date.toDate().time > lastSeenTime) {
                    count++
                }
            }
            initialUnreadCount = count // 結果を保存

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateProgress(value: Int) {
        val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, value)
        animation.duration = 300
        animation.interpolator = DecelerateInterpolator()
        animation.start()
        textLoading.text = "データを読み込んでいます... $value%"
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // ★ 計算した未読数を渡す
        intent.putExtra("INITIAL_UNREAD_COUNT", initialUnreadCount)

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}