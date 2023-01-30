package io.kommunicate.agent;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;

import java.util.List;

import io.kommunicate.agent.conversations.activity.AllConversationActivity;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.users.KMUser;
import io.sentry.Sentry;
import io.sentry.protocol.User;

public class SplashScreenActivity extends AppCompatActivity {

    public static int SPLASH_DISPLAY_LENGTH = 2000;
    Integer groupId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        if (getIntent() != null) {
            Uri data = getIntent().getData();
            if (data != null) {
                List<String> pathSegments = data.getPathSegments();
                if (pathSegments != null && !pathSegments.isEmpty() && pathSegments.size() >= 2) {
                    try {
                        groupId = Integer.parseInt(pathSegments.get(1));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        new Handler().postDelayed(() -> {
            if (KMUser.isLoggedIn(SplashScreenActivity.this)) {
                User user = new User();
                user.setId(KMUser.getLoggedInUser(this).getUserId());
                Sentry.setUser(user);
                ApplozicClient.getInstance(SplashScreenActivity.this).hideChatListOnNotification();
                KmExceptionAnalytics.deleteLogFile(SplashScreenActivity.this);
                Intent intent = new Intent(SplashScreenActivity.this, AllConversationActivity.class);
                if (groupId != null) {
                    if (ChannelService.getInstance(SplashScreenActivity.this).getChannelByChannelKey(groupId) != null) {
                        intent.putExtra(ConversationUIService.GROUP_ID, groupId);
                    } else {
                        Intent launchChat = new Intent(SplashScreenActivity.this, KmLaunchChatService.class);
                        launchChat.putExtra(ConversationUIService.GROUP_ID, groupId);
                        SplashScreenActivity.this.startService(launchChat);
                    }
                }
                SplashScreenActivity.this.startActivity(intent);
            } else {
                Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                if (groupId != null) {
                    intent.putExtra(ConversationUIService.GROUP_ID, groupId);
                }
                SplashScreenActivity.this.startActivity(intent);
            }
            SplashScreenActivity.this.finish();
            groupId = null;
        }, SPLASH_DISPLAY_LENGTH);
    }
}
