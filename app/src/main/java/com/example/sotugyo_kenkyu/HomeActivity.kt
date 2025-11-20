package com.example.sotugyo_kenkyu

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.lifecycleScope       // ★ 追加
import kotlinx.coroutines.launch             // ★ 追加

class HomeActivity : AppCompatActivity() {

    // ★ フラグメントのインスタンスを作成
    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val aiFragment = AiFragment() // ★ 追加
    private val favoriteFragment = FavoriteFragment() // ★ 追加
    private val recordFragment = RecordFragment() // ★ 追加

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // ★ ログイン後に AI セッションを準備（プロンプト読み込みもここで実行）
        lifecycleScope.launch {
            try {
                AiChatSessionManager.ensureSessionInitialized()
            } catch (e: Exception) {
                e.printStackTrace() // 必要なら Toast してもOK
            }
        }

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // ナビゲーションのクリックリスナー
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(homeFragment)
                R.id.nav_search -> replaceFragment(searchFragment)
                R.id.nav_ai -> replaceFragment(aiFragment)
                R.id.nav_favorite -> replaceFragment(favoriteFragment)
                R.id.nav_record -> replaceFragment(recordFragment)
            }
            true
        }

        // 起動時にHomeFragmentをデフォルトで表示
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // フラグメント切り替え関数
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment) // ← ChatList/AiFragment側もこのIDに合わせる
            .commit()
    }
}
