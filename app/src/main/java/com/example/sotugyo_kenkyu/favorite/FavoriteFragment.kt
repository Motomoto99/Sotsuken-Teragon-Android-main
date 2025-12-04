package com.example.sotugyo_kenkyu.favorite

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val folderList = mutableListOf<RecipeFolder>()
    private lateinit var adapter: FolderAdapter

    // 読み込み処理を管理する変数
    private var loadJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.header)
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
                v.updatePadding(top = systemBars.top + originalPaddingTop)
                insets
            }
        }

        // ★追加: フォルダ追加ボタンの設定
        // (fragment_favorite.xml に ImageButton id:btnAddFolder がある前提)
        val btnAddFolder = view.findViewById<ImageButton>(R.id.btnAddFolder)
        btnAddFolder?.setOnClickListener {
            showCreateFolderDialog()
        }

        recyclerView = view.findViewById(R.id.recyclerViewFavorites)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = FolderAdapter(
            folderList,
            onItemClick = { folder ->
                openFolderDetail(folder)
            },
            onDeleteClick = { folder ->
                showDeleteConfirmDialog(folder)
            }
        )
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // 画面が表示されるたびに最新データを読み込む
        loadFolders()
    }

    private fun loadFolders() {
        // 前回の読み込み処理が生きていたらキャンセルする
        loadJob?.cancel()

        loadJob = lifecycleScope.launch {
            try {
                // Firestoreから取得
                val userFolders = FolderRepository.getFolders()

                // リストを完全にクリアしてから作り直す
                folderList.clear()

                // 先頭に「すべてのお気に入り」を追加
                folderList.add(RecipeFolder(id = "ALL_FAVORITES", name = "すべてのお気に入り"))

                // 取得したフォルダを追加（念のためIDで重複排除しておく）
                folderList.addAll(userFolders.distinctBy { it.id })

                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // キャンセルされた場合のエラーは無視、それ以外は表示
                if (e !is CancellationException) {
                    Toast.makeText(context, "読み込み失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ★追加: フォルダ作成ダイアログを表示
    private fun showCreateFolderDialog() {
        val context = requireContext()
        val editText = EditText(context)
        editText.hint = "フォルダ名 (例: 週末の作り置き)"

        // レイアウト調整（左右に少し余白を入れる）
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.leftMargin = (24 * resources.displayMetrics.density).toInt()
        params.rightMargin = (24 * resources.displayMetrics.density).toInt()
        editText.layoutParams = params
        container.addView(editText)

        AlertDialog.Builder(context)
            .setTitle("新しいフォルダを作成")
            .setView(container)
            .setPositiveButton("作成") { _, _ ->
                val folderName = editText.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    createNewFolder(folderName)
                } else {
                    Toast.makeText(context, "フォルダ名を入力してください", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    // ★追加: Firestoreにフォルダを作成する処理
    private fun createNewFolder(folderName: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // 新しいフォルダのデータ
        val newFolderData = hashMapOf(
            "name" to folderName,
            "recipeIds" to emptyList<String>(),
            "createdAt" to Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("folders")
            .add(newFolderData)
            .addOnSuccessListener {
                Toast.makeText(context, "フォルダ「$folderName」を作成しました！", Toast.LENGTH_SHORT).show()
                // 一覧を再読み込みして反映
                loadFolders()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "作成に失敗しました: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmDialog(folder: RecipeFolder) {
        AlertDialog.Builder(requireContext())
            .setTitle("フォルダを削除")
            .setMessage("「${folder.name}」を削除しますか？\n中のレシピは「すべてのお気に入り」には残ります。")
            .setPositiveButton("削除") { _, _ ->
                deleteFolder(folder)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun deleteFolder(folder: RecipeFolder) {
        lifecycleScope.launch {
            try {
                FolderRepository.deleteFolder(folder.id)

                // リストから削除して更新
                folderList.remove(folder)
                adapter.notifyDataSetChanged()

                Toast.makeText(context, "削除しました", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "削除に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openFolderDetail(folder: RecipeFolder) {
        val fragment = FolderDetailFragment()
        val args = Bundle()
        args.putSerializable("TARGET_FOLDER", folder)
        fragment.arguments = args

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}