package com.example.sotugyo_kenkyu.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.sotugyo_kenkyu.R

class TabContainerFragment : Fragment() {

    companion object {
        private const val ARG_ROOT_FRAGMENT_CLASS = "root_fragment_class"

        // 新しいインスタンスを作るための便利メソッド
        fun newInstance(rootFragmentClass: Class<out Fragment>): TabContainerFragment {
            val fragment = TabContainerFragment()
            val args = Bundle()
            args.putSerializable(ARG_ROOT_FRAGMENT_CLASS, rootFragmentClass)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // さきほど作ったレイアウトを読み込む
        return inflater.inflate(R.layout.fragment_tab_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // まだ中身がなければ、指定された最初の画面（SearchFragmentなど）を表示
        if (childFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            val rootClass = arguments?.getSerializable(ARG_ROOT_FRAGMENT_CLASS) as? Class<out Fragment>
            if (rootClass != null) {
                val rootFragment = rootClass.newInstance()
                childFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, rootFragment)
                    .commit()
            }
        }
    }

    // このタブの中で「戻る」ことができるか確認するメソッド
    fun canGoBack(): Boolean {
        return childFragmentManager.backStackEntryCount > 0
    }

    // このタブの中で「戻る」処理を実行するメソッド
    fun goBack() {
        childFragmentManager.popBackStack()
    }
}