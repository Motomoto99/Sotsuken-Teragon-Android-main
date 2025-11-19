package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
        // fragment_home.xmlレイアウトを読み込む
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // topBarがステータスバーと重ならないようにパディングを調整
        val topBar = view.findViewById<ConstraintLayout>(R.id.topBar)

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // XMLで設定した元のpaddingTop (16dp) をピクセルに変換して保持
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()

            // ステータスバーの高さ + 元の余白 を設定
            v.updatePadding(top = systemBars.top + originalPaddingTop)

            insets
        }

        // 1. ユーザーアイコン（アカウント設定画面へ遷移）
        val userIcon: ImageButton = view.findViewById(R.id.iconUser)
        userIcon.setOnClickListener {
            val intent = Intent(activity, AccountSettingsActivity::class.java)
            startActivity(intent)
        }

        // 2. 通知アイコン（お知らせ画面へ遷移）
        val notificationIcon: ImageButton = view.findViewById(R.id.iconNotification)
        notificationIcon.setOnClickListener {
            val intent = Intent(activity, NotificationActivity::class.java)
            startActivity(intent)
        }
    }
}