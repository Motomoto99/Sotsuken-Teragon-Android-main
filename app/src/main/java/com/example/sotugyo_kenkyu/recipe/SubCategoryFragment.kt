package com.example.sotugyo_kenkyu.recipe

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
    val imageFileName: String? = null
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
                SubCategoryItem("41", "中華料理", "🇨🇳","41.png"),
                SubCategoryItem("42", "韓国料理", "🇰🇷","42.png"),
                SubCategoryItem("43", "イタリア料理", "🇮🇹","43.png"),
                SubCategoryItem("44", "フランス料理", "🇫🇷","44.png"),
                SubCategoryItem("46", "エスニック料理・中南米", "🌮","46.png"),
                SubCategoryItem("47", "沖縄料理", "🌺","47.png"),
                SubCategoryItem("48", "日本各地の郷土料理", "🗾","48.png")
            )

            // -----------------------
            // 🎉 行事・イベント
            // -----------------------
            "GROUP_EVENTS" -> listOf(
                SubCategoryItem("50", "クリスマス", "🎄","50.png"),
                SubCategoryItem("49", "おせち料理", "🍱","49.png"),
                SubCategoryItem("51", "ひな祭り", "🎎","51.png"),
                SubCategoryItem("52", "春（3月～5月）", "🌸","52.png"),
                SubCategoryItem("53", "夏（6月～8月）", "🌻","53.png"),
                SubCategoryItem("54", "秋（9月～11月）", "🍁","54.png"),
                SubCategoryItem("55", "冬（12月～2月）", "❄️","55.png"),
                SubCategoryItem("24", "その他の行事・イベント", "🎉","24.png")
            )

            // -----------------------
            // 🍖 肉
            // -----------------------
            "10" -> listOf(
                SubCategoryItem("10-275", "牛肉", "🐄","10-275.png"),
                SubCategoryItem("10-276", "豚肉", "🐖","10-276.png"),
                SubCategoryItem("10-277", "鶏肉", "🐓","10-277.png"),
                SubCategoryItem("10-278", "ひき肉", "🥩","10-278.png"),
                SubCategoryItem("10-66",  "ソーセージ", "🌭","10-66.png"),
                SubCategoryItem("10-68",  "ベーコン", "🥓","10-68.png")
            )

            // -----------------------
            // 🐟 魚
            // -----------------------
            "11" -> listOf(
                SubCategoryItem("11-70", "鮭・サーモン", "🐟", "11-70.png"),
                SubCategoryItem("11-71", "いわし", "🐟","11-71.png"),
                SubCategoryItem("11-72", "さば", "🐟","11-72.png"),
                SubCategoryItem("11-73", "あじ", "🐟","11-73.png"),
                SubCategoryItem("11-77", "マグロ", "🍣","11-77.png"),
                SubCategoryItem("11-78", "その他のさかな", "🐡","11-78.png"),
                SubCategoryItem("11-80", "いか", "🦑","11-80.png"),
                SubCategoryItem("11-81", "たこ", "🐙","11-81.png"),
                SubCategoryItem("11-82", "貝類", "🐚","11-82.png")
            )

            // -----------------------
            // 🥕 野菜
            // -----------------------
            "12" -> listOf(
                SubCategoryItem("12-96",  "玉ねぎ", "🧅","12-96.png"),
                SubCategoryItem("12-95",  "にんじん", "🥕","12-95.png"),
                SubCategoryItem("12-100", "春野菜", "🌱","12-100.png"),
                SubCategoryItem("12-98",  "キャベツ", "🥬","12-98.png")
            )

            // -----------------------
            // 🍚 ご飯
            // -----------------------
            "14" -> listOf(
                SubCategoryItem("14-127", "ピラフ", "🥘","14-127.png"),
                SubCategoryItem("14-129", "寿司", "🍣","14-129.png"),
                SubCategoryItem("14-130", "丼物", "🍚","14-130.png"),
                SubCategoryItem("14-126", "パエリア", "🥘","14-126.png"),
                SubCategoryItem("14-131", "チャーハン", "🍳","14-131.png"),
                SubCategoryItem("14-132", "炊き込みご飯", "🍄","14-132.png"),
                SubCategoryItem("14-133", "おかゆ・雑炊類", "🥣","14-133.png"),
                SubCategoryItem("14-128", "その他の○○ライス", "🍛","14-128.png")
            )

            // -----------------------
            // 🍝 パスタ
            // -----------------------
            "15" -> listOf(
                SubCategoryItem("15-137", "ミートソース", "🍝","15-137.png"),
                SubCategoryItem("15-139", "オイル・塩系パスタ", "🧂","15-139.png"),
                SubCategoryItem("15-138", "クリーム系パスタ", "🥛","15-138.png"),
                SubCategoryItem("15-141", "バジルソース系パスタ", "🌿","15-141.png"),
                SubCategoryItem("15-142", "和風パスタ", "🍄","15-142.png"),
                SubCategoryItem("15-143", "冷製パスタ", "🧊","15-143.png")
            )

            // -----------------------
            // 🍜 麺類
            // -----------------------
            "16" -> listOf(
                SubCategoryItem("16-153", "そば", "🥢","16-153.png"),
                SubCategoryItem("16-156", "ラーメン", "🍜","16-156.png"),
                SubCategoryItem("16-155", "焼きそば", "🍳","16-155.png"),
                SubCategoryItem("16-154", "そうめん", "🎐","16-154.png"),
                SubCategoryItem("16-152", "うどん", "🍲","16-152.png"),
                SubCategoryItem("16-385", "お好み焼き", "🥞","16-385.png")
            )

            // -----------------------
            // 🥣 スープ
            // -----------------------
            "17" -> listOf(
                SubCategoryItem("17-159", "味噌汁", "🥣","17-159.png"),
                SubCategoryItem("17-165", "和風スープ", "🍲","17-165.png"),
                SubCategoryItem("17-387", "けんちん汁", "🥕","17-387.png"),
                SubCategoryItem("17-170", "クリームスープ", "🥛","17-170.png"),
                SubCategoryItem("17-169", "野菜スープ", "🥦","17-169.png"),
                SubCategoryItem("17-164", "中華スープ", "🥟","17-164.png"),
                SubCategoryItem("17-173", "その他のスープ", "🍵","17-173.png")
            )

            // -----------------------
            // 🥗 サラダ
            // -----------------------
            "18" -> listOf(
                SubCategoryItem("18-419", "かぼちゃサラダ", "🎃","18-419.png"),
                SubCategoryItem("18-184", "素材で選ぶサラダ", "🥗","18-184.png"),
                SubCategoryItem("18-416", "春雨サラダ", "🥣","18-416.png"),
                SubCategoryItem("18-189", "スパゲティサラダ", "🍝","18-189.png"),
                SubCategoryItem("18-418", "コールスロー", "🥬","18-418.png")
            )

            // -----------------------
            // 🍲 鍋
            // -----------------------
            "23" -> listOf(
                SubCategoryItem("23-391", "おでん", "🍢","23-391.png"),
                SubCategoryItem("23-392", "すきやき", "🥩","23-392.png"),
                SubCategoryItem("23-393", "もつ鍋", "🥘","23-393.png"),
                SubCategoryItem("23-398", "ちゃんこ鍋", "🍲","23-398.png"),
                SubCategoryItem("23-396", "湯豆腐", "♨️","23-396.png")
            )

            // -----------------------
            // 🍰 菓子
            // -----------------------
            "21" -> listOf(
                SubCategoryItem("21-208", "チョコレート", "🍫","21-208.png"),
                SubCategoryItem("21-206", "ケーキ", "🍰","21-206.png"),
                SubCategoryItem("21-204", "クッキー", "🍪","21-204.png"),
                SubCategoryItem("21-205", "チーズケーキ", "🧀","21-205.png"),
                SubCategoryItem("21-214", "和菓子", "🍡","21-214.png")
            )

            // -----------------------
            // 🍞 パン
            // -----------------------
            "22" -> listOf(
                SubCategoryItem("22-229", "惣菜パン", "🌭","22-229.png"),
                SubCategoryItem("22-222", "クロワッサン・デニッシュ", "🥐","22-222.png"),
                SubCategoryItem("22-221", "菓子パン", "🍩","22-221.png"),
                SubCategoryItem("22-432", "サンドイッチ", "🥪","22-432.png"),
                SubCategoryItem("22-231", "ヘルシーなパン", "🍞","22-231.png"),
                SubCategoryItem("22-230", "その他パン", "🥖","22-230.png")
            )

            else -> emptyList()
        }
    }
}