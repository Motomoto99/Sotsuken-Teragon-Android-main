package com.example.sotugyo_kenkyu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.material.button.MaterialButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleSignInResult(result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        setupGoogleLogin()

        val emailLoginButton = findViewById<MaterialButton>(R.id.emailLoginButton)
        val googleLoginButton = findViewById<MaterialButton>(R.id.googleLoginButton)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)

        emailLoginButton.setOnClickListener {
            startActivity(Intent(this, EmailLoginActivity::class.java))
        }

        registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }
    }

    // ★★★ 追加: 起動時にログイン状態をチェック ★★★
    override fun onStart() {
        super.onStart()
        // すでにログインしているユーザーがいるか確認
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // ログイン済みなら、ログイン画面をスキップして次へ
            goToHomeScreen()
        }
    }

    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signInWithGoogle() {
        val intent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(intent)
    }

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

    private fun goToHomeScreen() {
        // ★変更: HomeActivity ではなく DataLoadingActivity (読み込み画面) へ遷移
        val intent = Intent(this, DataLoadingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}