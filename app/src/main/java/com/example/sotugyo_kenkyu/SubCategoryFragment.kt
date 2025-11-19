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
    private fun getSubCategories(parentId: String): List<Pair<String, String>> {
        return when (parentId) {
            // -----------------------
            // 🍖 お肉 (ID: 10)
            // -----------------------
            "10" -> listOf(
                "10-275" to "牛肉",
                "10-276" to "豚肉",
                "10-277" to "鶏肉",
                "10-278" to "ひき肉",
                "10-279" to "ハム・ソーセージ",
                "10-280" to "ベーコン",
                "10-68"  to "ラム肉"
            )

            // -----------------------
            // 🥦 野菜 (ID: 12)
            // -----------------------
            "12" -> listOf(
                "12-96"  to "根菜類（大根・人参など）",
                "12-95"  to "葉野菜（キャベツ・白菜など）",
                "12-97"  to "実野菜（トマト・ピーマンなど）",
                "12-99"  to "きのこ類",
                "12-405" to "イモ類（じゃがいも・里芋など）",
                "12-100" to "豆類",
                "12-98"  to "春野菜・夏野菜・秋野菜・冬野菜"
            )

            // -----------------------
            // 🐟 魚介 (ID: 11)
            // -----------------------
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

            // -----------------------
            // 🍚 ごはんもの (ID: 14)
            // -----------------------
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

            // -----------------------
            // 🍝 パスタ (ID: 15)
            // -----------------------
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

            // -----------------------
            // 🍜 麺類 (ID: 16)
            // -----------------------
            "16" -> listOf(
                "16-144" to "うどん",
                "16-145" to "そば",
                "16-146" to "ラーメン",
                "16-147" to "焼きそば",
                "16-148" to "そうめん",
                "16-152" to "冷やし中華",
                "16-153" to "フォー・ビーフン"
            )

            // -----------------------
            // 🥣 スープ・汁物 (ID: 17)
            // -----------------------
            "17" -> listOf(
                "17-155" to "味噌汁",
                "17-156" to "お吸い物",
                "17-157" to "スープ",
                "17-158" to "シチュー",
                "17-436" to "ポタージュ",
                "17-160" to "豚汁・けんちん汁",
                "17-161" to "中華スープ"
            )

            // -----------------------
            // 🥗 サラダ (ID: 18)
            // -----------------------
            "18" -> listOf(
                "18-163" to "野菜サラダ",
                "18-162" to "ポテトサラダ",
                "18-164" to "マカロニサラダ",
                "18-166" to "春雨サラダ",
                "18-167" to "豆腐サラダ",
                "18-165" to "かぼちゃサラダ",
                "18-168" to "豆サラダ"
            )

            // -----------------------
            // 🍞 パン (ID: 19)
            // -----------------------
            "19" -> listOf(
                "19-176" to "サンドイッチ",
                "19-177" to "トースト",
                "19-179" to "フレンチトースト",
                "19-172" to "食パン・テーブルパン",
                "19-173" to "菓子パン",
                "19-174" to "おかずパン",
                "19-175" to "ベーグル"
            )

            // -----------------------
            // 🍰 お菓子 (ID: 21)
            // -----------------------
            "21" -> listOf(
                "21-188" to "ケーキ",
                "21-193" to "クッキー・ビスケット",
                "21-194" to "チョコレート",
                "21-198" to "プリン・ゼリー",
                "21-200" to "アイスクリーム",
                "21-186" to "和菓子",
                "21-196" to "パンケーキ・ホットケーキ"
            )

            else -> emptyList()
        }
    }
}