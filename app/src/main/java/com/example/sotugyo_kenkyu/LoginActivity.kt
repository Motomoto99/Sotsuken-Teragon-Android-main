package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

/**
 * アプリ起動時の最初の画面（ログイン方法選択画面）
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Googleログインの結果を受け取る
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // ActivityResultContracts では resultCode だけだとエラー理由が特定できないため
            // 必ず「handleGoogleSignInResult」に渡して解析する
            handleGoogleSignInResult(result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // スプラッシュ
        installSplashScreen()

        // レイアウト読み込み
        setContentView(R.layout.activity_login)

        // FirebaseAuth 初期化
        auth = FirebaseAuth.getInstance()

        // Google ログイン設定
        setupGoogleLogin()

        // ボタン取得
        val emailLoginButton = findViewById<MaterialButton>(R.id.emailLoginButton)
        val googleLoginButton = findViewById<MaterialButton>(R.id.googleLoginButton)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)

        // メールログイン
        emailLoginButton.setOnClickListener {
            startActivity(Intent(this, EmailLoginActivity::class.java))
        }

        // 新規登録
        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Googleログイン
        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    /**
     * Googleログインの設定
     */
    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    /**
     * Googleログイン画面を開く
     */
    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(intent)
    }

    /**
     * Googleログインの結果を処理し Firebase 認証に連携
     */
    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            val idToken = account.idToken ?: run {
                Toast.makeText(this, "IDトークンを取得できませんでした", Toast.LENGTH_SHORT).show()
                return
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        goToHomeScreen()
                    } else {
                        Toast.makeText(this, "Firebase ログインに失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }

        } catch (e: ApiException) {
            Toast.makeText(this, "Googleログインエラー: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * ホーム画面へ遷移
     */
    private fun goToHomeScreen() {
        val intent = Intent(this, HomeActivity::class.java) // ←ここを変更してOK
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
