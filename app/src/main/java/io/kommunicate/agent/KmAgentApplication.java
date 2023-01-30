package io.kommunicate.agent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.multidex.MultiDex;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.TextUtils;

import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.applozic.mobicomkit.uiwidgets.conversation.richmessaging.callbacks.KmRichMessageListener;
import com.applozic.mobicomkit.uiwidgets.kommunicate.callbacks.KmToolbarClickListener;
import com.applozic.mobicomkit.uiwidgets.uilistener.KmActionCallback;
import com.applozic.mobicomkit.uiwidgets.uilistener.KmFragmentGetter;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.people.channel.Channel;
import com.applozic.mobicommons.people.contact.Contact;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Map;

import io.kommunicate.agent.activities.KmUserInfoActivity;
import io.kommunicate.agent.conversations.fragments.KmConversationFragment;
import io.kommunicate.services.KmChannelService;
import io.kommunicate.utils.KmConstants;

/**
 * Created by ashish on 09/02/18.
 */

public class KmAgentApplication extends Application implements KmActionCallback, KmRichMessageListener, KmToolbarClickListener, KmFragmentGetter {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ApplozicService.initApp(this);

        //To override the manifest flag to set crash reporting. Pass false to disable.
        if (!Utils.isDebugBuild(this)) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        }

        //force portrait orientation
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @SuppressLint("SourceLockedOrientationActivity")
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    @Override
    public void onReceive(Context context, Object object, String action) {
        switch (action) {
            case KmConstants.START_NEW_CHAT:
                //MainActivity.setStartNewChat(context, "reytum@live.com", null); //pass null if you want to use default bot
                break;

            case KmConstants.LOGOUT_CALL:
                MainActivity.performLogout(context, object); //object will receive the exit Activity, the one that will be launched when logout is successfull
                break;
        }
    }

    @Override
    public void onAction(Context context, String action, Message message, Object object, Map<String, Object> replyMetadata) {

    }

    @Override
    public void onClick(final Activity activity, Channel channel, Contact contact) {
        String userId = null;
        if (channel != null && Channel.GroupType.SUPPORT_GROUP.getValue().equals(channel.getType())) {
            userId = KmChannelService.getInstance(this).getUserInSupportGroup(channel.getKey());
        }

        if (activity != null && !TextUtils.isEmpty(userId)) {
            Intent intent = new Intent(activity, KmUserInfoActivity.class);
            intent.putExtra(ConversationUIService.USER_ID, userId);
            if (contact != null) {
                intent.putExtra(KmUserInfoActivity.IS_FOR_ONE_TO_ONE_CHAT, true);
            }
            if (channel != null) {
                intent.putExtra(ConversationUIService.GROUP_ID, channel.getKey());
            }
            activity.startActivity(intent);
        }
    }

    @Override
    public ConversationFragment getConversationFragment(Contact contact, Channel channel, Integer conversationId, String searchString, String messageSearchString) {
        return KmConversationFragment.newInstance(contact, channel, conversationId, searchString, messageSearchString);
    }
}
