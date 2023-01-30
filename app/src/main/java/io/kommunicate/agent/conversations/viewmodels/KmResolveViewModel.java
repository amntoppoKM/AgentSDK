package io.kommunicate.agent.conversations.viewmodels;

import android.text.TextUtils;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;

//view model for more items list (resolve, assignee, spam, tag...)
public class KmResolveViewModel extends ViewModel {
    private Channel channel;
    private MutableLiveData<Integer> conversationStatusLiveData;
    private MutableLiveData<String> assigneeNameLiveData;

    public MutableLiveData<Integer> getConversationStatusLiveData() {
        if (conversationStatusLiveData == null) {
            conversationStatusLiveData = new MutableLiveData<>();
        }
        return conversationStatusLiveData;
    }

    public MutableLiveData<String> getAssigneeNameLiveData() {
        if (assigneeNameLiveData == null) {
            assigneeNameLiveData = new MutableLiveData<>();
        }
        return assigneeNameLiveData;
    }

    public void updateConversationStatusLiveData(int conversationStatus) {
        getConversationStatusLiveData().postValue(conversationStatus);
    }

    public void updateAssigneeNameLiveData(String assigneeName) {
        getAssigneeNameLiveData().postValue(assigneeName);
    }

    public void updateChannelDetailsForMoreItems(Channel channel) {
        if (channel == null) {
            return;
        }
        this.channel = channel;
        updateConversationStatusLiveData(channel.getConversationStatus());
        //KmConversationStatus.updateConversationStatus(view.getContext(), KmConversationStatus.getStatusForUpdate(resolve.getStatusName()), channel.getKey());
        updateAssigneeNameLiveData(KmResolveViewModel.getAssigneeNameFrom(channel.getConversationAssignee()));
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public static String getAssigneeNameFrom(String assigneeId) {
        if (TextUtils.isEmpty(assigneeId)) {
            return null;
        }
        Contact assignee = new AppContactService(ApplozicService.getAppContext()).getContactById(assigneeId);
        if (assignee == null) {
            return null;
        }
        return assignee.getDisplayName();
    }
}
