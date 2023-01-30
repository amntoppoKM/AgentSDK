package io.kommunicate.agent.asyncs;

import android.content.Context;
import android.os.AsyncTask;

import com.applozic.mobicomkit.api.MobiComKitClientService;

import java.lang.ref.WeakReference;

import io.kommunicate.agent.services.AgentClientService;
import io.kommunicate.models.KmApiResponse;

public class AgentStatusUpdateTask extends AsyncTask<Void, Void, Boolean> {
    private WeakReference<Context> contextWeakReference;
    private String userId;
    private Boolean online;
    private KmAgentStatusHandler kmAgentStatusHandler;

    private static final String SUCCESS = "SUCCESS";

    public AgentStatusUpdateTask(Context context, String userId, boolean online, KmAgentStatusHandler kmAgentStatusHandler) {
        contextWeakReference = new WeakReference<>(context);
        this.userId = userId;
        this.online = online;
        this.kmAgentStatusHandler = kmAgentStatusHandler;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        AgentClientService agentClientService = new AgentClientService(contextWeakReference.get());
        KmApiResponse<String> kmApiResponse = agentClientService.setAgentStatus(userId, MobiComKitClientService.getApplicationKey(contextWeakReference.get()), online);
        return kmApiResponse != null ? SUCCESS.equals(kmApiResponse.getCode()) : null;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        super.onPostExecute(success);
        if(success != null && success) {
            kmAgentStatusHandler.onSuccess();
        } else {
            kmAgentStatusHandler.onFailure();
        }
    }

    public interface KmAgentStatusHandler {
        void onSuccess();
        void onFailure();
    }
}
