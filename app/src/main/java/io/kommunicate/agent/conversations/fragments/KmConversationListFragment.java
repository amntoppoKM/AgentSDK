package io.kommunicate.agent.conversations.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.people.channel.Channel;

import java.util.ArrayList;
import java.util.List;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.AgentGetStatusTask;
import io.kommunicate.agent.conversations.KmConversationStatusListener;
import io.kommunicate.agent.conversations.KmConversationUtils;
import io.kommunicate.agent.conversations.KmRecyclerScrollListener;
import io.kommunicate.agent.conversations.activity.AllConversationActivity;
import io.kommunicate.agent.conversations.adapters.KmConversationListAdapter;
import io.kommunicate.agent.conversations.repositories.KmConversationListRepo;
import io.kommunicate.agent.model.KmTag;
import io.kommunicate.agent.viewmodels.KmTagsViewModel;

public class KmConversationListFragment extends Fragment implements KmConversationStatusListener {

    private static final String TAG = "KmConversationListFragment";
    public static final String CONVERSATION_STATUS = "conversationStatus";
    private KmConversationListAdapter conversationListAdapter;
    private RecyclerView conversationRecyclerView;
    private List<Message> messageList;
    private int conversationStatus;
    private SwipeRefreshLayout kmConversationSwipeRefreshLayout;
    private LinearLayout noConversationLayout;
    private KmTagsViewModel tagsViewModel;

