package io.kommunicate.agent.conversations.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.channel.service.ChannelService;;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.KmUserInfoHelper;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmClickHandler;
import io.kommunicate.agent.conversations.KmConversationStatusListener;
import io.kommunicate.agent.conversations.adapters.KmMoreItemsListAdapter;
import io.kommunicate.agent.conversations.viewmodels.KmResolveViewModel;
import io.kommunicate.agent.databinding.KmConversationStatusListLayoutBinding;
import io.kommunicate.agent.fragments.KmTagsFragment;
import io.kommunicate.agent.model.KmConversationStatus;
import io.kommunicate.agent.model.KmMoreItem;
import io.kommunicate.agent.model.KmTag;
import io.kommunicate.agent.conversations.viewmodels.KmAppSettingsViewModel;
import io.kommunicate.agent.viewmodels.KmTagsViewModel;
import io.kommunicate.services.KmService;

import static io.kommunicate.agent.conversations.adapters.KmMoreItemsListAdapter.ASSIGNEE_ITEM_POSITION;
import static io.kommunicate.agent.conversations.adapters.KmMoreItemsListAdapter.RESOLVE_ITEM_POSITION;

public class KmBottomSlideFragment extends BottomSheetDialogFragment implements KmClickHandler<KmMoreItem>, KmConversationStatusListener {
    private static final String TAG = "KmBottomSlideFragment";
    private KmResolveViewModel resolveViewModel;
    private KmMoreItemsListAdapter kmMoreItemsListAdapter;
    private Channel channel;
    private Contact contact;
    private boolean isTagsFeatureAvailable;
    private KmTagsViewModel tagsViewModel;
    private List<Integer> appliedTagIds;
    private Boolean showAssigneeView = true;
    private Boolean isOperator;
    private Activity context;


    public static KmBottomSlideFragment newInstance() {
        return new KmBottomSlideFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isTagsFeatureAvailable = KmTagsFragment.Companion.isTagsFeatureAvailable(ApplozicService.getContext(getContext()));
        resolveViewModel = new ViewModelProvider(requireActivity()).get(KmResolveViewModel.class);
        channel = resolveViewModel.getChannel();
        context = getActivity();
        contact = KmService.getSupportGroupContact(getContext(), channel, new AppContactService(getContext()), AgentSharedPreference.getInstance(getContext()).getAgentRoleType());
        appliedTagIds = KmTag.Companion.getAppliedTagIds(channel);
        showAssigneeView = KmAppSettingsViewModel.AppSettingsCache.isAssignmentToOthersAllowed();
        isOperator = KmAppSettingsViewModel.AppSettingsCache.isOperator();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.km_conversation_status_list_layout, container, false);

        KmConversationStatusListLayoutBinding binding = DataBindingUtil.bind(view.findViewById(R.id.km_status_list_layout));

        if (binding != null && channel != null) {
            binding.setBottomSlideFragment(this);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            layoutManager.setOrientation(RecyclerView.VERTICAL);
            binding.statusRecyclerView.setLayoutManager(layoutManager);
            kmMoreItemsListAdapter = new KmMoreItemsListAdapter(getStatusListUsingChannel(), this);
            binding.statusRecyclerView.setAdapter(kmMoreItemsListAdapter);
        }

        tagsViewModel = new ViewModelProvider(requireActivity()).get(KmTagsViewModel.class);
        tagsViewModel.getTagList(KmTag.Companion
                .getAppliedTagIds(ChannelService.getInstance(getContext()).getChannel(channel.getKey()))).observe(getViewLifecycleOwner(), listResource -> {
            if (listResource.isSuccess() && listResource.getData() != null) {
                kmMoreItemsListAdapter.updateKmTagList(KmTag.Companion.getFilteredTagList(ChannelService.getInstance(getContext()).getChannel(channel.getKey()), listResource.getData()));
            }
        });

