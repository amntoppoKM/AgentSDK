package io.kommunicate.agent.asyncs;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import io.kommunicate.agent.KmAgentRegistrationResponse;
import io.kommunicate.agent.listeners.AgentLoginHandler;
import io.kommunicate.agent.services.AgentClientService;
import io.kommunicate.services.KmUserClientService;
import io.kommunicate.users.KMUser;

public class AgentLoginTask extends AsyncTask<Void, Void, Boolean> {
    KMUser user;
    AgentLoginHandler handler;
    boolean isGoogleSignIn;
    boolean isSSOLogin;
    WeakReference<Context> context;
    Exception e;
    KmUserClientService userClientService;
    AgentClientService agentService;
    KmAgentRegistrationResponse response;
    boolean useEncoding;

    public AgentLoginTask(KMUser user, AgentLoginHandler handler, boolean isGoogleSignIn, boolean isSSOLogin, Context context, boolean useEncoding) {
        this.user = user;
        this.handler = handler;
        this.isGoogleSignIn = isGoogleSignIn;
        this.isSSOLogin = isSSOLogin;
        this.useEncoding = useEncoding;
        this.context = new WeakReference<>(context);
        userClientService = new KmUserClientService(this.context.get());
        agentService = new AgentClientService(this.context.get());
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            response = agentService.loginKmUser(user, isGoogleSignIn, isSSOLogin, useEncoding);
        } catch (Exception e) {
            this.e = e;
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (response != null) {
            if (handler != null) {
                if(response.getCode() != null && response.getCode().equals(AgentClientService.MULTIPLE_APPS)) {
                    handler.onMultipleApp(response.getAppList(), context.get());
                }
                else if (response.getResult() != null && response.getResult().getApplozicUser() != null && response.getResult().getApplozicUser().isRegistrationSuccess()) {
                    handler.onSuccess(response, context.get());
                } else {
                    handler.onFailure(response, e);
                }
            }
        } else {
            if (handler != null) {
                handler.onFailure(null, e);
            }
        }
    }
}
