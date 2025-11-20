package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DataLoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_loading)

        // 並行してデータを読み込む
        loadAllData {
            // 全ての読み込みが完了したらホームへ
            goToHome()
        }
    }

    private fun loadAllData(onComplete: () -> Unit) {
        var completedCount = 0
        val totalTasks = 2 // 行うタスクの数（アイコン読み込み + 通知確認）

        // 完了をチェックする関数
        fun checkComplete() {
            completedCount++
            if (completedCount >= totalTasks) {
                onComplete()
            }
        }

        // タスク1: ユーザーアイコンのプリロード
        val user = FirebaseAuth.getInstance().currentUser
        if (user?.photoUrl != null) {
            Glide.with(this)
                .load(user.photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload() // 画面には出さずにメモリ/ディスクにキャッシュする
        }
        // アイコンは読み込み完了を待つ必要はない（キャッシュされればOK）のですぐ次へ
        checkComplete()


        // タスク2: Firestoreのお知らせデータを取得（キャッシュさせる）
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications").get()
            .addOnCompleteListener {
                // 成功・失敗に関わらず完了とする（失敗してもホームには行かせる）
                checkComplete()
            }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        // 戻るボタンでこの読み込み画面に戻ってこないようにフラグを設定
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        // 画面遷移のアニメーションをフェードにする（自然に見せるため）
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}