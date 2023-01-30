package io.kommunicate.agent.asyncs;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.lang.ref.WeakReference;

import io.kommunicate.agent.services.AgentClientService;

/**
 * Created by ashish on 13/02/18.
 */

public class KmUserPasswordResetTask extends AsyncTask<Void, Void, String> {

    private WeakReference<Context> context;
    private String userId;
    private String applicationId;
    private KmPassResetHandler handler;

    public KmUserPasswordResetTask(Context context, String userId, String applicationId, KmPassResetHandler handler) {
        this.context = new WeakReference<Context>(context);
        this.userId = userId;
        this.applicationId = applicationId;
        this.handler = handler;
    }

    @Override
    protected String doInBackground(Void... voids) {
        return new AgentClientService(context.get()).resetUserPassword(userId, applicationId);
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (handler != null) {
            if (!TextUtils.isEmpty(s)) {
                handler.onSuccess(context.get(), s);
            } else {
                handler.onFailure(context.get(), "Some error occurred");
            }
        }
    }

    public interface KmPassResetHandler {
        void onSuccess(Context context, String response);

        void onFailure(Context context, String error);
    }
}
