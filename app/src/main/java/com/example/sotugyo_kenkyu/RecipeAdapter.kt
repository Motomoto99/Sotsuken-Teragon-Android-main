package com.example.sotugyo_kenkyu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeAdapter(
    private val recipeList: List<Recipe>,
    private val onFavoriteClick: (Recipe) -> Unit,
    private val onItemClick: (Recipe) -> Unit // ★追加：項目クリック用
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageFood: ImageView = view.findViewById(R.id.imageFood)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val buttonFavorite: ImageButton = view.findViewById(R.id.buttonFavorite)
        val container: View = view // ★追加：クリック範囲用
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
                .into(holder.imageFood)
        }

        holder.buttonFavorite.setOnClickListener {
            onFavoriteClick(recipe)
        }

        // ★追加：項目全体のクリックリスナー
        holder.container.setOnClickListener {
            onItemClick(recipe)
        }
    }

    override fun getItemCount(): Int = recipeList.size
}