    public static KmConversationListFragment newInstance(int status) {
        KmConversationListFragment conversationListFragment = new KmConversationListFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(CONVERSATION_STATUS, status);
        conversationListFragment.setArguments(bundle);

        return conversationListFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (messageList == null) {
            messageList = new ArrayList<>();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.km_conversation_list_fragment, container, false);
        conversationRecyclerView = view.findViewById(R.id.kmConversationRecyclerView);
        kmConversationSwipeRefreshLayout = view.findViewById(R.id.km_conversation_swipe_refresh);
        noConversationLayout = view.findViewById(R.id.km_empty_conversation_layout);

        kmConversationSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        conversationRecyclerView.setLayoutManager(layoutManager);
        conversationListAdapter = new KmConversationListAdapter(getContext(), messageList, this);
        conversationRecyclerView.setAdapter(conversationListAdapter);

        tagsViewModel = new ViewModelProvider(requireActivity()).get(KmTagsViewModel.class);
        tagsViewModel.getTagList(null).observe(getViewLifecycleOwner(), listResource -> {
            if (listResource.isSuccess() && listResource.getData() != null) {
                conversationListAdapter.updateKmTagList(listResource.getData());
            }
        });

        conversationRecyclerView.addOnScrollListener(new KmRecyclerScrollListener() {
            @Override
            public void onScrollUp() {

            }

            @Override
            public void onScrollDown() {

            }

            @Override
            public void onLoadMore() {
                if (conversationListAdapter != null && messageList != null && !messageList.isEmpty()) {
                    conversationListAdapter.showLoading(true);
                    KmConversationListRepo.getConversationListAsync(getContext(), conversationStatus, messageList.get(messageList.size() - 1).getCreatedAtTime(), true, (newMessageList, e) -> {
                        conversationListAdapter.showLoading(false);
                        int insertionIndex = messageList.size();
                        if (newMessageList != null) {
                            messageList.addAll(newMessageList);
                            conversationListAdapter.setMessageList(messageList);
                        }
                        conversationListAdapter.notifyItemRangeInserted(insertionIndex, messageList.size() - 1);
                    });
                }
            }
        });

        kmConversationSwipeRefreshLayout.setOnRefreshListener(() -> {
            KmConversationListRepo.getConversationListAsync(getContext(), conversationStatus, null, true, (newMessageList, e) -> {
                attachMessageList(newMessageList, -1, e, null);
            });
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        conversationStatus = getArguments() != null ? getArguments().getInt(CONVERSATION_STATUS) : 0;
        attachConversationViewModel(conversationStatus, null);
    }

    public void attachConversationViewModel(int status, AllConversationActivity.MessageListLoadFinishCallback callback) {
        conversationListAdapter.showLoading(true);
        KmConversationListRepo.getConversationListAsync(getContext(), status, null, false, (newMessageList, e) -> {
            attachMessageList(newMessageList, status, e, callback);
        });

        if (status > Channel.ASSIGNED_CONVERSATIONS) {
            KmConversationListRepo.getConversationListAsync(getContext(), status, null, true, (newMessageList, e) -> {
                attachMessageList(newMessageList, status, e, callback);
            });
        }
    }

    private void attachMessageList(List<Message> newMessageList, int status, Exception e, AllConversationActivity.MessageListLoadFinishCallback callback) {
        if (messageList.isEmpty() && newMessageList != null && newMessageList.isEmpty()) {
            showEmptyConversationLayout(true, false);
        } else if (newMessageList != null) {
            messageList.clear();
            if(AgentSharedPreference.getInstance(getContext()).getAgentRoleType().equals(AgentGetStatusTask.AgentDetail.RoleType.OPERATOR.getValue())
            && AgentSharedPreference.getInstance(getContext()).isAgentRouting()) {
                String currentUserId = MobiComUserPreference.getInstance(getContext()).getUserId();
                for(Message message : newMessageList) {
                    if (message.getGroupId() != null && message.getGroupId() != 0) {
                        Channel channel = ChannelService.getInstance(getContext()).getChannel(message.getGroupId());
                        if (channel != null && currentUserId.equals(channel.getConversationAssignee())) {
                                messageList.add(message);
                        }
                    }
                }
            }
            else {
                messageList.addAll(newMessageList);
            }
            conversationListAdapter.setMessageList(messageList);
            conversationListAdapter.notifyDataSetChanged();
            if (status != -1) {
                conversationStatus = status;
            }
        } else if (e != null) {
            if (e.getMessage() != null && e.getMessage().startsWith("Unable to resolve host")) {
                KmToast.error(ApplozicService.getContext(getContext()), R.string.internet_connection_not_available, Toast.LENGTH_SHORT).show();
            } else {
                KmToast.error(ApplozicService.getContext(getContext()), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        if(conversationListAdapter != null && conversationListAdapter.isLoading()) {
            conversationListAdapter.showLoading(false); //hide the shimmer loading items
        }

        if (kmConversationSwipeRefreshLayout != null && kmConversationSwipeRefreshLayout.isRefreshing()) {
            kmConversationSwipeRefreshLayout.setRefreshing(false);
        }

        if (callback != null) {
            callback.onFinish(status);
        }
    }

    public void refreshMessageList() {
        if (messageList != null && !messageList.isEmpty()) {
            if (conversationStatus > Channel.ASSIGNED_CONVERSATIONS) {
                KmConversationListRepo.getConversationListAsync(getContext(), conversationStatus, null, true, (newMessageList, e) -> {
                    attachMessageList(newMessageList, conversationStatus, e, null);
                });
            }
        }
    }

    public void notifyAdapter() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (conversationListAdapter != null) {
                    conversationListAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    public void addMessage(Message message) {
        if (messageList == null) {
            return;
        }
        if (messageList.size() == 0) {
            showEmptyConversationLayout(false, false);
        }
        KmConversationUtils.addConversation(message, messageList);
        conversationListAdapter.setMessageList(messageList);
        notifyAdapter();
    }

    public void removeMessage(Message message) {
        removeConversation(message.getContactIds(), message.getGroupId());
    }

    public void removeConversation(String userId, Integer channelKey) {
        if (messageList == null) {
            return;
        }
        int index = KmConversationUtils.removeConversation(userId, channelKey, messageList);
        if (index == -1) {
            return;
        }
        if (conversationStatus == Channel.ASSIGNED_CONVERSATIONS && messageList.size() == 0) {
            showEmptyConversationLayout(true, true);
            return;
        }
        conversationListAdapter.setMessageList(messageList);
        conversationListAdapter.notifyItemRemoved(index);
    }

    public int getCurrentStatus() {
        return conversationStatus;
    }

    @Override
    public void onResume() {
        super.onResume();
        KmConversationListRepo.getConversationListAsync(getContext(), conversationStatus, null, true, (newMessageList, e) -> {
            attachMessageList(newMessageList, -1, e, null);
        });
    }

    @Override
    public void onStatusChange(int newStatus) {

    }

    @Override
    public void onAssigneeChange(String newAssigneeId, String teamId) {

    }

    public void showEmptyConversationLayout(boolean show, boolean isForResolved) {
        noConversationLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        kmConversationSwipeRefreshLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        conversationRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);

        if (show) {
            ((ImageView) noConversationLayout.findViewById(R.id.km_empty_conversation_image_view)).setImageResource(isForResolved ? R.drawable.ic_cato_kudos : R.drawable.ic_no_message);
            ((TextView) noConversationLayout.findViewById(R.id.km_empty_conversation_heading)).setText(isForResolved ? R.string.km_all_conversations_resolved_header : R.string.km_no_conversations_text);
            noConversationLayout.findViewById(R.id.km_empty_conversation_description).setVisibility(isForResolved ? View.VISIBLE : View.GONE);
        }
    }

}
