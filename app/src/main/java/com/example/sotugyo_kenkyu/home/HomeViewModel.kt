package com.example.sotugyo_kenkyu.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sotugyo_kenkyu.record.Record
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {

    private val _myRecords = MutableLiveData<List<Record>>()
    val myRecords: LiveData<List<Record>> = _myRecords

    private val _publicRecords = MutableLiveData<List<Record>>()
    val publicRecords: LiveData<List<Record>> = _publicRecords

    private val _userIconUrl = MutableLiveData<String?>()
    val userIconUrl: LiveData<String?> = _userIconUrl

    // ★追加: 読み込み中フラグ（くるくるの制御用）
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var isInitialized = false

    fun loadDataIfNeeded() {
        if (isInitialized) return
        refresh() // 初回もリフレッシュ処理を呼ぶ形に統一
    }

    // ★追加: 強制更新メソッド
    fun refresh() {
        isInitialized = true
        _isLoading.value = true

        viewModelScope.launch {
            // 3つの処理を並列で実行し、すべて終わるのを待つ
            joinAll(
                launch { loadUserIcon() },
                launch { loadRecentRecords() },
                launch { loadEveryoneRecords() }
            )
            // すべて終わったらくるくるを消す
            _isLoading.value = false
        }
    }

    // ★変更: suspend関数に変更し、内部のviewModelScope.launchを削除
    private suspend fun loadUserIcon() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        try {
            val document = db.collection("users").document(user.uid).get().await()
            val url = document.getString("photoUrl")
            _userIconUrl.postValue(url) // Backgroundスレッドから呼ぶのでpostValue
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadRecentRecords() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        try {
            val snapshot = db.collection("users").document(user.uid).collection("my_records")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(2)
                .get().await()
            _myRecords.postValue(snapshot.toObjects(Record::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadEveryoneRecords() {
        val db = FirebaseFirestore.getInstance()
        try {
            val snapshot = db.collectionGroup("my_records")
                .whereEqualTo("isPublic", true)
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(2)
                .get().await()
            _publicRecords.postValue(snapshot.toObjects(Record::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}