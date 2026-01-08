package com.example.sotugyo_kenkyu.account

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var imageViewUserIcon: ImageView
    private lateinit var editTextUsername: EditText
    private lateinit var buttonEditUsername: ImageButton
    private lateinit var buttonSaveUsername: Button
    private lateinit var textCharLimit: TextView

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_account_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        imageViewUserIcon = findViewById(R.id.imageViewUserIcon)
        val buttonEditIcon = findViewById<ImageButton>(R.id.buttonEditIcon)

        editTextUsername = findViewById(R.id.editTextUsername)
        buttonEditUsername = findViewById(R.id.buttonEditUsername)
        buttonSaveUsername = findViewById(R.id.buttonSaveUsername)
        textCharLimit = findViewById(R.id.textCharLimit)

        val backButton = findViewById<ImageButton>(R.id.buttonBack)
        val header = findViewById<View>(R.id.header)

        val menuAllergy = findViewById<View>(R.id.menuAllergySettings)
        val menuTerms = findViewById<View>(R.id.menuTerms)

        // ★変更: IDが変わったため修正
        val menuDeleteAccount = findViewById<View>(R.id.menuDeleteAccount) // アカウント削除(リスト)
        val textSignOut = findViewById<View>(R.id.textSignOut) // サインアウト(下部テキスト)

        val menuPrivacyPolicy = findViewById<View>(R.id.menuPrivacyPolicy)
        val menuContact = findViewById<View>(R.id.menuContact)

        // WindowInsets
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }

        editTextUsername.isEnabled = false
        editTextUsername.setTextColor(Color.parseColor("#404040"))

        loadUserProfile()

        backButton.setOnClickListener { finish() }

        menuAllergy.setOnClickListener {
            val intent = Intent(this, AllergySettingsActivity::class.java)
            startActivity(intent)
        }

        menuTerms.setOnClickListener {
            val intent = Intent(this, TermsOfServiceActivity::class.java)
            startActivity(intent)
        }

        menuPrivacyPolicy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        menuContact.setOnClickListener {
            val intent = Intent(this, ContactActivity::class.java)
            startActivity(intent)
        }

        // ★変更: アカウント削除ボタンの処理
        menuDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        // ★変更: サインアウトボタンの処理
        textSignOut.setOnClickListener {
            showSignOutConfirmation()
        }

        buttonEditIcon.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        buttonEditUsername.setOnClickListener {
            editTextUsername.isEnabled = true
            editTextUsername.requestFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextUsername, InputMethodManager.SHOW_IMPLICIT)
            editTextUsername.setSelection(editTextUsername.text.length)

            buttonSaveUsername.visibility = View.VISIBLE
            buttonEditUsername.visibility = View.INVISIBLE
            textCharLimit.visibility = View.VISIBLE
        }

        buttonSaveUsername.setOnClickListener {
            saveUsername()
        }

        // バージョン情報の自動表示
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            val versionText = findViewById<TextView>(R.id.textVersion)
            versionText.text = "Ver $version"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSignOutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("サインアウト")
            .setMessage("本当にサインアウトしますか？")
            .setPositiveButton("サインアウト") { _, _ ->
                auth.signOut()
                Toast.makeText(this, "サインアウトしました", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // ★追加: アカウント削除の確認ダイアログと処理
    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("アカウント削除")
            .setMessage("本当にアカウントを削除しますか？\nこの操作は取り消せません。\nすべてのデータが削除されます。")
            .setPositiveButton("削除する") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            // Firestoreのユーザーデータ削除（任意実装）
            // 注意: 本来はCloud Functions等で連動させるのが安全ですが、ここでは簡易的にクライアントから削除を試みます
            db.collection("users").document(user.uid).delete()

            // Firebase Authからユーザー削除
            user.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "アカウントを削除しました", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    // 再認証が必要な場合のエラー処理
                    Toast.makeText(this, "削除に失敗しました: ${e.message}\n再ログインしてから試してください。", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun loadUserProfile() {
        val user = auth.currentUser ?: return

        Glide.with(this)
            .load(R.drawable.outline_account_circle_24)
            .circleCrop()
            .into(imageViewUserIcon)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                        ?: user.displayName
                        ?: "初期ユーザー"
                    editTextUsername.setText(username)

                    val photoUrl = document.getString("photoUrl")
                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .circleCrop()
                            .into(imageViewUserIcon)
                    }
                }
            }
            .addOnFailureListener {
                editTextUsername.setText(user.displayName ?: "初期ユーザー")
            }
    }

    private fun uploadImage(imageUri: Uri) {
        val user = auth.currentUser ?: return
        Toast.makeText(this, "画像をアップロード中...", Toast.LENGTH_SHORT).show()

        val storageRef = storage.reference.child("users/${user.uid}/profile.jpg")

        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateUserProfileImage(uri)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "アップロード失敗: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserProfileImage(uri: Uri) {
        val user = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(uri)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = hashMapOf("photoUrl" to uri.toString())
                    db.collection("users").document(user.uid)
                        .set(userData, SetOptions.merge())

                    Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(imageViewUserIcon)

                    Toast.makeText(this, "アイコンを変更しました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "プロフィールの更新に失敗しました", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUsername() {
        val newName = editTextUsername.text.toString().trim()
        val user = auth.currentUser ?: return

        if (newName.isEmpty()) {
            Toast.makeText(this, "ユーザー名を入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        if (newName.length > 20) {
            Toast.makeText(this, "20文字以内で入力してください", Toast.LENGTH_SHORT).show()
            return
        }

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userData = hashMapOf("username" to newName)
                    db.collection("users").document(user.uid)
                        .set(userData, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "更新しました", Toast.LENGTH_SHORT).show()

                            editTextUsername.isEnabled = false
                            editTextUsername.setTextColor(Color.parseColor("#404040"))

                            buttonSaveUsername.visibility = View.GONE
                            buttonEditUsername.visibility = View.VISIBLE
                            textCharLimit.visibility = View.GONE
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "DB更新失敗: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "更新失敗: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}