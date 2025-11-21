package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoriteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var db: FirebaseFirestore
    private val favoriteList = mutableListOf<Recipe>()
    private lateinit var adapter: RecipeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 検索画面と同じレイアウトでOKですが、タイトルなどが違うので
        // もし専用のレイアウト (fragment_favorite.xml) を作っているならそちらを使ってください
        // ここでは既存のレイアウトを活用する例を書きます
        return inflater.inflate(R.layout.fragment_favorite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = view.findViewById<View>(R.id.header) // xmlに追加が必要(後述)

        // WindowInsets調整
        if (header != null) {
            ViewCompat.setOnApplyWindowInsetsListener(header) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val originalPaddingTop = (16 * resources.displayMetrics.density).toInt()
                v.updatePadding(top = systemBars.top + originalPaddingTop)
                insets
            }
        }

        recyclerView = view.findViewById(R.id.recyclerViewFavorites)
        recyclerView.layoutManager = LinearLayoutManager(context)

        db = FirebaseFirestore.getInstance()

        // アダプター設定
        adapter = RecipeAdapter(
            favoriteList,
            onFavoriteClick = { recipe ->
                // お気に入り画面で星を外したら、リストから消す処理
                removeFavorite(recipe)
            },
            onItemClick = { recipe ->
                val fragment = RecipeDetailFragment()
                val args = Bundle()
                args.putSerializable("RECIPE_DATA", recipe)
                fragment.arguments = args

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        recyclerView.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("favorites")
            .get()
            .addOnSuccessListener { result ->
                favoriteList.clear()
                for (document in result) {
                    val recipe = document.toObject(Recipe::class.java)
                    recipe.id = document.id
                    recipe.isFavorite = true // ここにある時点で絶対にお気に入り
                    favoriteList.add(recipe)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(context, "読み込み失敗", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFavorite(recipe: Recipe) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // DBから削除
        db.collection("users")
            .document(user.uid)
            .collection("favorites")
            .document(recipe.id)
            .delete()
            .addOnSuccessListener {
                // 画面のリストからも削除して更新
                val position = favoriteList.indexOf(recipe)
                if (position != -1) {
                    favoriteList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                }
                Toast.makeText(context, "お気に入りを解除しました", Toast.LENGTH_SHORT).show()
            }
    }
}