package com.example.sotugyo_kenkyu.favorite

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.recipe.Recipe
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

// ★修正: コンストラクタに sourceFolderId (移動元のフォルダID) を追加
class AddToFolderBottomSheet(
    private val targetRecipe: Recipe,
    private val sourceFolderId: String? = null // これがnullなら追加、あれば移動
) : BottomSheetDialogFragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var buttonCreateNew: View
    private val folders = mutableListOf<RecipeFolder>()

    // 閉じたときに呼び出し元に通知するためのコールバック
    var onDismissListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_to_folder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ★追加: タイトルをモードによって変えると親切（任意）
        val title = view.findViewById<TextView>(R.id.textSheetTitle) // layoutにIDがあれば
        if (title != null) {
            title.text = if (sourceFolderId != null) "移動先のフォルダを選択" else "フォルダに追加"
        }

        recyclerView = view.findViewById(R.id.recyclerFolders)
        buttonCreateNew = view.findViewById(R.id.buttonCreateNewFolder)

        recyclerView.layoutManager = LinearLayoutManager(context)

        buttonCreateNew.setOnClickListener {
            showCreateFolderDialog()
        }

        loadFolders()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // 画面が閉じたタイミングでコールバックを実行し、親画面を更新させる
        onDismissListener?.invoke()
    }

    private fun loadFolders() {
        lifecycleScope.launch {
            try {
                val list = FolderRepository.getFolders()
                folders.clear()
                // 移動の場合、移動元のフォルダ（自分自身）はリストに出さない方が親切
                val filteredList = if (sourceFolderId != null) {
                    list.filter { it.id != sourceFolderId }
                } else {
                    list
                }
                folders.addAll(filteredList)

                recyclerView.adapter = FolderSelectionAdapter(folders) { folder ->
                    processAddOrMove(folder)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ★修正: 追加または移動の処理
    private fun processAddOrMove(destinationFolder: RecipeFolder) {
        lifecycleScope.launch {
            try {
                // 1. まず移動先にレシピを追加（これは共通）
                FolderRepository.addRecipeToFolder(destinationFolder.id, targetRecipe)

                // 2. 移動モード（sourceFolderIdがある）なら、元のフォルダから削除
                if (sourceFolderId != null) {
                    FolderRepository.removeRecipeFromFolder(sourceFolderId, targetRecipe.id)
                    Toast.makeText(context, "「${destinationFolder.name}」に移動しました", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "「${destinationFolder.name}」に追加しました", Toast.LENGTH_SHORT).show()
                }

                dismiss() // 画面を閉じる
            } catch (e: Exception) {
                Toast.makeText(context, "処理に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateFolderDialog() {
        val editText = EditText(context)
        editText.hint = "フォルダ名"

        AlertDialog.Builder(requireContext())
            .setTitle("新しいフォルダを作成")
            .setView(editText)
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
                loadFolders()
            } catch (e: Exception) {
                Toast.makeText(context, "作成失敗", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class FolderSelectionAdapter(
        private val items: List<RecipeFolder>,
        private val onClick: (RecipeFolder) -> Unit
    ) : RecyclerView.Adapter<FolderSelectionAdapter.ViewHolder>() {

        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val textName: TextView = v.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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