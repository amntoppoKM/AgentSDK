package io.kommunicate.agent.adapters

import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.kommunicate.agent.conversations.fragments.KmConversationListFragment

class KmConversationPagerAdapter(private val context: Context, private val titleList: List<String>, private val fragmentManager: FragmentManager) : FragmentStatePagerAdapter(fragmentManager) {

    private var fragmentList = arrayListOf<KmConversationListFragment>()

    fun addFragment(fragment: KmConversationListFragment) {
        fragmentList.add(fragment)
    }

    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getItem(position: Int): KmConversationListFragment {
        return fragmentList[position]
    }

    override fun getPageTitle(position: Int): CharSequence {
        return titleList[position]
    }
}