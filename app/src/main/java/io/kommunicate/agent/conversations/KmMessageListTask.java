package io.kommunicate.agent.conversations;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.exception.ApplozicException;
import com.applozic.mobicomkit.listners.MessageListHandler;
import com.applozic.mobicommons.commons.core.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.List;

import io.kommunicate.agent.KmUtils;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.services.KmMessageService;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.agent.exception.KmExceptionHandle;

public class KmMessageListTask extends AsyncTask<Void, List<Message>, List<Message>> {

    private WeakReference<Context> context;
    private Long lastFetchTime;
    private int pageFetchSize;
    private int status;
    private boolean makeNetworkCall;
    private String messageSearchString;
    private MessageListHandler messageListHandler;
    private KmMessageService mobiComConversationService;
    private ApplozicException e;

    public KmMessageListTask(Context context, int status, int pageFetchSize, Long lastFetchTime, boolean makeNetworkCall, MessageListHandler messageListHandler) {
        this(context, null, status, pageFetchSize, lastFetchTime, makeNetworkCall, messageListHandler);
    }

    public KmMessageListTask(Context context, String messageSearchString, MessageListHandler messageListHandler) {
        this(context, messageSearchString, 0, 0, null, false, messageListHandler);
    }

    public KmMessageListTask(Context context, String messageSearchString, int status, int pageFetchSize, Long lastFetchTime, boolean makeNetworkCall, MessageListHandler messageListHandler) {
        this.context = new WeakReference<>(context);
        this.lastFetchTime = lastFetchTime;
        this.status = status;
        this.pageFetchSize = pageFetchSize;
        this.messageListHandler = messageListHandler;
        this.makeNetworkCall = makeNetworkCall;
        this.messageSearchString = messageSearchString;
        mobiComConversationService = new KmMessageService(context);
    }

    @Override
    protected List<Message> doInBackground(Void... voids) {
        try {
            if (!TextUtils.isEmpty(messageSearchString)) {
                return mobiComConversationService.getConversationSearchList(messageSearchString);
            } else {
                return mobiComConversationService.getAlConversationList(status, pageFetchSize, lastFetchTime, makeNetworkCall);
            }
        } catch (Exception e) {
            this.e = new ApplozicException(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Message> messageList) {
        super.onPostExecute(messageList);
        ApplozicException defaultException = new ApplozicException(Utils.getString(context.get(), R.string.km_internal_error_text));
        if (messageListHandler != null) {
            if (messageList != null) {
                messageListHandler.onResult(messageList, null);
            } else {
                if(e != null && KmUtils.UN_AUTHORIZED.equals(e.getMessage())) {
                    KmExceptionHandle.getInstance(context.get()).handleUnauthorizedAccess();
                    return;
                } else if(e != null) {
                    KmExceptionAnalytics.captureException(e);
                }
                else {
                    KmExceptionAnalytics.captureMessageWithLogs(context.get(), "Internal error");
                }
                messageListHandler.onResult(null, e != null ? e : defaultException);
            }
        }
    }
}
