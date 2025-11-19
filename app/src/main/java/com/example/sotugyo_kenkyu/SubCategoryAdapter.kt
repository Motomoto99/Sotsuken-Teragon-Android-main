package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubCategoryAdapter(
    private val categoryList: List<Pair<String, String>>, // IDと名前のペア
    private val onItemClick: (String, String) -> Unit
) : RecyclerView.Adapter<SubCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textName: TextView = view.findViewById(R.id.textCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 既存の item_category_button.xml を使い回します（画像部分は非表示にする処理を入れても良いですが、今回は簡単のためそのままで）
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_button, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (id, name) = categoryList[position]

        holder.textName.text = name

        // アイコンや矢印の制御（必要なら）
        val icon = holder.itemView.findViewById<View>(R.id.iconCategory)
        icon.visibility = View.GONE // 中分類ではアイコンを隠す例

        holder.itemView.setOnClickListener {
            onItemClick(id, name)
        }
    }

    override fun getItemCount(): Int = categoryList.size
}