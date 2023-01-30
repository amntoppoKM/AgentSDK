package io.kommunicate.agent;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.people.channel.Channel;

import io.kommunicate.agent.asyncs.KmGroupInfoTask;
import io.kommunicate.agent.conversations.activity.AllConversationActivity;

public class KmLaunchChatService extends Service {

    ResultReceiver resultReceiver;
    public static final String LAUNCH_CHAT_RECEIVER = "launchChatReceiver";
    public static final String SUCCESS = "success";
    public static final String FAILED = "failed";
    public static final String CHANNEL_KEY = "channelKey";
    public static final String STATUS_CODE = "status";
    public static final int RESULT_CODE = 1011;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final int groupId = intent.getIntExtra(ConversationUIService.GROUP_ID, 0);
            resultReceiver = intent.getParcelableExtra(LAUNCH_CHAT_RECEIVER);

            if (groupId == 0) {
                stopSelf();
            } else {
                KmGroupInfoTask.GroupMemberListener listener = new KmGroupInfoTask.GroupMemberListener() {
                    @Override
                    public void onSuccess(Channel channel, Context context) {
                        if (resultReceiver != null) {
                            Bundle bundle = new Bundle();
                            bundle.putString(STATUS_CODE, SUCCESS);
                            if (channel != null) {
                                bundle.putInt(CHANNEL_KEY, channel.getKey());
                            }
                            resultReceiver.send(RESULT_CODE, bundle);
                        } else {
                            if (channel != null) {
                                openConversation(channel.getKey());
                            }
                        }
                        stopSelf();
                    }

                    @Override
                    public void onFailure(Channel channel, Exception e, Context context) {
                        if (resultReceiver != null) {
                            Bundle bundle = new Bundle();
                            bundle.putString(STATUS_CODE, FAILED);
                            resultReceiver.send(RESULT_CODE, bundle);
                        }
                        KmToast.error(context, context.getString(R.string.conversation_not_found), Toast.LENGTH_LONG).show();
                        stopSelf();
                    }
                };

                new KmGroupInfoTask(this, groupId, listener).execute();
            }

        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void openConversation(Integer groupId) {
        Intent intent = new Intent(this, AllConversationActivity.class);
        if (groupId != null && groupId != 0) {
            intent.putExtra(ConversationUIService.GROUP_ID, groupId);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
