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
        // レシピ一覧と同じレイアウトを使い回せますが、タイトルなどが違うので
        // 新しく `fragment_sub_category.xml` を作るのが綺麗です。
        // (今回はリスト表示だけなので、RecipeListFragmentと同じレイアウト構成でOK)
        return inflater.inflate(R.layout.fragment_recipe_list_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleText: TextView = view.findViewById(R.id.textPageTitle)
        val backButton: ImageButton = view.findViewById(R.id.buttonBack)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewRecipes) // IDは使い回し

        // タイトル設定 ("お肉系 から探す" など)
        titleText.text = "$parentCategoryName から探す"

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 中分類データの取得
        val subCategories = getSubCategories(parentCategoryId ?: "")

        // リスト表示
        recyclerView.layoutManager = LinearLayoutManager(context)
        // ※アダプターは後で作ります
        recyclerView.adapter = SubCategoryAdapter(subCategories) { subCatId, subCatName ->
            // 中分類がクリックされたら、レシピ一覧へ遷移
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

    // ★ここが重要！ 大分類IDごとの中分類データ定義
    // 楽天レシピの公式IDに合わせています
    // 楽天レシピの公式IDに基づいた中分類データ
    // ★ここを修正・追記
    private fun getSubCategories(parentId: String): List<Pair<String, String>> {
        return when (parentId) {
            // -----------------------
            // 🌍 世界の料理・その他 (GROUP_WORLD)
            // -----------------------
            "GROUP_WORLD" -> listOf(
                "41" to "中華料理",
                "42" to "韓国料理",
                "43" to "イタリア料理",
                "44" to "フランス料理",
                "46" to "エスニック料理・中南米",
                "47" to "沖縄料理",
                "48" to "日本各地の郷土料理"
            )

            // -----------------------
            // 🎉 行事・イベント (GROUP_EVENTS)
            // -----------------------
            "GROUP_EVENTS" -> listOf(
                "50" to "クリスマス",
                "49" to "おせち料理",
                "51" to "ひな祭り",
                "52" to "春（3月～5月）",
                "53" to "夏（6月～8月）",
                "54" to "秋（9月～11月）",
                "55" to "冬（12月～2月）",
                "24" to "その他の行事・イベント"
            )


            // --- 既存のカテゴリ (肉、魚などはそのまま維持) ---
            "10" -> listOf( //肉
                "10-275" to "牛肉",
                "10-276" to "豚肉",
                "10-277" to "鶏肉",
                "10-278" to "ひき肉",
                "10-66"  to "ソーセージ・ウインナー",
                "10-68"  to "ベーコン",
            )

            "11" -> listOf( //魚
                "11-70" to "鮭・サーモン・鮭",
                "11-71" to "いわし",
                "11-72" to "さば",
                "11-73" to "あじ",
                "11-78" to "その他のさかな",
                "11-77" to "マグロ",
                "11-80" to "いか",
                "11-81" to "たこ",
                "11-82" to "貝類"
            )

            "12" -> listOf( //野菜
                "12-96"  to "玉ねぎ",
                "12-95"  to "にんじん",
                "12-100" to "春野菜",
                "12-98"  to "キャベツ"
            )

            "14" -> listOf( //ご飯
                "14-127" to "ピラフ",
                "14-129" to "寿司",
                "14-130" to "丼物",
                "14-126" to "パエリア",
                "14-131" to "チャーハン",
                "14-132" to "炊き込みご飯",
                "14-133" to "おかゆ・雑炊類",
                "14-128" to "その他の○○ライス"
            )

            "15" -> listOf( //パスタ
                "15-137" to "ミートソース",
                "15-139" to "オイル・塩系パスタ",
                "15-138" to "クリーム系パスタ",
                "15-141" to "バジルソース系パスタ",
                "15-142" to "和風パスタ",
                "15-143" to "冷製パスタ"
            )

            "16" -> listOf( //麺類
                "16-153" to "そば",
                "16-156" to "ラーメン",
                "16-155" to "焼きそば",
                "16-154" to "そうめん",
                "16-152" to "うどん",
                "16-385" to "お好み焼き"
            )

            "17" -> listOf( //スープ
                "17-159" to "味噌汁",
                "17-165" to "和風スープ",
                "17-387" to "けんちん汁",
                "17-170" to "クリームスープ",
                "17-169" to "野菜スープ",
                "17-164" to "中華スープ",
                "17-173" to "その他のスープ"
            )

            "18" -> listOf( //サラダ
                "18-419" to "かぼちゃサラダ",
                "18-184" to "素材で選ぶサラダ",
                "18-416" to "春雨サラダ",
                "18-189" to "スパゲティサラダ",
                "18-418" to "コールスロー"
            )

            "23" -> listOf( //鍋
                "23-391" to "おでん",
                "23-392" to "すきやき",
                "23-393" to "もつ鍋",
                "23-398" to "ちゃんこ鍋",
                "23-396" to "湯豆腐"
            )

            "21" -> listOf( //菓子
                "21-208" to "チョコレート",
                "21-206" to "ケーキ",
                "21-204" to "クッキー",
                "21-205" to "チーズケーキ",
                "21-214" to "和菓子"
            )

            "22" -> listOf( //パン
                "22-229" to "惣菜パン",
                "22-222" to "クロワッサン・デニッシュ",
                "22-221" to "菓子パン",
                "22-432" to "サンドイッチ",
                "22-231" to "ヘルシーなパン",
                "22-230" to "その他パン"
            )

            else -> emptyList()
        }
    }
}