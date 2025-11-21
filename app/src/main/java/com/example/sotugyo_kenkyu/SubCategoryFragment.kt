package com.example.sotugyo_kenkyu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

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

            // -----------------------
            // 🍩 お菓子 (ID: 21)
            // -----------------------
            "21" -> listOf(
                "21-237" to "チョコレート",
                "21-238" to "ケーキ",
                "21-239" to "クッキー",
                "21-240" to "チーズケーキ",
                "21-241" to "和菓子",
                "21-242" to "冷たいお菓子",
                "21-395" to "その他のお菓子"
            )

            // -----------------------
            // 🍞 パン (ID: 22)
            // -----------------------
            "22" -> listOf(
                "22-229" to "惣菜パン",
                "22-222" to "クロワッサン・デニッシュ",
                "22-221" to "菓子パン",
                "22-432" to "サンドイッチ",
                "22-231" to "ヘルシーなパン",
                "22-230" to "その他パン"
            )

            // --- 既存のカテゴリ (肉、魚などはそのまま維持) ---
            "10" -> listOf(
                "10-275" to "牛肉",
                "10-276" to "豚肉",
                "10-277" to "鶏肉",
                "10-278" to "ひき肉",
                "10-279" to "ハム・ソーセージ",
                "10-280" to "ベーコン",
                "10-68"  to "ラム肉"
            )
            "11" -> listOf(
                "11-70" to "鮭・サーモン",
                "11-71" to "いわし",
                "11-72" to "サバ",
                "11-73" to "アジ",
                "11-74" to "ブリ",
                "11-78" to "マグロ",
                "11-77" to "エビ",
                "11-80" to "イカ",
                "11-81" to "タコ",
                "11-82" to "貝類"
            )
            "12" -> listOf(
                "12-96"  to "根菜類（大根・人参など）",
                "12-95"  to "葉野菜（キャベツ・白菜など）",
                "12-97"  to "実野菜（トマト・ピーマンなど）",
                "12-99"  to "きのこ類",
                "12-405" to "イモ類",
                "12-100" to "豆類",
                "12-98"  to "季節の野菜"
            )
            "14" -> listOf(
                "14-127" to "丼もの",
                "14-128" to "チャーハン",
                "14-129" to "炊き込みご飯",
                "14-130" to "寿司",
                "14-126" to "カレー",
                "14-131" to "オムライス",
                "14-132" to "リゾット・ドリア",
                "14-133" to "おにぎり"
            )
            "15" -> listOf(
                "15-136" to "トマト系パスタ",
                "15-135" to "クリーム系パスタ",
                "15-137" to "オイル系・塩パスタ",
                "15-139" to "和風パスタ",
                "15-138" to "ジェノベーゼ",
                "15-141" to "冷製パスタ",
                "15-142" to "グラタン",
                "15-143" to "ラザニア"
            )
            "16" -> listOf(
                "16-144" to "うどん",
                "16-153" to "そば",
                "16-156" to "ラーメン",
                "16-155" to "焼きそば",
                "16-154" to "そうめん",
                "16-152" to "冷やし中華",
                "16-385" to "お好み焼き"
            )
            "17" -> listOf(
                "17-159" to "味噌汁",
                "17-165" to "和風スープ",
                "17-387" to "けんちん汁",
                "17-170" to "クリームスープ",
                "17-169" to "野菜スープ",
                "17-164" to "中華スープ",
                "17-173" to "その他のスープ"
            )
            "18" -> listOf(
                "18-419" to "かぼちゃサラダ",
                "18-184" to "素材で選ぶサラダ",
                "18-164" to "マカロニサラダ",
                "18-416" to "春雨サラダ",
                "18-189" to "スパゲティサラダ",
                "18-418" to "コールスロー"
            )
            "19" -> listOf(
                "19-193" to "タレ",
                "19-192" to "ソース",
                "19-194" to "つゆ",
                "19-196" to "ドレッシング"
            )

            else -> emptyList()
        }
    }
}