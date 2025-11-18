package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout // ★ インポート
import androidx.core.view.ViewCompat // ★ インポート
import androidx.core.view.WindowInsetsCompat // ★ インポート
import androidx.core.view.updatePadding // ★ インポート
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_home.xmlレイアウトを読み込む
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    // ★★★ 追加 ★★★
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // topBarがステータスバーと重ならないように調整
        val topBar = view.findViewById<ConstraintLayout>(R.id.topBar)

        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // topBar の元々の paddingStart/End/Bottom を維持しつつ、
            // paddingTop だけをステータスバーの高さ分（systemBars.top）だけ更新
            v.updatePadding(
                top = systemBars.top + v.paddingTop // 元のPaddingTopに加算 (もし16dpを固定値として設定済みなら、 v.paddingTop を削除して systemBars.top だけにするか、元のXMLの paddingTop を 0 にする)
                // ※ fragment_home.xml の topBar の padding="16dp" を
                // paddingTop="0dp", paddingStart="16dp", paddingEnd="16dp", paddingBottom="16dp"
                // に変更したほうが、より安全に v.updatePadding(top = systemBars.top) を適用できます。
                // (今回は簡潔さのため、XML側でpadding="16dp"とし、ここでは v.updatePadding(top = systemBars.top + v.paddingTop) としています)
            )

            // ★追記：もしXMLのpadding="16dp"を活かすなら、以下の方が安全です
            // val originalPaddingTop = v.paddingTop // 元の16dp
            // v.updatePadding(top = systemBars.top + originalPaddingTop)

            insets
        }
    }
}