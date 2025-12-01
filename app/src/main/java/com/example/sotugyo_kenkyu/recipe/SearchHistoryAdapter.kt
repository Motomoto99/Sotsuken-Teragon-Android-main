package com.example.sotugyo_kenkyu.recipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

class SearchHistoryAdapter(
    private var historyList: List<String>,
    private val onItemClick: (String) -> Unit // タップ時の処理
) : RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder>() {

    fun updateData(newList: List<String>) {
        historyList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        // 標準のシンプルなレイアウトを使うわ（アイコン＋テキスト）
        // ※もし自分で item_search_history.xml を作るなら書き換えてね
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val keyword = historyList[position]
        holder.bind(keyword)
    }

    override fun getItemCount(): Int = historyList.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(keyword: String) {
            textView.text = keyword
            // 虫眼鏡アイコンを左につける（おまけ）
            textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_recent_history, 0, 0, 0)
            textView.compoundDrawablePadding = 24

            itemView.setOnClickListener {
                onItemClick(keyword)
            }
        }
    }
}