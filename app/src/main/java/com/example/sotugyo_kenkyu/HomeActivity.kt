package com.example.sotugyo_kenkyu

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

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
                R.id.nav_ai -> replaceFragment(aiFragment) // ★ 変更
                R.id.nav_favorite -> replaceFragment(favoriteFragment) // ★ 変更
                R.id.nav_record -> replaceFragment(recordFragment) // ★ 変更
            }
            true
        }

        // 起動時にHomeFragmentをデフォルトで表示 (変更なし)
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    // フラグメント切り替え関数 (変更なし)
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}