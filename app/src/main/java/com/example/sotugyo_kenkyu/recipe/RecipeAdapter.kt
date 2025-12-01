package com.example.sotugyo_kenkyu.recipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sotugyo_kenkyu.R

class RecipeAdapter(
    private var recipeList: List<Recipe>,
    private val onFavoriteClick: (Recipe) -> Unit, // お気に入りボタンの処理
    private val onItemClick: (Recipe) -> Unit      // 全体クリックの処理
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    // これが「新しいデータを受け取って、画面を更新する」ためのスイッチよ
    fun updateData(newList: List<Recipe>) {
        recipeList = newList
        notifyDataSetChanged() // ← これが「表示を更新しろ！」っていう命令
    }

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageFood: ImageView = view.findViewById(R.id.imageFood)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val buttonFavorite: ImageButton = view.findViewById(R.id.buttonFavorite)
        val container: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_list, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.textTitle.text = recipe.recipeTitle
        if (recipe.categoryPathNames.isNotEmpty()) {
            holder.textCategory.text = recipe.categoryPathNames.last()
        } else {
            holder.textCategory.text = ""
        }

        if (recipe.foodImageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(recipe.foodImageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop() // 画像を綺麗に切り抜く
                .into(holder.imageFood)
        }

        // ★ 星アイコンの表示切り替え
        updateFavoriteIcon(holder.buttonFavorite, recipe.isFavorite)

        // ★ 星ボタンのクリック処理
        holder.buttonFavorite.setOnClickListener {
            // 1. 見た目を即座に反転させる（ユーザーへのレスポンス重視）
            recipe.isFavorite = !recipe.isFavorite
            updateFavoriteIcon(holder.buttonFavorite, recipe.isFavorite)

            // 2. 親側の処理（Firestore更新など）を呼ぶ
            onFavoriteClick(recipe)
        }

        holder.container.setOnClickListener {
            onItemClick(recipe)
        }
    }

    // 星の色を変える便利関数
    private fun updateFavoriteIcon(button: ImageButton, isFavorite: Boolean) {
        val context = button.context
        if (isFavorite) {
            // お気に入り済み：
            // 1. アイコンを「塗りつぶし星」にする
            button.setImageResource(R.drawable.ic_star_filled)
            // 2. 色を「ゴールド（黄色）」にする
            button.setColorFilter(ContextCompat.getColor(context, R.color.gold))
        } else {
            // 未登録：
            // 1. アイコンを「枠線だけの星」に戻す
            button.setImageResource(R.drawable.ic_star_outline)
            // 2. 色を「グレー」にする
            button.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
    }

    override fun getItemCount(): Int = recipeList.size
}