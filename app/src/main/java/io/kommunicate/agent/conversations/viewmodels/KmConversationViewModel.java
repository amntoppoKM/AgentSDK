package io.kommunicate.agent.conversations.viewmodels;


import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.activities.LeadCollectionActivity;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.contact.Contact;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.kommunicate.KmCustomEventManager;
import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.asyncs.AgentGetStatusTask;
import io.kommunicate.agent.asyncs.AgentStatusUpdateTask;
import io.kommunicate.agent.model.NetworkBoundResourceModel;

/**
 * the view model class
 * repository isn't implemented for this view model (see android architecture components pattern)
 * this class acts as both the as source of data and the view model class
 */
public class KmConversationViewModel extends ViewModel {
    //agent status related view model ints
    public static final int STATUS_AWAY = 0;
    public static final int STATUS_ONLINE = 1;

    public static final String STATUS_TOPIC = "status-v2";

    private static final String TAG = "KmConversationViewModel";
    public static final String DESIGNATION = "designation";

    private MutableLiveData<List<Message>> messageListLiveData;
    private MutableLiveData<Contact> loggedInUser;
    private MutableLiveData<NetworkBoundResourceModel<Integer>> agentStatus; //agent status
    private static Integer status;

    public LiveData<List<Message>> getMessageList(Context context, int status, boolean makeServerCall) {
        if (messageListLiveData == null) {
            messageListLiveData = new MutableLiveData<>();
        }
        //loadMessages(context, status, null, makeServerCall);
        return messageListLiveData;
    }

    public void unregisterMessageList(Context context, LifecycleOwner lifecycleOwner) {
        if (messageListLiveData != null && messageListLiveData.hasActiveObservers()) {
            messageListLiveData.removeObservers(lifecycleOwner);
        }
    }

    public LiveData<Contact> getLoggedInUser(Context context) {
        if (loggedInUser == null) {
            loggedInUser = new MutableLiveData<>();
        }
        Contact contact = new AppContactService(context).getContactById(MobiComUserPreference.getInstance(context).getUserId());
        AgentGetStatusTask.AgentDetail agentDetail = AgentSharedPreference.getInstance(context).getAgentDetails();
        if (agentDetail != null) {
            if (!TextUtils.isEmpty(agentDetail.getEmail())) {
                contact.setEmailId(agentDetail.getEmail());
            }
            if (!TextUtils.isEmpty(agentDetail.getName())) {
                contact.setFullName(agentDetail.getName());
            }
        }
        loggedInUser.setValue(contact);
        return loggedInUser;
    }

    public MutableLiveData<NetworkBoundResourceModel<Integer>> getAgentStatus() {
        if (agentStatus == null) {
            agentStatus = new MutableLiveData<>();
            agentStatus.postValue(new NetworkBoundResourceModel<>(status, NetworkBoundResourceModel.RESOURCE_OK));
            //agentStatus.postValue(new NetworkBoundResourceModel<>(null, NetworkBoundResourceModel.RESOURCE_RETRIEVING));
        }
        return agentStatus;
    }

    /**
     * retrieve the agent status using the respective API *and* set it with the view model
     *
     * @param context the context, NOTE: pass the activity/fragment
     */
    public void retrieveAgentStatus(Context context) {
        NetworkBoundResourceModel<Integer> boundAgentStatus = getAgentStatus().getValue() != null ? getAgentStatus().getValue() : new NetworkBoundResourceModel<>(null, NetworkBoundResourceModel.RESOURCE_RETRIEVING);
        boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_RETRIEVING);
        getAgentStatus().postValue(boundAgentStatus);

        Contact contact = getLoggedInUser(context).getValue();


