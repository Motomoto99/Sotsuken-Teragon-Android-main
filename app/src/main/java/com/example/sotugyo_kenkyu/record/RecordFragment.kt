package com.example.sotugyo_kenkyu.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

        ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
            v.updatePadding(top = systemBars.top + originalPaddingTop)
            insets
        }

        recyclerView = view.findViewById(R.id.recyclerViewRecord)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fabAddRecord)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // ★修正: 引数なし（リストのみ）で初期化
        recordAdapter = RecordAdapter(recordList)
        recyclerView.adapter = recordAdapter

        fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), RecordInputActivity::class.java)
            startActivity(intent)
        }

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