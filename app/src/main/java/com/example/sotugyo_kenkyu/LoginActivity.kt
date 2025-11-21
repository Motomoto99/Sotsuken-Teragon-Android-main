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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore // 追加
import com.google.firebase.firestore.SetOptions // 追加

class LoginActivity : AppCompatActivity() {

    private val auth: FirebaseAuth by lazy { Firebase.auth }

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleGoogleSignInResult(result.data)
        }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            goToHomeScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContentView(R.layout.activity_login)

        setupGoogleLogin()

        findViewById<MaterialButton>(R.id.emailLoginButton).setOnClickListener {
            startActivity(Intent(this, EmailLoginActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.registerButton).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.googleLoginButton).setOnClickListener {
            signInWithGoogle()
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
                        val user = auth.currentUser
                        // ★新規ユーザーかどうか判定
                        val isNewUser = authTask.result.additionalUserInfo?.isNewUser == true

                        if (user != null && isNewUser) {
                            // ★Googleの情報を使わず、固定の初期値をFirestoreに保存する
                            val db = FirebaseFirestore.getInstance()
                            val userData = hashMapOf(
                                "username" to "初期ユーザー", // 名前を固定
                                "photoUrl" to ""           // 画像なし（デフォルトアイコンを表示させる）
                            )

                            db.collection("users").document(user.uid)
                                .set(userData, SetOptions.merge())
                                .addOnCompleteListener {
                                    // 保存完了後にホームへ（失敗しても進む）
                                    goToHomeScreen()
                                }
                        } else {
                            // 既存ユーザーはそのままホームへ
                            goToHomeScreen()
                        }
                    } else {
                        Toast.makeText(this, "Firebase ログインに失敗しました", Toast.LENGTH_SHORT).show()
                    }
                }

        } catch (e: ApiException) {
            Toast.makeText(this, "Googleログインエラー: ${e.statusCode}", Toast.LENGTH_LONG).show()
        }
    }

    private fun goToHomeScreen() {
        val intent = Intent(this, DataLoadingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(0, 0)
    }
}