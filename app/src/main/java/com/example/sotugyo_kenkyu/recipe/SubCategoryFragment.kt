package com.example.sotugyo_kenkyu.recipe

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sotugyo_kenkyu.R

// データクラス：絵文字(emoji)はそのまま、色(color)を追加
data class SubCategoryItem(
    val id: String,
    val name: String,
    val emoji: String,
)

class SubCategoryFragment : Fragment() {

    private var parentCategoryId: String? = null
    private var parentCategoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            parentCategoryId = it.getString("PARENT_ID")
            parentCategoryName = it.getString("PARENT_NAME")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recipe_list_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleText: TextView = view.findViewById(R.id.textPageTitle)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewRecipes)

        titleText.text = "$parentCategoryName から探す"

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val subCategories = getSubCategories(parentCategoryId ?: "")

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = SubCategoryAdapter(subCategories) { subCatId, subCatName ->
            val fragment = RecipeListFragment()
            val args = Bundle()
            args.putString("CATEGORY_ID", subCatId)
            args.putString("CATEGORY_NAME", subCatName)
            fragment.arguments = args

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // ★変更: 戻り値の型を List<SubCategoryItem> にし、絵文字を追加
    private fun getSubCategories(parentId: String): List<SubCategoryItem> {
        return when (parentId) {
            // -----------------------
            // 🌍 世界の料理・その他
            // -----------------------
            "GROUP_WORLD" -> listOf(
                SubCategoryItem("41", "中華料理", "🇨🇳",),
                SubCategoryItem("42", "韓国料理", "🇰🇷",),
                SubCategoryItem("43", "イタリア料理", "🇮🇹",),
                SubCategoryItem("44", "フランス料理", "🇫🇷",),
                SubCategoryItem("46", "エスニック料理・中南米", "🌮"),
                SubCategoryItem("47", "沖縄料理", "🌺"),
                SubCategoryItem("48", "日本各地の郷土料理", "🗾")
            )

            // -----------------------
            // 🎉 行事・イベント
            // -----------------------
            "GROUP_EVENTS" -> listOf(
                SubCategoryItem("50", "クリスマス", "🎄"),
                SubCategoryItem("49", "おせち料理", "🍱"),
                SubCategoryItem("51", "ひな祭り", "🎎"),
                SubCategoryItem("52", "春（3月～5月）", "🌸"),
                SubCategoryItem("53", "夏（6月～8月）", "🌻"),
                SubCategoryItem("54", "秋（9月～11月）", "🍁"),
                SubCategoryItem("55", "冬（12月～2月）", "❄️"),
                SubCategoryItem("24", "その他の行事・イベント", "🎉")
            )

            // -----------------------
            // 🍖 肉
            // -----------------------
            "10" -> listOf(
                SubCategoryItem("10-275", "牛肉", "🐄",),
                SubCategoryItem("10-276", "豚肉", "🐖",),
                SubCategoryItem("10-277", "鶏肉", "🐓",),
                SubCategoryItem("10-278", "ひき肉", "🥩",),
                SubCategoryItem("10-66",  "ソーセージ", "🌭",),
                SubCategoryItem("10-68",  "ベーコン", "🥓",)
            )

            // -----------------------
            // 🐟 魚
            // -----------------------
            "11" -> listOf(
                SubCategoryItem("11-70", "鮭・サーモン", "🐟"),
                SubCategoryItem("11-71", "いわし", "🐟"),
                SubCategoryItem("11-72", "さば", "🐟"),
                SubCategoryItem("11-73", "あじ", "🐟"),
                SubCategoryItem("11-78", "その他のさかな", "🐡"),
                SubCategoryItem("11-77", "マグロ", "🍣"),
                SubCategoryItem("11-80", "いか", "🦑"),
                SubCategoryItem("11-81", "たこ", "🐙"),
                SubCategoryItem("11-82", "貝類", "🐚")
            )

            // -----------------------
            // 🥕 野菜
            // -----------------------
            "12" -> listOf(
                SubCategoryItem("12-96",  "玉ねぎ", "🧅"),
                SubCategoryItem("12-95",  "にんじん", "🥕"),
                SubCategoryItem("12-100", "春野菜", "🌱"),
                SubCategoryItem("12-98",  "キャベツ", "🥬")
            )

            // -----------------------
            // 🍚 ご飯
            // -----------------------
            "14" -> listOf(
                SubCategoryItem("14-127", "ピラフ", "🥘"),
                SubCategoryItem("14-129", "寿司", "🍣"),
                SubCategoryItem("14-130", "丼物", "🍚"),
                SubCategoryItem("14-126", "パエリア", "🥘"),
                SubCategoryItem("14-131", "チャーハン", "🍳"),
                SubCategoryItem("14-132", "炊き込みご飯", "🍄"),
                SubCategoryItem("14-133", "おかゆ・雑炊類", "🥣"),
                SubCategoryItem("14-128", "その他の○○ライス", "🍛")
            )

            // -----------------------
            // 🍝 パスタ
            // -----------------------
            "15" -> listOf(
                SubCategoryItem("15-137", "ミートソース", "🍝"),
                SubCategoryItem("15-139", "オイル・塩系パスタ", "🧂"),
                SubCategoryItem("15-138", "クリーム系パスタ", "🥛"),
                SubCategoryItem("15-141", "バジルソース系パスタ", "🌿"),
                SubCategoryItem("15-142", "和風パスタ", "🍄"),
                SubCategoryItem("15-143", "冷製パスタ", "🧊")
            )

            // -----------------------
            // 🍜 麺類
            // -----------------------
            "16" -> listOf(
                SubCategoryItem("16-153", "そば", "🥢"),
                SubCategoryItem("16-156", "ラーメン", "🍜"),
                SubCategoryItem("16-155", "焼きそば", "🍳"),
                SubCategoryItem("16-154", "そうめん", "🎐"),
                SubCategoryItem("16-152", "うどん", "🍲"),
                SubCategoryItem("16-385", "お好み焼き", "🥞")
            )

            // -----------------------
            // 🥣 スープ
            // -----------------------
            "17" -> listOf(
                SubCategoryItem("17-159", "味噌汁", "🥣"),
                SubCategoryItem("17-165", "和風スープ", "🍲"),
                SubCategoryItem("17-387", "けんちん汁", "🥕"),
                SubCategoryItem("17-170", "クリームスープ", "🥛"),
                SubCategoryItem("17-169", "野菜スープ", "🥦"),
                SubCategoryItem("17-164", "中華スープ", "🥟"),
                SubCategoryItem("17-173", "その他のスープ", "🍵")
            )

            // -----------------------
            // 🥗 サラダ
            // -----------------------
            "18" -> listOf(
                SubCategoryItem("18-419", "かぼちゃサラダ", "🎃"),
                SubCategoryItem("18-184", "素材で選ぶサラダ", "🥗"),
                SubCategoryItem("18-416", "春雨サラダ", "🥣"),
                SubCategoryItem("18-189", "スパゲティサラダ", "🍝"),
                SubCategoryItem("18-418", "コールスロー", "🥬")
            )

            // -----------------------
            // 🍲 鍋
            // -----------------------
            "23" -> listOf(
                SubCategoryItem("23-391", "おでん", "🍢"),
                SubCategoryItem("23-392", "すきやき", "🥩"),
                SubCategoryItem("23-393", "もつ鍋", "🥘"),
                SubCategoryItem("23-398", "ちゃんこ鍋", "🍲"),
                SubCategoryItem("23-396", "湯豆腐", "♨️")
            )

            // -----------------------
            // 🍰 菓子
            // -----------------------
            "21" -> listOf(
                SubCategoryItem("21-208", "チョコレート", "🍫"),
                SubCategoryItem("21-206", "ケーキ", "🍰"),
                SubCategoryItem("21-204", "クッキー", "🍪"),
                SubCategoryItem("21-205", "チーズケーキ", "🧀"),
                SubCategoryItem("21-214", "和菓子", "🍡")
            )

            // -----------------------
            // 🍞 パン
            // -----------------------
            "22" -> listOf(
                SubCategoryItem("22-229", "惣菜パン", "🌭"),
                SubCategoryItem("22-222", "クロワッサン・デニッシュ", "🥐"),
                SubCategoryItem("22-221", "菓子パン", "🍩"),
                SubCategoryItem("22-432", "サンドイッチ", "🥪"),
                SubCategoryItem("22-231", "ヘルシーなパン", "🍞"),
                SubCategoryItem("22-230", "その他パン", "🥖")
            )

            else -> emptyList()
        }
    }
}