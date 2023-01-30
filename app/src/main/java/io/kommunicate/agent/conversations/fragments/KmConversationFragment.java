package io.kommunicate.agent.conversations.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.channel.database.ChannelDatabaseService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.adapter.DetailedConversationAdapter;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;

import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.emoticon.EmojiconHandler;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.channel.ChannelUserMapper;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.ConversationAssigneeUpdateTask;
import io.kommunicate.agent.conversations.adapters.KmMessageListAdapter;
import io.kommunicate.agent.conversations.viewmodels.KmAppSettingsViewModel;
import io.kommunicate.agent.conversations.viewmodels.KmResolveViewModel;
import io.kommunicate.agent.model.KmConversationStatus;
import io.kommunicate.callbacks.KmCallback;
import io.kommunicate.services.KmChannelService;
import io.kommunicate.services.KmService;

import static android.view.View.VISIBLE;

public class KmConversationFragment extends ConversationFragment {
    private static final String TAG = "KmConversationFragment";
    protected static final String CONTACT = "CONTACT";
    protected static final String CHANNEL = "CHANNEL";
    protected static final String CONVERSATION_ID = "CONVERSATION_ID";
    protected static final String SEARCH_STRING = "SEARCH_STRING";
    private KmResolveViewModel resolveViewModel;
    private KmAppSettingsViewModel appSettingsViewModel;
    private static final String DEFAULT_BOT = "bot";

