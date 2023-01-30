package io.kommunicate.agent.services;

import android.content.Context;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.contact.AppContactService;
import com.applozic.mobicomkit.contact.BaseContactService;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kommunicate.agent.KmUtils;
import io.kommunicate.agent.model.AgentAPIResponse;
import io.kommunicate.agent.model.KmAgentContact;

public class AgentService {

    private static final String TAG = "AgentService";
    private Context context;
    private AgentClientService agentClientService;
    private BaseContactService baseContactService;

    public AgentService(Context context) {
        this.context = ApplozicService.getContext(context);
        this.agentClientService = new AgentClientService(this.context);
        this.baseContactService = new AppContactService(this.context);
    }

    public List<KmAgentContact> getUserList(Short... roles) throws Exception {
        String response = agentClientService.getUsersList(roles);
        if(response.equals(KmUtils.UN_AUTHORIZED)) {
            throw new Exception(KmUtils.UN_AUTHORIZED);
        }
        AgentAPIResponse<KmAgentContact> agentListResponse = (AgentAPIResponse<KmAgentContact>) GsonUtils.getObjectFromJson(response, new TypeToken<AgentAPIResponse<KmAgentContact>>() {
        }.getType());
        if (agentListResponse != null) {
            return agentListResponse.getResponse();
        }
        return null;
    }

    public ApiResponse updateDisplayNameORImageLink(User user) {
        ApiResponse response = agentClientService.updateDisplayNameORImageLink(user);

        if (response == null) {
            return null;
        }

        if (response.isSuccess()) {
            String userId = !TextUtils.isEmpty(user.getUserId()) ? user.getUserId() : MobiComUserPreference.getInstance(context).getUserId();
            Contact contact = baseContactService.getContactById(userId);
            contact.setFullName(user.getDisplayName());
            contact.setImageURL(user.getImageLink());
            contact.setContactNumber(user.getContactNumber());
            contact.setEmailId(user.getEmail());

            Map<String, String> existingMetadata = contact.getMetadata();
            if (existingMetadata == null) {
                existingMetadata = new HashMap<>();
            }
            if (user.getMetadata() != null) {
                existingMetadata.putAll(user.getMetadata());
            }
            contact.setMetadata(existingMetadata);

            baseContactService.upsert(contact);
            Contact contact1 = baseContactService.getContactById(userId);
            Utils.printLog(context, TAG, contact1.getImageURL() + ", " + contact1.getDisplayName() + "," + contact1.getStatus() + "," + contact1.getStatus() + "," + contact1.getMetadata() + "," + contact1.getEmailId() + "," + contact1.getContactNumber());
        }
        return response;
    }
}
