package io.kommunicate.agent.asyncs;

import android.os.AsyncTask;
import android.text.TextUtils;

import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.json.GsonUtils;

import io.kommunicate.agent.services.AgentClientService;
import io.kommunicate.callbacks.KmCallback;

public class ConversationAssigneeUpdateTask extends AsyncTask<Void, Void, String> {

    private Integer groupId;
    private String assigneeId;
    private boolean switchAssignee;
    private boolean sendNotifyMessage;
    private boolean takeOverFromBot;
    private KmCallback callback;
    private AgentClientService agentClientService;

    public ConversationAssigneeUpdateTask(Integer groupId, String assigneeId, boolean switchAssignee, boolean sendNotifyMessage, boolean takeOverFromBot, KmCallback callback) {
        this.groupId = groupId;
        this.assigneeId = assigneeId;
        this.switchAssignee = switchAssignee;
        this.sendNotifyMessage = sendNotifyMessage;
        this.takeOverFromBot = takeOverFromBot;
        this.callback = callback;
        this.agentClientService = new AgentClientService(ApplozicService.getAppContext());
    }

    @Override
    protected String doInBackground(Void... voids) {
        return agentClientService.switchConversationAssignee(groupId, assigneeId, switchAssignee, sendNotifyMessage, takeOverFromBot);
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
