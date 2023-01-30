package io.kommunicate.agent.conversations.services;

import android.content.Context;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.HttpRequestUtils;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.conversation.MessageClientService;
import com.applozic.mobicommons.people.channel.Channel;

import java.net.URLEncoder;

import io.kommunicate.agent.services.AgentClientService;

public class KmMessageClientService extends MessageClientService {

    private static final String GET_KM_CONVERSATION_LIST_URL = "/rest/ws/group/support";
    private final HttpRequestUtils httpRequestUtils;
    private AgentClientService agentClientService;
    private final Context context;

    public KmMessageClientService(Context context) {
        super(context);
        this.httpRequestUtils = new HttpRequestUtils(context);
        this.agentClientService = new AgentClientService(context);
        this.context = context;
    }

    private String getKmConversationListUrl() {
        return getBaseUrl() + GET_KM_CONVERSATION_LIST_URL;
    }

    public String getKmConversationList(int[] statusArray, String assigneeId, int pageSize, Long lastFetchTime) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(getKmConversationListUrl());
        if (!TextUtils.isEmpty(assigneeId)) {
            urlBuilder.append("/assigned?userId=").append(URLEncoder.encode(assigneeId, "UTF-8"));
            urlBuilder.append("&pageSize=").append(pageSize);
        } else {
            urlBuilder.append("?pageSize=").append(pageSize);
        }

        if (lastFetchTime != null && lastFetchTime != 0) {
            urlBuilder.append("&lastFetchTime=").append(lastFetchTime);
        }
        if (statusArray != null && statusArray.length > 0) {
            for (int status : statusArray) {
                urlBuilder.append("&status=").append(status);
            }
        }
        return agentClientService.getResponseWithException(urlBuilder.toString(), "application/json", "application/json", false, null);
    }

    public String getKmConversationList(int status, int pageSize, Long lastFetchTime) throws Exception {
        return getKmConversationList(status == Channel.CLOSED_CONVERSATIONS ? new int[]{2} : new int[]{0, 6},
                status == Channel.ASSIGNED_CONVERSATIONS ? MobiComUserPreference.getInstance(context).getUserId() : null,
                pageSize, lastFetchTime);
    }
}