        new AgentGetStatusTask(context.getApplicationContext(), contact == null ? MobiComUserPreference.getInstance(context).getUserId() : contact.getUserId(), new AgentGetStatusTask.KmAgentGetStatusHandler() {
            @Override
            public void onFinished(boolean status) {
                boundAgentStatus.setResource(status ? STATUS_ONLINE : STATUS_AWAY);
                boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_SUCCESS_GET);
                KmConversationViewModel.status = boundAgentStatus.getResource();
                getAgentStatus().postValue(boundAgentStatus);
            }

            @Override
            public void onError(String error) {
                Utils.printLog(context, TAG, error);
                boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_FAILURE_GET);
                getAgentStatus().postValue(boundAgentStatus);
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * set the status (in server) and also toggle it (in the view model)
     *
     * @param context the context, NOTE: pass the activity/fragment context
     */
    public MutableLiveData<Integer> toggleAgentStatus(Context context) {
        NetworkBoundResourceModel<Integer> boundAgentStatus = getAgentStatus().getValue();
        MutableLiveData<Integer> agentStatusLiveData = new MutableLiveData<>();

        if (boundAgentStatus != null && boundAgentStatus.getResource() != null) {
            //to toggle the menu item immediately
            int status = boundAgentStatus.getResource();
            boundAgentStatus.setResource(status == STATUS_ONLINE ? STATUS_AWAY : STATUS_ONLINE);
            boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_SETTING);
            getAgentStatus().postValue(boundAgentStatus);

            Contact contact = getLoggedInUser(context).getValue();
            String userId = contact == null ? MobiComUserPreference.getInstance(context).getUserId() : contact.getUserId();
            publishAgentStatus(context, status != STATUS_ONLINE);
            new AgentStatusUpdateTask(context, userId, status != STATUS_ONLINE, new AgentStatusUpdateTask.KmAgentStatusHandler() {
                @Override
                public void onSuccess() {
                    agentStatusLiveData.postValue(status != STATUS_ONLINE ? STATUS_AWAY : STATUS_ONLINE);
//                    boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_SUCCESS_PUT);
//                    getAgentStatus().postValue(boundAgentStatus); //callback fired for successful put
//                    boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_OK);
//                    getAgentStatus().postValue(boundAgentStatus); //callback fired for status OK
                }

                @Override
                public void onFailure() {
                    agentStatusLiveData.postValue(-1);
                    boundAgentStatus.setResourceState(NetworkBoundResourceModel.RESOURCE_FAILURE_PUT);
                    boundAgentStatus.setResource(status);
                    getAgentStatus().postValue(boundAgentStatus);
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            retrieveAgentStatus(context);
        }
        return agentStatusLiveData;
    }

    public void publishAgentStatus(Context context, boolean online) {
        String data = MobiComUserPreference.getInstance(context).getSuUserKeyString() +
                "," +
                MobiComUserPreference.getInstance(context).getDeviceKeyString() +
                "," +
                (online ? KmCustomEventManager.AGENT_ONLINE_STATUS : KmCustomEventManager.AGENT_AWAY_STATUS);
        KmCustomEventManager.getInstance(context).publishDataToTopic(STATUS_TOPIC, data, false);
    }

    public static User getUser(Context context) {
        String userId = MobiComUserPreference.getInstance(context).getUserId();
        Contact contact = new AppContactService(context).getContactById(userId);
        User user = new User();
        user.setUserId(userId);

        if (contact != null) {
            user.setImageLink(contact.getImageURL());
            user.setDisplayName(contact.getDisplayName());
            if (!TextUtils.isEmpty(contact.getEmailId())) {
                user.setEmail(contact.getEmailId());
            } else if (!TextUtils.isEmpty(contact.getUserId()) && Pattern.compile(LeadCollectionActivity.EMAIL_VALIDATION_REGEX).matcher(contact.getUserId()).matches()) {
                user.setEmail(contact.getUserId());
            }

            user.setContactNumber(contact.getContactNumber());
            user.setMetadata(contact.getMetadata());
        }

        AgentGetStatusTask.AgentDetail agentDetail = AgentSharedPreference.getInstance(context).getAgentDetails();

        if (agentDetail != null) {
            if (!TextUtils.isEmpty(agentDetail.getApplicationId())) {
                user.setApplicationId(agentDetail.getApplicationId());
            }
            if (!TextUtils.isEmpty(agentDetail.getEmail())) {
                user.setEmail(agentDetail.getEmail());
            }
            if (!TextUtils.isEmpty(agentDetail.getName())) {
                user.setDisplayName(agentDetail.getName());
            }
            if (!TextUtils.isEmpty(agentDetail.getContactNo())) {
                user.setContactNumber(agentDetail.getContactNo());
            }
            if (!TextUtils.isEmpty(agentDetail.getRole())) {
                Map<String, String> metadata = user.getMetadata();
                if (metadata == null) {
                    metadata = new HashMap<>();
                }
                metadata.put(DESIGNATION, agentDetail.getRole());
            }
        }
        return user;
    }
}
