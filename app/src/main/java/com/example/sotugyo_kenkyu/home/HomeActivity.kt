package com.example.sotugyo_kenkyu.home

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.ai.AiChatSessionManager
import com.example.sotugyo_kenkyu.ai.AiFragment
import com.example.sotugyo_kenkyu.common.TabContainerFragment
import com.example.sotugyo_kenkyu.favorite.FavoriteFragment
import com.example.sotugyo_kenkyu.recipe.SearchFragment
import com.example.sotugyo_kenkyu.record.RecordFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    // 各タブのコンテナフラグメントを保持するマップ
    private val tabFragments =  mutableMapOf<Int, TabContainerFragment>()

    // 現在表示中のタブID
    private var currentTabId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        // AIセッション準備
        lifecycleScope.launch {
            try {
                AiChatSessionManager.ensureSessionInitialized()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = systemBars.bottom)
            insets
        }

        // 初期表示
        if (savedInstanceState == null) {
            switchTab(R.id.nav_home)
            bottomNavigation.selectedItemId = R.id.nav_home
        }

        // ナビゲーション切替リスナー
        bottomNavigation.setOnItemSelectedListener { item ->
            switchTab(item.itemId)
            true
        }

        // 戻るボタンの挙動を制御（タブ内の履歴があればそれを戻る）
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = tabFragments[currentTabId]
                // 現在のタブの中で戻れる履歴があるか確認
                if (currentFragment != null && currentFragment.canGoBack()) {
                    currentFragment.goBack()
                } else {
                    // 履歴がなければアプリ終了（またはデフォルトの挙動）
                    // ホーム以外にいる場合はホームに戻すというUXも一般的ですが、今回は終了にします
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun switchTab(tabId: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        // 1. 現在表示されているタブがあれば隠す
        val currentFragment = tabFragments[currentTabId]
        if (currentFragment != null) {
            transaction.hide(currentFragment)
        }

        // 2. 次に表示するタブを取得（なければ作成）
        var targetFragment = tabFragments[tabId]
        if (targetFragment == null) {
            // 初回作成時
            targetFragment = createTabFragment(tabId)
            tabFragments[tabId] = targetFragment
            // activity_home.xml で変更したID (main_nav_container) に追加
            transaction.add(R.id.main_nav_container, targetFragment, tabId.toString())
        } else {
            // 既存なら表示
            transaction.show(targetFragment)
        }

        transaction.commit()
        currentTabId = tabId
    }

    // タブIDに対応するルートフラグメントを指定してコンテナを作成
    private fun createTabFragment(tabId: Int): TabContainerFragment {
        val rootClass = when (tabId) {
            R.id.nav_home -> HomeFragment::class.java
            R.id.nav_search -> SearchFragment::class.java
            R.id.nav_ai -> AiFragment::class.java
            R.id.nav_favorite -> FavoriteFragment::class.java
            R.id.nav_record -> RecordFragment::class.java
            else -> HomeFragment::class.java
        }
        return TabContainerFragment.newInstance(rootClass)
    }
}