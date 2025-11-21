package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categoryList: List<CategoryData>,
    private val onItemClick: (CategoryData) -> Unit // クリック時の処理をFragmentに任せる
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    // Viewの保持役
    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textCategoryName)
        val icon: ImageView = view.findViewById(R.id.iconCategory)
        val container: View = view // クリック範囲用
    }

    // 1. ここでレイアウト(型)を作る
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        // 既存の item_category_button を使うよ！
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_button, parent, false)
        return CategoryViewHolder(view)
    }

    // 2. ここでデータを流し込む
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.textName.text = category.name
        holder.icon.setImageResource(category.iconRes)

        // クリックイベント
        holder.container.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int = categoryList.size
}