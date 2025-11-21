package com.example.sotugyo_kenkyu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class RecordFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recordAdapter: RecordAdapter
    private val recordList = mutableListOf<Record>()

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

        recyclerView = view.findViewById(R.id.recyclerViewRecord)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddRecord)

        // 2列のグリッド表示
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recordAdapter = RecordAdapter(recordList)
        recyclerView.adapter = recordAdapter

        // ＋ボタンを押したら記録入力画面へ遷移
        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), RecordInputActivity::class.java)
            startActivity(intent)
        }

        // 初回データ読み込み
        loadRecords()
    }

    // 画面に戻ってきたときも更新（追加・削除したデータを反映するため）
    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 日付の新しい順に取得
        db.collection("users").document(user.uid).collection("my_records")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                recordList.clear()
                for (document in result) {
                    val record = document.toObject(Record::class.java)
                    recordList.add(record)
                }
                recordAdapter.notifyDataSetChanged()
            }
    }
}

// --- アダプター ---
class RecordAdapter(private val items: List<Record>) :
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

        holder.textFoodName.text = item.menuName

        // 日付・時間をフォーマット (例: 11/20 12:00)
        if (item.date != null) {
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
            holder.textTime.text = sdf.format(item.date.toDate())
        } else {
            holder.textTime.text = ""
        }

        // 画像表示
        if (item.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(item.imageUrl)
                .centerCrop()
                .into(holder.imageFood)
        } else {
            // 画像がない場合
            Glide.with(holder.itemView.context)
                .load(R.drawable.background_with_logo) // デフォルト画像
                .centerCrop()
                .into(holder.imageFood)
        }

        // ★ クリックしたら詳細画面へ遷移
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, RecordDetailActivity::class.java)

            // 詳細画面に必要なデータを渡す
            intent.putExtra("RECORD_ID", item.id)
            intent.putExtra("USER_ID", item.userId)
            intent.putExtra("MENU_NAME", item.menuName)
            intent.putExtra("MEMO", item.memo)
            intent.putExtra("IMAGE_URL", item.imageUrl)
            intent.putExtra("IS_PUBLIC", item.isPublic)
            intent.putExtra("RATING", item.rating)
            if (item.date != null) {
                intent.putExtra("DATE_TIMESTAMP", item.date.toDate().time)
            }

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = items.size
}