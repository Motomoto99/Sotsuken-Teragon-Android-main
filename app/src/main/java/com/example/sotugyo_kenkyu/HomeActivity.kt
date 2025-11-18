package com.example.sotugyo_kenkyu

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
// ★ Toastをインポートしている行があれば削除 (例: import android.widget.Toast)

class HomeActivity : AppCompatActivity() {

    private val homeFragment = HomeFragment()
    // (他のフラグメントも今後作成)
    // private val searchFragment = SearchFragment()
    // ...

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

        // ★★★ 変更点 ★★★
        // 動作確認用の showToast(...) の呼び出しを削除
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> replaceFragment(homeFragment)

                R.id.nav_search -> {
                    // replaceFragment(searchFragment) // (未作成のためコメントアウト)
                    // showToast("検索") // ←
                }
                R.id.nav_ai -> {
                    // replaceFragment(aiFragment) // (未作成のためコメントアウト)
                    // showToast("AI") // ← 削除
                }
                R.id.nav_favorite -> {
                    // replaceFragment(favoriteFragment) // (未作成のためコメントアウト)
                    // showToast("お気に入り") // ← 削除
                }
                R.id.nav_record -> {
                    // replaceFragment(recordFragment) // (未作成のためコメントアウト)
                    // showToast("記録") // ← 削除
                }
            }
            true
        }

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // ★★★ 変更点 ★★★
    // 動作確認用の showToast 関数自体を削除
    /*
    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, "$message 画面", android.widget.Toast.LENGTH_SHORT).show()
    }
    */
}