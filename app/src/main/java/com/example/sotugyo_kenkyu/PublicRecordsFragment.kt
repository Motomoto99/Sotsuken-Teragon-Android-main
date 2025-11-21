package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PublicRecordsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recordAdapter: RecordAdapter // RecordFragment.ktにあるアダプターを再利用
    private val recordList = mutableListOf<Record>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_public_records, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.header)
        val buttonBack = view.findViewById<ImageButton>(R.id.buttonBack)

        // ヘッダーのパディング調整
        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        buttonBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        recyclerView = view.findViewById(R.id.recyclerViewPublicRecords)

        // 2列のグリッド表示
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recordAdapter = RecordAdapter(recordList) // RecordFragmentで定義したクラスを使用
        recyclerView.adapter = recordAdapter

        loadPublicRecords()
    }

    private fun loadPublicRecords() {
        val db = FirebaseFirestore.getInstance()

        // 全ユーザーの公開記録を取得 (Collection Group Query)
        db.collectionGroup("my_records")
            .whereEqualTo("isPublic", true)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                recordList.clear()
                for (document in result) {
                    try {
                        val record = document.toObject(Record::class.java)
                        recordList.add(record)
                    } catch (e: Exception) {
                        Log.e("PublicRecords", "Error parsing record", e)
                    }
                }
                recordAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("PublicRecords", "Error loading records", e)
            }
    }
}