package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

// 星を押したときに出す「フォルダ選択画面」
class AddToFolderBottomSheet(
    private val targetRecipe: Recipe
) : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonCreateNew: View
    private val folders = mutableListOf<RecipeFolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 専用のレイアウトが必要です（後述）
        return inflater.inflate(R.layout.bottom_sheet_add_to_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerFolders)
        buttonCreateNew = view.findViewById(R.id.buttonCreateNewFolder)

        recyclerView.layoutManager = LinearLayoutManager(context)

        // 新規フォルダ作成ボタン
        buttonCreateNew.setOnClickListener {
            showCreateFolderDialog()
        }

        loadFolders()
    }

    private fun loadFolders() {
        lifecycleScope.launch {
            try {
                val list = FolderRepository.getFolders()
                folders.clear()
                folders.addAll(list)

                // アダプター設定（簡易的な内部クラスとして定義）
                recyclerView.adapter = FolderSelectionAdapter(folders) { folder ->
                    // フォルダがクリックされたらレシピを追加
                    addRecipeToFolder(folder)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun addRecipeToFolder(folder: RecipeFolder) {
        lifecycleScope.launch {
            try {
                FolderRepository.addRecipeToFolder(folder.id, targetRecipe)
                Toast.makeText(context, "「${folder.name}」に追加しました", Toast.LENGTH_SHORT).show()
                dismiss() // 画面を閉じる
            } catch (e: Exception) {
                Toast.makeText(context, "追加に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // フォルダ作成ダイアログ
    private fun showCreateFolderDialog() {
        val editText = EditText(context)
        editText.hint = "フォルダ名（例：夜ご飯）"

        AlertDialog.Builder(requireContext())
            .setTitle("新しいフォルダを作成")
            .setView(editText) // ここは本当はレイアウトでマージン調整した方が綺麗
            .setPositiveButton("作成") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    createNewFolder(name)
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun createNewFolder(name: String) {
        lifecycleScope.launch {
            try {
                FolderRepository.createFolder(name)
                loadFolders() // リストを更新
            } catch (e: Exception) {
                Toast.makeText(context, "作成失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- 内部アダプタークラス ---
    inner class FolderSelectionAdapter(
        private val items: List<RecipeFolder>,
        private val onClick: (RecipeFolder) -> Unit
    ) : RecyclerView.Adapter<FolderSelectionAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val textName: android.widget.TextView = v.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // Android標準のシンプルなリストレイアウトを使用
            val v = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textName.text = items[position].name
            holder.itemView.setOnClickListener { onClick(items[position]) }
        }

        override fun getItemCount() = items.size
    }
}