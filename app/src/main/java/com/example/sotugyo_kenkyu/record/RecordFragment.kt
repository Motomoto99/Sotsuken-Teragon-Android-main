package com.example.sotugyo_kenkyu.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView // 追加
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels // 追加
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.ai.AiChatSessionManager // ★追加
import com.google.android.material.bottomnavigation.BottomNavigationView // 追加
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RecordFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recordAdapter: RecordAdapter
    private val recordList = mutableListOf<Record>()

    // ★ViewModelの初期化 (これで画面回転してもデータが消えません)
    private val viewModel: RecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.header)

        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        recyclerView = view.findViewById(R.id.recyclerViewRecord)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddRecord)

        // ★追加: AIコメント部分のUI取得
        val textAiComment = view.findViewById<TextView>(R.id.textAiComment)
        val layoutAiAdvice = view.findViewById<View>(R.id.layoutAiAdvice)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recordAdapter = RecordAdapter(recordList)
        recyclerView.adapter = recordAdapter

        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), RecordInputActivity::class.java)
            startActivity(intent)
        }

        // ★追加: AIコメントの監視と表示更新
        viewModel.aiComment.observe(viewLifecycleOwner) { comment ->
            textAiComment.text = comment
        }

        // ★追加: 初回読み込み開始 (ViewModel内で「すでに読み込み済みなら無視」する制御が入っています)
        viewModel.loadInitialComment()

        // ★追加: 吹き出しタップで「AIタブ」へ移動し、会話を引き継ぐ
        layoutAiAdvice.setOnClickListener {
            // 現在表示されているコメントを取得
            val currentComment = viewModel.aiComment.value ?: ""

            // 文脈を作成してManagerに預ける
            val contextMessage = """
                先ほど、私の食事記録を見て以下のコメントをくれましたね。
                「$currentComment」
                
                これについて、具体的になぜそう思ったのかや、おすすめの改善レシピなどを詳しく教えてください。
            """.trimIndent()

            AiChatSessionManager.pendingContext = contextMessage

            // 親のアクティビティ(HomeActivity)にあるBottomNavigationを操作して「AIチャットタブ」へ切り替える
            activity?.findViewById<BottomNavigationView>(R.id.bottomNavigation)?.selectedItemId = R.id.nav_ai
        }

        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
        // ※ここではAIコメントの再読み込みはしません（起動時保持のため）
        // もし「記録を追加して戻ってきたらコメントを変えたい」場合は、
        // viewModel.refreshComment() を呼ぶように変更してください。
    }

    private fun loadRecords() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

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