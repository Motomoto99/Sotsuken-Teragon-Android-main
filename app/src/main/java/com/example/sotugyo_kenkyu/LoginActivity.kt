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
 * アプリ起動時の最初の画面（ログイン方法選択画面）を担当するActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Googleサインインの結果を受け取る
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // resultCode に関係なく、とりあえず結果を解析する
            handleGoogleSignInResult(result.data)
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. スプラッシュスクリーン
        installSplashScreen()

        // 2. レイアウト設定
        setContentView(R.layout.activity_login)

        // FirebaseAuth 初期化
        auth = FirebaseAuth.getInstance()

        // GoogleSignIn の設定
        // default_web_client_id は google-services.json から自動生成される
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 3. ボタン取得
        val emailLoginButton = findViewById<MaterialButton>(R.id.emailLoginButton)
        val googleLoginButton = findViewById<MaterialButton>(R.id.googleLoginButton)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)

        // 4. 「メールログイン」ボタン
        emailLoginButton.setOnClickListener {
            val intent = Intent(this, EmailLoginActivity::class.java)
            startActivity(intent)
        }

        // 5. 「アカウント登録」ボタン
        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 6. 「Googleログイン」ボタン
        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    // Googleサインイン画面を開く
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    // Googleサインインの結果を処理し、FirebaseAuth と連携
    private fun handleGoogleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            val idToken = account.idToken
            if (idToken == null) {
                Toast.makeText(this, "IDトークン取得に失敗しました", Toast.LENGTH_SHORT).show()
                return
            }

            val credential = GoogleAuthProvider.getCredential(idToken, null)

            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { authTask ->
                    if (authTask.isSuccessful) {
                        goToHomeScreen()
                    } else {
                        Toast.makeText(this, "Firebaseログインに失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }

        } catch (e: ApiException) {
            // ここで Google 側のエラー内容が分かる
            Toast.makeText(
                this,
                "Googleログインエラー: statusCode=${e.statusCode}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // ホーム画面へ遷移（クラス名は実アプリに合わせて変更）
    private fun goToHomeScreen() {
        val intent = Intent(this, HomeActivity::class.java) // ←ここを実際のホームActivityに変更
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
