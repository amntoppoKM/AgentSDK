package io.kommunicate.agent.conversations.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.feed.GroupInfoUpdate;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;

import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.kommunicate.KmConversationHelper;
import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.ConversationAssigneeUpdateTask;
import io.kommunicate.agent.conversations.KmConversationStatusListener;
import io.kommunicate.agent.conversations.adapters.KmAssigneeListAdapter;
import io.kommunicate.agent.databinding.KmUserListFragmentBinding;
import io.kommunicate.agent.model.KmAgentContact;
import io.kommunicate.agent.model.KmTeam;
import io.kommunicate.agent.viewmodels.KmTeamViewModel;
import io.kommunicate.async.KmUpdateConversationTask;
import io.kommunicate.callbacks.KmCallback;
import io.kommunicate.users.KmContact;

public class KmUserListFragment extends Fragment implements KmCallback {
    public static final String AGENT_TAB = "AGENT_TAB";
    public static final String BOT_TAB = "BOT_TAB";
    public static final String TEAMS_TAB = "TEAMS_TAB";

    private static final String TAG = "KmUserListFragment";
    private static final String TAB_TYPE = "TAB_TYPE";
    private KmAssigneeListAdapter assigneeListAdapter;
    private KmUserListFragmentBinding binding;
    private int channelKey;
    private String tabType;
    private KmTeamViewModel teamViewModel;
    private KmConversationStatusListener conversationStatusListener;

    public String getTabType() {
        return  tabType;
    }

