// app/src/main/java/com/example/sotugyo_kenkyu/record/RecordFragment.kt
package com.example.sotugyo_kenkyu.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.example.sotugyo_kenkyu.ai.AiChatSessionManager
import com.example.sotugyo_kenkyu.ai.PromptRepository // ★追加
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RecordFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recordAdapter: RecordAdapter
    private val recordList = mutableListOf<Record>()

    private val viewModel: RecordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }
    //【AI料理提案を一時停止】
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

        val textAiComment = view.findViewById<TextView>(R.id.textAiComment)
        val layoutAiAdvice = view.findViewById<View>(R.id.layoutAiAdvice)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recordAdapter = RecordAdapter(recordList)
        recyclerView.adapter = recordAdapter

        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), RecordInputActivity::class.java)
            startActivity(intent)
        }

        viewModel.aiComment.observe(viewLifecycleOwner) { comment ->
            textAiComment.text = comment
        }




        //【AI料理提案を一時停止】//
        //  【AI料理提案を一時停止】   //
        //      【AI料理提案を一時停止】   //
        //          【AI料理提案を一時停止】   //
        //viewModel.loadInitialComment()        //



        //【AI料理提案を一時停止】//
        //  【AI料理提案を一時停止】   //
        //      【AI料理提案を一時停止】   //
        //          【AI料理提案を一時停止】   //
        // ★修正: 吹き出しタップ時の処理（ここをコメントアウトして無効化！）
        /*
        layoutAiAdvice.setOnClickListener {
             val currentComment = textAiComment.text.toString()
             if (currentComment.isNotEmpty()) {
                 // AI画面へ遷移し、プロンプトを渡す処理
                 val prompt = createAdviceMessage(currentComment)
                 // ここで画面遷移しているコード（親のActivityやFragment経由など）
                 (activity as? HomeActivity)?.navigateToAiChat(prompt)
             }
        }
        */


        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
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