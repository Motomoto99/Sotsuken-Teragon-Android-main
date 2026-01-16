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
import com.google.android.material.button.MaterialButton
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
        val fabAdd = view.findViewById<MaterialButton>(R.id.fabAddRecord)
        val textAiComment = view.findViewById<TextView>(R.id.textAiComment)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recordAdapter = RecordAdapter(recordList)
        recyclerView.adapter = recordAdapter

        fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), RecordInputActivity::class.java))
        }

        viewModel.aiComment.observe(viewLifecycleOwner) { comment ->
            textAiComment.text = comment
        }

        // ★ViewModelにデータ取得を指示（保存データがない場合は生成も行う）
        viewModel.loadAiComment()

        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
        // 戻ってきたときに最新のアドバイス（InputActivityで更新されたもの）を再取得
        viewModel.refreshComment()
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
                    recordList.add(document.toObject(Record::class.java))
                }
                recordAdapter.notifyDataSetChanged()
            }
    }
}