        tagsViewModel.getAppliedTagsLiveData().observe(getViewLifecycleOwner(), integers -> appliedTagIds = integers);
        resolveViewModel.updateChannelDetailsForMoreItems(channel);
        resolveViewModel.getConversationStatusLiveData().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                kmMoreItemsListAdapter.updateItem(RESOLVE_ITEM_POSITION, KmMoreItem.getStatusMoreItem(status));
            }
        });

        if (isOperator) {
            if (showAssigneeView) {
                resolveViewModel.getAssigneeNameLiveData().observe(getViewLifecycleOwner(), name -> {
                    if (!TextUtils.isEmpty(name)) {
                        kmMoreItemsListAdapter.updateItem(ASSIGNEE_ITEM_POSITION, KmMoreItem.getAssigneeNameMoreItem(name));
                    }
                });
            }

        } else {
            resolveViewModel.getAssigneeNameLiveData().observe(getViewLifecycleOwner(), name -> {
                if (!TextUtils.isEmpty(name)) {
                    kmMoreItemsListAdapter.updateItem(ASSIGNEE_ITEM_POSITION, KmMoreItem.getAssigneeNameMoreItem(name));
                }
            });
        }

        return view;
    }

    public static String getFragTag() {
        return TAG;
    }

    public List<KmMoreItem> getStatusListUsingChannel() {
        List<KmMoreItem> statusList = new ArrayList<>();
        statusList.add(KmMoreItem.getStatusMoreItem(channel.getConversationStatus()));

        if (isOperator) {
            if (showAssigneeView) {
                statusList.add(KmMoreItem.getAssigneeNameMoreItem(KmResolveViewModel.getAssigneeNameFrom(channel.getConversationAssignee())));
            }
        } else {
            statusList.add(KmMoreItem.getAssigneeNameMoreItem(KmResolveViewModel.getAssigneeNameFrom(channel.getConversationAssignee())));
        }
        statusList.add(KmMoreItem.getTagsMoreItem(isTagsFeatureAvailable));
        statusList.add(KmMoreItem.getTranscriptMoreItem());
        if (channel.getConversationStatus() != KmConversationStatus.STATUS_SPAM) {
            statusList.add(KmMoreItem.getSpamMoreItem());
        }
        return statusList;
    }

    @Override
    public void onItemClicked(View view, KmMoreItem data) {
        if (KmMoreItem.MoreItemType.ASSIGNEE_CHANGE.equals(data.getMoreItemType())) {
            openAssigneeListFragment();
        } else if (KmMoreItem.MoreItemType.SPAM.equals(data.getMoreItemType())) {
            KmConversationStatus.updateConversationStatus(getContext(), KmConversationStatus.STATUS_SPAM, channel.getKey());
            dismissFragment();
        } else if (KmMoreItem.MoreItemType.STATUS.equals(data.getMoreItemType())) {
            KmConversationStatus.updateConversationStatus(getContext(), KmConversationStatus.getStatusForUpdate(channel.getConversationStatus()), channel.getKey());
            channel.getMetadata().put(Channel.CONVERSATION_STATUS, String.valueOf(KmConversationStatus.getStatusForUpdate(channel.getConversationStatus())));
            ChannelService.getInstance(context).updateChannel(channel);

            dismissFragment();
        } else if (KmMoreItem.MoreItemType.TAG.equals(data.getMoreItemType())) {
            if (isTagsFeatureAvailable) {
                openTagsFragment();
            }
        } else if(KmMoreItem.MoreItemType.TRANSCRIPT.equals(data.getMoreItemType())) {
                openTranscriptFragment(contact);
        }
    }

    public void dismissFragment() {
        dismissAllowingStateLoss();
    }

    public void openTranscriptFragment(Contact contact) {
        BottomSheetDialog sendTranscriptDialog = new BottomSheetDialog(context);
        View transcriptView = LayoutInflater.from(getContext()).inflate(R.layout.km_send_transcript_bottom_dialog, null, false);
        sendTranscriptDialog.setContentView(transcriptView);
        Button sendTranscriptButton = transcriptView.findViewById(R.id.km_send_transcript_button);
        TextView transcriptText = transcriptView.findViewById(R.id.km_send_transcript_text);
        EditText emailEditText = transcriptView.findViewById(R.id.km_email_edit_text);
        ImageButton closeButton = transcriptView.findViewById(R.id.km_close_transcript);
        Button cancelButton = transcriptView.findViewById(R.id.km_cancel_transcript_button);
        sendTranscriptDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        sendTranscriptDialog.show();
        closeButton.setOnClickListener(view -> {
            sendTranscriptDialog.dismiss();
        });
        cancelButton.setOnClickListener(view -> {
            sendTranscriptDialog.dismiss();
        });
        if(!TextUtils.isEmpty(contact.getEmailId())) {
            transcriptText.setText(getString(R.string.km_transcript_to, contact.getEmailId()));
            sendTranscriptButton.setOnClickListener(view -> {
                KmUserInfoHelper.sendTranscript(context, channel.getKey(), contact.getEmailId());
                sendTranscriptDialog.dismiss();
            });
        } else {
            emailEditText.setVisibility(View.VISIBLE);
            transcriptText.setText(getString(R.string.km_transcript_enter_email));
            sendTranscriptButton.setOnClickListener(view -> {
                if (!KmUserInfoHelper.isValidEmail(emailEditText.getText().toString())) {
                    emailEditText.setError(getString(R.string.km_invalid_email_error));
                }
                else {
                    processContactUpdate(emailEditText.getText().toString());
                    KmUserInfoHelper.sendTranscript(context, channel.getKey(), emailEditText.getText().toString());
                    sendTranscriptDialog.dismiss();
                }
            });
        }

    }

    public void processContactUpdate(String email) {
        if (contact != null) {
            contact.setEmailId(email);
            User user = new User();
            user.setUserId(contact.getUserId());
            user.setEmail(email);
            KmUserInfoHelper.updateUserDetails(context, user);
        }
    }
    public void openTagsFragment() {
        KmTagsFragment tagsFragment = new KmTagsFragment(kmMoreItemsListAdapter);
        Bundle bundle = new Bundle();
        bundle.putString(KmTagsFragment.APPLIED_TAG_IDS, GsonUtils.getJsonFromObject(appliedTagIds, List.class));
        bundle.putString(KmTagsFragment.CONVERSATION_JSON, GsonUtils.getJsonFromObject(channel, Channel.class));
        tagsFragment.setArguments(bundle);
        tagsFragment.show(getChildFragmentManager(), KmTagsFragment.FRAG_TAG);
    }

    public void openAssigneeListFragment() {
        KmAssigneeListFragment kmAssigneeListFragment = KmAssigneeListFragment.newInstance(channel.getConversationAssignee(), channel.getTeamId(), channel.getKey());
        kmAssigneeListFragment.setConversationStatusListener(this);
        kmAssigneeListFragment.show(getChildFragmentManager(), KmAssigneeListFragment.getFragTag());
    }

    @Override
    public void onStatusChange(int newStatus) {

    }

    @Override
    public void onAssigneeChange(String newAssigneeId, String teamId) {
        dismissFragment();
    }
}