    public static ConversationFragment newInstance(Contact contact, Channel channel, Integer conversationId, String searchString, String messageSearchString) {
        ConversationFragment f = new KmConversationFragment();
        Bundle args = new Bundle();
        if (contact != null) {
            args.putSerializable(CONTACT, contact);
        }
        if (channel != null) {
            args.putSerializable(CHANNEL, channel);
        }
        if (conversationId != null) {
            args.putInt(CONVERSATION_ID, conversationId);
        }
        args.putString(SEARCH_STRING, searchString);
        args.putString(ConversationUIService.MESSAGE_SEARCH_STRING, messageSearchString);
        f.setArguments(args);
        return f;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {
            resolveViewModel = new ViewModelProvider(requireActivity()).get(KmResolveViewModel.class);
            appSettingsViewModel = new ViewModelProvider(requireActivity()).get(KmAppSettingsViewModel.class);
            ImageButton moreOptionsButton = view.findViewById(R.id.more_options_btn);

            if (moreOptionsButton != null) {
                moreOptionsButton.setVisibility(VISIBLE);
                moreOptionsButton.setOnClickListener(v -> {
                    updateResolveViewModelChannelAndOpenFragment();
                });
            }
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    protected void processMobiTexterUserCheck() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if (resolveViewModel != null && channel != null) {
            resolveViewModel.updateChannelDetailsForMoreItems(ChannelService.getInstance(getContext()).getChannel(channel.getKey()));
        }
        if (appSettingsViewModel != null) {
            KmAppSettingsViewModel.AppSettingsCache.saveCurrentUserRole(AgentSharedPreference.getInstance(getContext()).getAgentRoleType());
            if (KmAppSettingsViewModel.AppSettingsCache.isOperator()) {
                appSettingsViewModel.fetchAppSettings();
            }
        }
    }

    @Override
    public void onMessageReceived(Message message) {
        super.onMessageReceived(message);
        updateMoreItemsViewModelDataFromStatusMessage(message);
    }

    @Override
    public void onMessageSync(Message message, String key) {
        super.onMessageSync(message, key);
        updateMoreItemsViewModelDataFromStatusMessage(message);
    }

    public void updateMoreItemsViewModelDataFromStatusMessage(Message message) {
        if (message == null || resolveViewModel == null) {
            return;
        }
        if (channel != null) {
            resolveViewModel.setChannel(channel);
        }
        if (!TextUtils.isEmpty(message.getConversationStatus())) {
            resolveViewModel.updateConversationStatusLiveData(channel.getConversationStatus());
        }
        if (!TextUtils.isEmpty(message.getConversationAssignee())) {
            resolveViewModel.updateAssigneeNameLiveData(KmResolveViewModel.getAssigneeNameFrom(message.getConversationAssignee()));
        }
    }

    @Override
    protected void setChannel(Channel channel) {
        super.setChannel(channel);
        if (resolveViewModel != null) {
            resolveViewModel.updateChannelDetailsForMoreItems(channel);
        }
    }

    public void updateResolveViewModelChannelAndOpenFragment() {
        if (resolveViewModel == null || channel == null) {
            Utils.printLog(getContext(), TAG, "Channel or KmResolveViewModel null.");
            return;
        }
        resolveViewModel.setChannel(channel);
        openFragment();
    }

    public void openFragment() {
        KmBottomSlideFragment.newInstance().show(getChildFragmentManager(), KmBottomSlideFragment.getFragTag());
    }

    @Override
    public void processTakeOverFromBot(Context context, Channel channel) {
        if (context == null || channel == null) {
            return;
        }

        final String loggedInUserId = MobiComUserPreference.getInstance(context).getUserId();

        Set<String> botIds = KmChannelService.getInstance(context).getListOfUsersByRole(channel.getKey(), ChannelUserMapper.UserRole.MODERATOR.getValue());
        if (botIds != null) {
            botIds.remove(DEFAULT_BOT);
        }

        Map<String, String> metadata = channel.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put(Channel.CONVERSATION_ASSIGNEE, loggedInUserId);
        channel.setMetadata(metadata);
        ChannelService.getInstance(context).updateChannel(channel);
        final ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage(ApplozicService.getContext(context).getString(R.string.processing_take_over_from_bot));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new ConversationAssigneeUpdateTask(channel.getKey(), loggedInUserId, true, true, true, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                progressDialog.dismiss();
                showTakeOverFromBotLayout(false, null);
            }

            @Override
            public void onFailure(Object error) {
                progressDialog.dismiss();
                KmToast.error(context, GsonUtils.getJsonFromObject(error, Object.class), Toast.LENGTH_SHORT).show();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void processSupportGroupDetails(final Channel channel) {
        Contact contact = KmService.getSupportGroupContact(getContext(), channel, appContactService, loggedInUserRole);

        if (KmConversationStatus.isConversationResolved(channel.getConversationStatus())) {
            showTakeOverFromBotLayout(false, null);
        } else {
            Contact assigneeContact = KmService.getAssigneeContact(channel, appContactService);
            ChannelUserMapper assigneeUserMapper = ChannelDatabaseService.getInstance(getContext()).getChannelUserByChannelKeyAndUserId(channel.getKey(), assigneeContact.getUserId());
            if (assigneeUserMapper != null && assigneeContact != null) {
                showTakeOverFromBotLayout(assigneeUserMapper.getRole() == 2 && !"bot".equals(assigneeContact.getUserId()), assigneeContact);
            }
        }

        updateSupportGroupTitleAndImageAndHideSubtitle(channel);
        switchContactStatus(contact, null);
        conversationAssignee = contact;

        if (contact != null) {
            getUserDetail(getContext(), contact.getUserId(), contact1 -> {
                conversationAssignee = contact1;
                updateSupportGroupTitleAndImageAndHideSubtitle(channel);
                retrieveAgentStatusAndSwitchContactStatusUI(contact1);
            });
        }
    }

    @Override
    public void onStartLoading(boolean loadingStarted) {

    }

    @Override
    protected DetailedConversationAdapter getConversationAdapter(Activity activity, int rowViewId, List<Message> messageList, Contact contact, Channel channel, Class messageIntentClass, EmojiconHandler emojiIconHandler) {
        return new KmMessageListAdapter(activity, rowViewId, messageList, contact, channel, messageIntentClass, emojiIconHandler);
    }
}
