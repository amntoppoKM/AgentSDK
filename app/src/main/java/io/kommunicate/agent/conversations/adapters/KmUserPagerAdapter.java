package io.kommunicate.agent.conversations.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

import io.kommunicate.agent.conversations.fragments.KmUserListFragment;

public class KmUserPagerAdapter extends FragmentStatePagerAdapter {

    private List<KmUserListFragment> fragmentList;
    private List<String> titleList;

    public KmUserPagerAdapter(FragmentManager fm, List<String> titleList) {
        super(fm);
        fragmentList = new ArrayList<>();
        this.titleList = titleList;
    }

    @Override
    public KmUserListFragment getItem(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getCount() {
        return titleList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position);
    }

    public void addFragment(KmUserListFragment userListFragment) {
        if (fragmentList == null) {
            fragmentList = new ArrayList<>();
        }
        if (!fragmentList.contains(userListFragment)) {
            fragmentList.add(userListFragment);
        }
    }

    public void setSearchText(String searchText, int position) {
        getItem(position).setSearchText(searchText);
    }

    public void setErrorText(int position) {
        getItem(position).showEmptyListText(true, false);
    }
}