    public static KmUserListFragment newInstance(String assigneeId, String tabType, int channelId, List<KmTeam> teamList, List<KmAgentContact> userList) {
        KmUserListFragment fragment = new KmUserListFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Channel.CONVERSATION_ASSIGNEE, assigneeId);
        bundle.putString(TAB_TYPE, tabType);
        bundle.putInt(ConversationUIService.GROUP_ID, channelId);
        if (userList != null && !userList.isEmpty()) {
            bundle.putString(TAG, GsonUtils.getJsonFromObject(userList.toArray(), KmContact[].class));
        }
        if (teamList != null && !teamList.isEmpty()) {
            bundle.putString(TAG, GsonUtils.getJsonFromObject(teamList.toArray(), KmTeam[].class));
        }
        fragment.setArguments(bundle);
        return fragment;
    }

    public void setConversationStatusListener(KmConversationStatusListener conversationStatusListener) {
        this.conversationStatusListener = conversationStatusListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            tabType = getArguments().getString(TAB_TYPE);
            channelKey = getArguments().getInt(ConversationUIService.GROUP_ID);
        }
    }

    public void showEmptyListText(boolean show, boolean isForSearch) {
        if (binding != null) {
            binding.kmUserListRecycler.setVisibility(show ? View.GONE : View.VISIBLE);
            binding.kmEmptyListMessage.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.kmLoadingProgressBar.setVisibility(View.GONE);
            binding.kmEmptyListMessage.setText(isForSearch ? getNoSearchResultString() : getEmptyTextString());
        }
    }

    private void showInitialLoading(boolean show) {
        if (binding != null) {
            binding.kmLoadingProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            binding.kmEmptyListMessage.setVisibility(View.GONE);
        }
    }

    public void setSearchText(String searchText) {
        if (assigneeListAdapter != null) {
            assigneeListAdapter.getFilter().filter(searchText);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.km_user_list_fragment, container, false);

        binding = DataBindingUtil.bind(view);
        teamViewModel = new ViewModelProvider(requireActivity()).get(KmTeamViewModel.class);
        if (getArguments() != null && binding != null) {
            showInitialLoading(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setOrientation(RecyclerView.VERTICAL);
            binding.kmUserListRecycler.setLayoutManager(layoutManager);
            assigneeListAdapter = new KmAssigneeListAdapter(getArguments().getString(Channel.CONVERSATION_ASSIGNEE, null), this);
            if (getArguments().containsKey(TAG)) {
                if (TEAMS_TAB == tabType) {
                    addTeamList(Arrays.asList((KmTeam[]) GsonUtils.getObjectFromJson(getArguments().getString(TAG), KmTeam[].class)));
                } else {
                    addUserList(Arrays.asList((KmAgentContact[]) GsonUtils.getObjectFromJson(getArguments().getString(TAG), KmAgentContact[].class)));
                }
            }

            if (TEAMS_TAB == tabType && assigneeListAdapter.isListEmpty()) {
                teamViewModel.getTeamList().observe(getViewLifecycleOwner(), listResource -> {
                    if (listResource.isSuccess() && listResource.getData() != null) {
                        addTeamList(listResource.getData());
                    }
                });
            }
            binding.kmUserListRecycler.setAdapter(assigneeListAdapter);
        }

        return view;
    }

    public void addUserList(List<KmAgentContact> userList) {
        showInitialLoading(false);

        if (assigneeListAdapter != null) {
            assigneeListAdapter.addAssigneeList(userList);

            if (userList == null || userList.isEmpty()) {
                showEmptyListText(true, false);
            }
        }
    }

    public void addTeamList(List<KmTeam> teamList) {
        showInitialLoading(false);

        if (assigneeListAdapter != null) {
            assigneeListAdapter.addAssigneeList(teamList);

            if (teamList == null || teamList.isEmpty()) {
                showEmptyListText(true, false);
            }
        }
    }

    private String getEmptyTextString() {
        switch (tabType) {
            case AGENT_TAB:
                return Utils.getString(getContext(), R.string.km_empty_agent_list_message);
            case BOT_TAB:
                return Utils.getString(getContext(), R.string.km_empty_bot_list_message);
            case TEAMS_TAB:
                return Utils.getString(getContext(), R.string.km_empty_team_list_message);
        }
        return "";
    }

    private String getNoSearchResultString() {
        switch (tabType) {
            case AGENT_TAB:
                return Utils.getString(getContext(), R.string.km_no_agents_found);
            case BOT_TAB:
                return Utils.getString(getContext(), R.string.km_no_bots_found);
            case TEAMS_TAB:
                return Utils.getString(getContext(), R.string.km_no_teams_found);
        }
        return "";
    }

    @Override
    public void onSuccess(Object message) {
        if (message instanceof KmAgentContact) {
            updateConversationAssignee(((KmAgentContact) message).getUserId(), channelKey);
        } else if (message instanceof KmTeam) {
            updateTeam((KmTeam) message, channelKey);
        } else {
            Boolean showEmptySearchText = (Boolean) message;
            showEmptyListText(showEmptySearchText, true);
        }
    }

    @Override
    public void onFailure(Object error) {

    }

    private void updateConversationAssignee(final String assigneeId, Integer conversationId) {
        ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage(Utils.getString(getContext(), R.string.km_assignee_update_text));
        dialog.setCancelable(false);
        dialog.show();

        new ConversationAssigneeUpdateTask(conversationId, assigneeId, true, true, true, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                if (getActivity() instanceof KmConversationStatusListener) {
                    ((KmConversationStatusListener) getActivity()).onAssigneeChange(assigneeId, null);
                }
                if (conversationStatusListener != null) {
                    conversationStatusListener.onAssigneeChange(assigneeId, null);
                }
                dialog.dismiss();
            }

            @Override
            public void onFailure(Object error) {
                dialog.dismiss();
                KmToast.error(getContext(), GsonUtils.getJsonFromObject(error, Object.class), Toast.LENGTH_SHORT).show();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void updateTeam(final KmTeam team, Integer conversationId) {
        ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage(Utils.getString(getContext(), R.string.km_team_update_text));
        dialog.setCancelable(false);
        dialog.show();

        Channel channel = ChannelService.getInstance(getContext()).getChannel(conversationId);
        Map<String, String> metadata = channel.getMetadata();
        metadata.put(KmConversationHelper.KM_TEAM_ID, String.valueOf(team.getTeamId()));
        GroupInfoUpdate groupInfoUpdate = new GroupInfoUpdate(metadata, conversationId);

        KmUpdateConversationTask.KmConversationUpdateListener kmConversationUpdateListener = new KmUpdateConversationTask.KmConversationUpdateListener() {
            @Override
            public void onSuccess(Context context) {
                if (getActivity() instanceof KmConversationStatusListener) {
                    ((KmConversationStatusListener) getActivity()).onAssigneeChange(null, String.valueOf(team.getTeamId()));
                }
                if (conversationStatusListener != null) {
                    conversationStatusListener.onAssigneeChange(null, String.valueOf(team.getTeamId()));
                }
                dialog.dismiss();
            }

            @Override
            public void onFailure(Context context) {
                dialog.dismiss();
            }
        };

        new KmUpdateConversationTask(getContext(), groupInfoUpdate, kmConversationUpdateListener).execute();

    }
}
