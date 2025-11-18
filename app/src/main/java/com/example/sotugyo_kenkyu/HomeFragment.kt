package com.example.sotugyo_kenkyu

import android.content.Intent // ★ インポート
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton // ★ インポート
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topBar = view.findViewById<ConstraintLayout>(R.id.topBar)

        // WindowInsetsの調整 (変更なし)
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // (前回のリプライで提案した padding 調整ロジック)
            // 例: v.updatePadding(top = systemBars.top + v.paddingTop)
            // (元のpaddingTopを活かす場合)
            val originalPaddingTop = 16 * resources.displayMetrics.density.toInt() // 16dpを仮定
            v.updatePadding(top = systemBars.top + originalPaddingTop)


            insets
        }

        // ★★★ 追加 ★★★
        // ユーザーアイコンのクリックリスナーを設定
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        userIcon.setOnClickListener {
            // AccountSettingsActivity を起動
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            startActivity(intent)
        }
    }
}