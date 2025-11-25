package com.example.sotugyo_kenkyu

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job // ★追加
import kotlinx.coroutines.launch

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val folderList = mutableListOf<RecipeFolder>()
    private lateinit var adapter: FolderAdapter

    // ★追加: 読み込み処理を管理する変数
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

        // ここでの loadFolders() は削除し、onResume に任せます
        // loadFolders()
    }

    override fun onResume() {
        super.onResume()
        // 画面が表示されるたびに最新データを読み込む
        loadFolders()
    }

    private fun loadFolders() {
        // ★ポイント1: 前回の読み込み処理が生きていたらキャンセルする
        loadJob?.cancel()

        loadJob = lifecycleScope.launch {
            try {
                // Firestoreから取得
                val userFolders = FolderRepository.getFolders()

                // ★ポイント2: リストを完全にクリアしてから作り直す
                folderList.clear()

                // 先頭に「すべてのお気に入り」を追加
                folderList.add(RecipeFolder(id = "ALL_FAVORITES", name = "すべてのお気に入り"))

                // 取得したフォルダを追加（念のためIDで重複排除しておく）
                folderList.addAll(userFolders.distinctBy { it.id })

                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                // キャンセルされた場合のエラーは無視、それ以外は表示
                if (e !is kotlinx.coroutines.CancellationException) {
                    Toast.makeText(context, "読み込み失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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