package io.kommunicate.agent.conversations.repositories;

import android.content.Context;
import android.os.AsyncTask;

import com.applozic.mobicomkit.listners.MessageListHandler;

import io.kommunicate.agent.conversations.KmMessageListTask;

public class KmConversationListRepo {

    public final static int DEFAULT_PAGE_FETCH_SIZE = 60;

    public static void getConversationListAsync(Context context, int status, Long lastFetchTime, boolean makeNetworkCall, MessageListHandler handler) {
        new KmMessageListTask(context, status, DEFAULT_PAGE_FETCH_SIZE, lastFetchTime, makeNetworkCall, handler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void getMessageSearchListAsync(Context context, String searchString, MessageListHandler handler) {
        new KmMessageListTask(context, searchString, handler).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
