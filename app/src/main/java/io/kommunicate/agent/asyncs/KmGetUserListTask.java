package io.kommunicate.agent.asyncs;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;

import io.kommunicate.agent.KmUtils;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.agent.exception.KmExceptionHandle;
import io.kommunicate.agent.model.KmAgentContact;
import io.kommunicate.agent.services.AgentService;
import io.kommunicate.callbacks.KmCallback;

public class KmGetUserListTask extends AsyncTask<Void, Void, List<KmAgentContact>> {

    private WeakReference<Context> contextWeakReference;
    private Short[] roles;
    private KmCallback callback;
    private Exception exception;
    private final static String DEFAULT_BOT_NAME = "bot";

    public KmGetUserListTask(Context context, KmCallback callback, Short... roles) {
        this.contextWeakReference = new WeakReference<>(context);
        this.roles = roles;
        this.callback = callback;
    }

    @Override
    protected List<KmAgentContact> doInBackground(Void... voids) {
        try {
            return new AgentService(contextWeakReference.get()).getUserList(roles);
        } catch (Exception e) {
            e.printStackTrace();
            this.exception = e;
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<KmAgentContact> kmAgentContacts) {
        super.onPostExecute(kmAgentContacts);
        if (callback != null) {
            if (kmAgentContacts != null) {
                Iterator<KmAgentContact> iterator = kmAgentContacts.iterator();

                while (iterator.hasNext()) {
                    KmAgentContact agent = iterator.next();
                    if (TextUtils.isEmpty(agent.getUserName()) || (agent.getRoleType() == 3 && agent.getUserName().equals(DEFAULT_BOT_NAME))) {
                        iterator.remove();
                    }
                }
                callback.onSuccess(kmAgentContacts);
            } else {
                if(exception != null && KmUtils.UN_AUTHORIZED.equals(exception.getMessage())) {
                    KmExceptionHandle.getInstance(contextWeakReference.get()).handleUnauthorizedAccess();
                    return;
                } else if(exception != null) {
                    KmExceptionAnalytics.captureException(exception);
                } else {
                    KmExceptionAnalytics.captureMessage("Internal error");
                }
                callback.onFailure(exception != null ? exception.getLocalizedMessage() : "Some internal error occurred");
            }
        }
    }
}
