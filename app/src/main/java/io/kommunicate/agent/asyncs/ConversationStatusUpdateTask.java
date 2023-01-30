package io.kommunicate.agent.asyncs;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.json.GsonUtils;

import io.kommunicate.agent.services.AgentClientService;
import io.kommunicate.callbacks.KmCallback;

public class ConversationStatusUpdateTask extends AsyncTask<Void, Void, String> {

    private Integer groupId;
    private int status;
    private boolean sendNotifyMessage;
    private KmCallback callback;
    private AgentClientService agentClientService;

    public ConversationStatusUpdateTask(Integer groupId, int status, boolean sendNotifyMessage, KmCallback callback) {
        this.groupId = groupId;
        this.status = status;
        this.sendNotifyMessage = sendNotifyMessage;
        this.callback = callback;
        this.agentClientService = new AgentClientService(ApplozicService.getAppContext());
    }

    @Override
    protected String doInBackground(Void... voids) {
        return agentClientService.changeConversationStatus(groupId, status, sendNotifyMessage);
    }

    @Override
    protected void onPostExecute(String s) {
        if (callback != null) {
            if (!TextUtils.isEmpty(s)) {
                ApiResponse<String> apiResponse = (ApiResponse<String>) GsonUtils.getObjectFromJson(s, ApiResponse.class);
                if (apiResponse != null) {
                    if (apiResponse.isSuccess()) {
                        callback.onSuccess(apiResponse.getResponse());
                    } else {
                        callback.onFailure(apiResponse.getErrorResponse());
                    }
                } else {
                    callback.onFailure("Some error occurred");
                }
            } else {
                callback.onFailure("Some error occurred");
            }
        }
        super.onPostExecute(s);
    }
}
