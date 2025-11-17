package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = FirebaseAuth.getInstance()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val register_action : Button = findViewById(R.id.buttonRegister)
        val email : EditText = findViewById(R.id.editTextEmail)
        val password : EditText = findViewById(R.id.editTextPassword)
        val re_password : EditText = findViewById(R.id.editTextPasswordConfirm)

        //「アカウントを登録」ボタンを押したときの処理
        register_action.setOnClickListener {
            val email = email.text.toString()
            val password = password.text.toString()
            val re_password = re_password.text.toString()

            //入力チェック
            if(email.isEmpty() || password.isEmpty() || re_password.isEmpty()){
                Toast.makeText(this, "すべての項目を入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(password != re_password){
                Toast.makeText(this, "パスワードが一致しません", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //Firebase Authでアカウント登録
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "登録しました", Toast.LENGTH_SHORT).show()
                        // 必要なら画面遷移など
                    } else {
                        Toast.makeText(this, "登録失敗: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }

        }
    }
}