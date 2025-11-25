package com.example.sotugyo_kenkyu.favorite

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

class FolderAdapter(
    private val folderList: List<RecipeFolder>,
    private val onItemClick: (RecipeFolder) -> Unit,
    private val onDeleteClick: (RecipeFolder) -> Unit // ★追加
) : RecyclerView.Adapter<FolderAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textFolderName)
        val textCount: TextView = view.findViewById(R.id.textRecipeCount)
        val buttonDelete: ImageButton = view.findViewById(R.id.buttonDeleteFolder) // ★追加
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val folder = folderList[position]

        if (folder.id == "ALL_FAVORITES") {
            holder.textName.text = "すべてのお気に入り"
            holder.textCount.text = "全保存済みレシピ"

            // ★「すべてのお気に入り」は削除できないのでボタンを隠す
            holder.buttonDelete.visibility = View.GONE
        } else {
            holder.textName.text = folder.name
            holder.textCount.text = "${folder.recipeCount} 件のレシピ"

            // ★通常のフォルダは削除ボタンを表示
            holder.buttonDelete.visibility = View.VISIBLE
        }

        holder.container.setOnClickListener {
            onItemClick(folder)
        }

        // ★削除ボタンのクリック処理
        holder.buttonDelete.setOnClickListener {
            onDeleteClick(folder)
        }
    }

    override fun getItemCount(): Int = folderList.size
}