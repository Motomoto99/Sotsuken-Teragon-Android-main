package com.example.sotugyo_kenkyu

import android.content.Intent // ★追加
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RecordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.header)

        // ヘッダーのパディング調整
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewRecord)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddRecord)

        // 2列のグリッド表示
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // ダミーデータ
        val dummyData = listOf(
            RecordItem("08:00", "トーストセット", null),
            RecordItem("12:30", "トマトパスタ", "https://example.com/pasta.jpg"),
            RecordItem("15:00", "パンケーキ", null),
            RecordItem("19:00", "ハンバーグ定食", null),
            RecordItem("21:00", "プロテイン", null)
        )

        recyclerView.adapter = RecordAdapter(dummyData)

        // ★★★ ここを修正しました ★★★
        // ＋ボタンを押したら記録入力画面へ遷移
        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), RecordInputActivity::class.java)
            startActivity(intent)
        }
    }
}

// --- 以下、データクラスとアダプター（変更なし） ---

data class RecordItem(
    val time: String,
    val foodName: String,
    val imageUrl: String?
)

class RecordAdapter(private val items: List<RecordItem>) :
    RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    class RecordViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTime: TextView = view.findViewById(R.id.textTime)
        val textFoodName: TextView = view.findViewById(R.id.textFoodName)
        val imageFood: ImageView = view.findViewById(R.id.imageFood)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = items[position]
        holder.textTime.text = item.time
        holder.textFoodName.text = item.foodName

        // 画像の表示 (URLがなければデフォルト画像)
        if (item.imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.imageFood)
        } else {
            Glide.with(holder.itemView.context)
                .load(R.drawable.background_with_logo)
                .centerCrop()
                .into(holder.imageFood)
        }
    }

    override fun getItemCount(): Int = items.size
}