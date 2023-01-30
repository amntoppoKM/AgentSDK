package io.kommunicate.agent.conversations.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.channel.Channel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.kommunicate.agent.KmAssigneeListHelper;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmConversationStatusListener;
import io.kommunicate.agent.conversations.adapters.KmUserPagerAdapter;
import io.kommunicate.agent.databinding.KmAssigneeListLayoutBinding;
import io.kommunicate.agent.model.KmAgentContact;
import io.kommunicate.agent.conversations.viewmodels.KmAppSettingsViewModel;
import io.kommunicate.agent.viewmodels.KmTeamViewModel;
import io.kommunicate.callbacks.KmCallback;


public class KmAssigneeListFragment extends BottomSheetDialogFragment implements TabLayout.OnTabSelectedListener, SearchView.OnQueryTextListener, KmConversationStatusListener {
    private static final String TAG = "KmAssigneeListFragment";
    private String assigneeId;
    private String teamId;
    private KmAssigneeListLayoutBinding binding;
    private KmUserPagerAdapter pagerAdapter;
    private String searchText;
    private int channelId;
    private KmTeamViewModel teamViewModel;
    private KmConversationStatusListener conversationStatusListener;

    public static String getFragTag() {
        return TAG;
    }

    public static KmAssigneeListFragment newInstance(String assigneeId, String teamId, int channelId) {
        KmAssigneeListFragment assigneeListFragment = new KmAssigneeListFragment();
        Bundle args = new Bundle();
        args.putString(Channel.CONVERSATION_ASSIGNEE, assigneeId);
        args.putString(Channel.KM_TEAM_ID, teamId);
        args.putInt(ConversationUIService.GROUP_ID, channelId);
        assigneeListFragment.setArguments(args);
        return assigneeListFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            assigneeId = bundle.getString(Channel.CONVERSATION_ASSIGNEE, null);
            teamId = bundle.getString(Channel.KM_TEAM_ID, null);
            channelId = bundle.getInt(ConversationUIService.GROUP_ID);
        }
    }

    public void setConversationStatusListener(KmConversationStatusListener conversationStatusListener) {
        this.conversationStatusListener = conversationStatusListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.km_assignee_list_layout, container, false);

        binding = DataBindingUtil.bind(view.findViewById(R.id.kmAssigneeListLayout));
        List<String> titleList = new ArrayList<>();

        teamViewModel = new ViewModelProvider(requireActivity()).get(KmTeamViewModel.class);

        if (binding != null) {
            binding.setAssigneeListFragment(this);
            pagerAdapter = new KmUserPagerAdapter(getChildFragmentManager(), titleList);

            KmUserListFragment agentsFragment = KmUserListFragment.newInstance(assigneeId, KmUserListFragment.AGENT_TAB, channelId, null, KmAssigneeListHelper.getAssigneeList(KmAssigneeListHelper.AGENT_LIST_CODE));
            KmUserListFragment botsFragment = KmUserListFragment.newInstance(assigneeId, KmUserListFragment.BOT_TAB, channelId, null, KmAssigneeListHelper.getAssigneeList(KmAssigneeListHelper.BOT_LIST_CODE));
            KmUserListFragment teamsFragment = KmUserListFragment.newInstance(teamId, KmUserListFragment.TEAMS_TAB, channelId, KmAssigneeListHelper.getTeamList(), null);

            if (KmAppSettingsViewModel.AppSettingsCache.isOperator()) {
                if (KmAppSettingsViewModel.AppSettingsCache.getAllowTeamMateAssignment()) {
                    titleList.add(getString(R.string.km_agent_list_title));
                    agentsFragment.setConversationStatusListener(this);
                    pagerAdapter.addFragment(agentsFragment);
                    updateAgentsList(titleList);
                }
                if (KmAppSettingsViewModel.AppSettingsCache.getAllowBotAssignment()) {
                    titleList.add(getString(R.string.km_bot_list_title));
                    botsFragment.setConversationStatusListener(this);
                    pagerAdapter.addFragment(botsFragment);
                    updateBotsList(titleList);
                }
                if (KmAppSettingsViewModel.AppSettingsCache.getAllowTeamAssignment()) {
                    titleList.add(getString(R.string.km_team_list_title));
                    teamsFragment.setConversationStatusListener(this);
                    pagerAdapter.addFragment(teamsFragment);
                    updateTeamsList(teamViewModel, titleList);
                }
            } else {

                titleList.addAll(new ArrayList<>(Arrays.asList(
                        getString(R.string.km_agent_list_title),
                        getString(R.string.km_bot_list_title),
                        getString(R.string.km_team_list_title)
                )));

                agentsFragment.setConversationStatusListener(this);
                botsFragment.setConversationStatusListener(this);
                teamsFragment.setConversationStatusListener(this);

                pagerAdapter.addFragment(agentsFragment);
                pagerAdapter.addFragment(botsFragment);
                pagerAdapter.addFragment(teamsFragment);
                updateAgentsList(titleList);
                updateBotsList(titleList);
                updateTeamsList(teamViewModel, titleList);
            }
            binding.kmAssigneeSearchView.setOnQueryTextListener(this);
            binding.kmAssigneeViewPager.setAdapter(pagerAdapter);
            binding.kmAssignTabLayout.setupWithViewPager(binding.kmAssigneeViewPager);
            binding.kmAssignTabLayout.addOnTabSelectedListener(this);
        }
        return view;
    }

    private void updateTeamsList(KmTeamViewModel teamViewModel, List<String> titleList) {
        if (KmAssigneeListHelper.isListEmpty(KmAssigneeListHelper.TEAM_LIST_CODE)) {

            teamViewModel.getTeamList().observe(this, listResource -> {
                if (listResource.isSuccess() && listResource.getData() != null) {
                    KmAssigneeListHelper.addTeamList(listResource.getData());
                    pagerAdapter.getItem(getTabFragmentPosition(KmUserListFragment.TEAMS_TAB, titleList)).addTeamList(listResource.getData());
                }
            });
        }
    }

    private void updateBotsList(List<String> titleList) {
        if (KmAssigneeListHelper.isListEmpty(KmAssigneeListHelper.BOT_LIST_CODE)) {

            KmAssigneeListHelper.fetchBotList(getContext(), new KmCallback() {
                @Override
                public void onSuccess(Object message) {
                    KmAssigneeListHelper.addAssigneeList(KmAssigneeListHelper.BOT_LIST_CODE, (List<KmAgentContact>) message);
                    pagerAdapter.getItem(getTabFragmentPosition(KmUserListFragment.BOT_TAB, titleList)).addUserList((List<KmAgentContact>) message);
                }

                @Override
                public void onFailure(Object error) {
                    pagerAdapter.setErrorText(getTabFragmentPosition(KmUserListFragment.BOT_TAB, titleList));
                    KmToast.error(getContext(), Utils.getString(getContext(), R.string.km_server_error) + " :" + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateAgentsList(List<String> titleList) {
        if (KmAssigneeListHelper.isListEmpty(KmAssigneeListHelper.AGENT_LIST_CODE)) {
            KmAssigneeListHelper.fetchAgentList(getContext(), new KmCallback() {
                @Override
                public void onSuccess(Object message) {
                    KmAssigneeListHelper.addAssigneeList(KmAssigneeListHelper.AGENT_LIST_CODE, (List<KmAgentContact>) message);
                    pagerAdapter.getItem(getTabFragmentPosition(KmUserListFragment.AGENT_TAB, titleList)).addUserList((List<KmAgentContact>) message);
                }

                @Override
                public void onFailure(Object error) {
                    pagerAdapter.setErrorText(getTabFragmentPosition(KmUserListFragment.AGENT_TAB, titleList));
                    KmToast.error(getContext(), Utils.getString(getContext(), R.string.km_server_error) + " :" + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public int getTabFragmentPosition(String tabType, List<String> titleList) {
        int position = 0;
        switch (tabType) {
            case KmUserListFragment.AGENT_TAB:
                position = titleList.indexOf(getString(R.string.km_agent_list_title));
                break;
            case KmUserListFragment.BOT_TAB:
                position = titleList.indexOf(getString(R.string.km_bot_list_title));
                break;
            case KmUserListFragment.TEAMS_TAB:
                position = titleList.indexOf(getString(R.string.km_team_list_title));
                break;
        }
        return position;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getDialog() != null) {
            getDialog().setOnShowListener((DialogInterface.OnShowListener) dialog -> {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog;
                FrameLayout bottomSheet = (FrameLayout) bottomSheetDialog.findViewById(R.id.design_bottom_sheet);

                if (bottomSheet != null) {
                    CoordinatorLayout coordinatorLayout = (CoordinatorLayout) bottomSheet.getParent();
                    BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
                    bottomSheetBehavior.setPeekHeight(bottomSheet.getHeight());
                    coordinatorLayout.getParent().requestLayout();
                }
            });
        }
    }

    public void dismissFragment() {
        dismissAllowingStateLoss();
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (binding != null) {
            binding.kmAssigneeViewPager.setCurrentItem(tab.getPosition(), true);

            if (pagerAdapter != null) {
                pagerAdapter.setSearchText(searchText, tab.getPosition());
            }
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {

    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        this.searchText = newText;
        if (binding != null && pagerAdapter != null) {
            pagerAdapter.setSearchText(newText, binding.kmAssigneeViewPager.getCurrentItem());
        }
        return false;
    }

    @Override
    public void onStatusChange(int newStatus) {
        if (conversationStatusListener != null) {
            conversationStatusListener.onStatusChange(newStatus);
        }
    }

    @Override
    public void onAssigneeChange(String newAssigneeId, String teamId) {
        if (conversationStatusListener != null) {
            conversationStatusListener.onAssigneeChange(newAssigneeId, teamId);
        }
        dismissFragment();
    }
}
