package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val categoryList: List<CategoryData>,
    private val onItemClick: (CategoryData) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textCategoryName)
        val imgPhoto: ImageView = view.findViewById(R.id.imgCategoryPhoto) // 画像View
        val textEmoji: TextView = view.findViewById(R.id.textEmoji)       // 絵文字View
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_button, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categoryList[position]
        holder.textName.text = category.name

        // ★ ここで画像か絵文字かを切り替える！
        if (category.imageRes != null) {
            // 画像がある場合（メインカテゴリ）
            holder.imgPhoto.visibility = View.VISIBLE
            holder.textEmoji.visibility = View.GONE
            holder.imgPhoto.setImageResource(category.imageRes)
        } else {
            // 画像がない場合（その他カテゴリ）
            holder.imgPhoto.visibility = View.GONE
            holder.textEmoji.visibility = View.VISIBLE
            holder.textEmoji.text = category.emoji ?: "📁"
        }

        holder.container.setOnClickListener {
            onItemClick(category)
        }
    }

    override fun getItemCount(): Int = categoryList.size
}