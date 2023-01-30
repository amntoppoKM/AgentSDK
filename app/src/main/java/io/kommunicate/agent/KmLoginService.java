package io.kommunicate.agent;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.widget.Toast;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.PushNotificationTask;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import io.kommunicate.KmConversationHelper;
import io.kommunicate.KmException;

import io.kommunicate.Kommunicate;
import io.kommunicate.agent.applist.AppListActivity;
import io.kommunicate.agent.asyncs.AgentGetStatusTask;
import io.kommunicate.agent.asyncs.AgentLoginTask;
import io.kommunicate.agent.conversations.activity.AllConversationActivity;
import io.kommunicate.agent.conversations.viewmodels.KmAppSettingsViewModel;
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel;
import io.kommunicate.agent.exception.KmExceptionAnalytics;
import io.kommunicate.agent.listeners.AgentLoginHandler;
import io.kommunicate.agent.model.NetworkBoundResourceModel;
import io.kommunicate.agent.model.Resource;
import io.kommunicate.callbacks.KMLoginHandler;
import io.kommunicate.callbacks.KmCallback;
import io.kommunicate.models.KmAppSettingModel;
import io.kommunicate.users.KMUser;

public class KmLoginService {
    public static final String KM_APP_LIST = "KM_APP_LIST";
    public static final String KM_USER_PASSWORD = "KM_USER_PASSWORD";
    public static final String KM_USER_ID = "KM_USER_ID";
    public static final String IS_GOOGLE_SIGN_IN = "IS_GOOGLE_SIGN_IN";
    public static final String IS_SSO_LOGIN = "IS_SSO_LOGIN";

    public static void loginUser(final Context context, KMUser user, boolean isGoogleSignIn, boolean isSSOLogin, final ProgressDialog dialog, final Integer groupId, boolean isSwitchApplication) {

        if (user == null) {
            return;
        }

        user.setRoleType(User.RoleType.AGENT.getValue());
        user.setHideActionMessages(false);
        user.setSkipDeletedGroups(true);
        KmAppSettingsViewModel appSettingsViewModel = new ViewModelProvider((ViewModelStoreOwner) context).get(KmAppSettingsViewModel.class);


        AgentLoginHandler listener = new AgentLoginHandler() {
            @Override
            public void onSuccess(KmAgentRegistrationResponse registrationResponse, final Context context) {
                Kommunicate.init(context, registrationResponse.getResult().getApplicationId());
                Map<String, String> metadata = new HashMap<>();
                metadata.put("skipBot", "true");

                ApplozicClient.getInstance(context)
                        .setMessageMetaData(metadata)
                        .setNotificationMuteThreashold(2)
                        .hideActionMessages(false)
                        .setAppName("Kommunicate Agent")
                        .skipDeletedGroups(true)
                        .hideChatListOnNotification();

                Applozic.getInstance(context).setNotificationChannelVersion(0);
                Applozic.getInstance(context).setCustomNotificationSound("android.resource://" + context.getPackageName() + "/raw/kommunicate_tone");

                PushNotificationTask.TaskListener pushNotificationTaskListener = new PushNotificationTask.TaskListener() {
                    @Override
                    public void onSuccess(RegistrationResponse registrationResponse) {
                    }

                    @Override
                    public void onFailure(RegistrationResponse registrationResponse, Exception exception) {
                    }
                };

                PushNotificationTask pushNotificationTask = new PushNotificationTask(Applozic.getInstance(context).getDeviceRegistrationId(), pushNotificationTaskListener, context);
                pushNotificationTask.execute();
                appSettingsViewModel.fetchAppSettings().observe((LifecycleOwner) context, new Observer<Resource<KmAppSettingModel>>() {
                    @Override
                    public void onChanged(Resource<KmAppSettingModel> kmAppSetting) {
                        if(KmAppSettingsViewModel.AppSettingsCache.isTrialExpired()) {
                            AgentSharedPreference.getInstance(context).setTrialExpired(true);
                        }
                    }
                });

                new AgentGetStatusTask(context.getApplicationContext(), user.getUserId(), new AgentGetStatusTask.KmAgentGetStatusHandler() {
                    @Override
                    public void onFinished(boolean status) {
                    }

                    @Override
                    public void onError(String error) {
                    }
                }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                if (groupId != null && groupId != 0) {
                    if (ChannelService.getInstance(context).getChannelByChannelKey(groupId) != null) {
                        try {
                            KmConversationHelper.openConversation(context, false, groupId, new KmCallback() {
                                @Override
                                public void onSuccess(Object message) {
                                    finishActivity(dialog, context);
                                }

                                @Override
                                public void onFailure(Object error) {
                                    openConversation(context, null);
                                    finishActivity(dialog, context);
                                }
                            });
                        } catch (KmException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Intent launchChat = new Intent(context, KmLaunchChatService.class);
                        launchChat.putExtra(ConversationUIService.GROUP_ID, groupId);
                        launchChat.putExtra(KmLaunchChatService.LAUNCH_CHAT_RECEIVER, getResultReceiver(context, dialog));
                        context.startService(launchChat);
                    }
                } else {
                    openConversation(context, null);
                    finishActivity(dialog, context);
                }
            }

            @Override
            public void onFailure(KmAgentRegistrationResponse registrationResponse, Exception exception) {

                if (dialog != null) {
                    dialog.dismiss();
                }
                if (isSwitchApplication) {
                    MainActivity.performLogout(context, "io.kommunicate.agent.MainActivity");
                    KmToast.error(context,"Unable to switch application, try logging in again", Toast.LENGTH_SHORT).show();
                }
                else {
                    KmToast.error(context, registrationResponse != null ? registrationResponse.getMessage() : exception != null ? exception.getMessage() : Utils.getString(context, R.string.km_internal_error_text), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onMultipleApp(Map<String, String> appList, Context context) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                AgentSharedPreference.getInstance(context).setMultipleApplication(true);
                Intent intent = new Intent(context, AppListActivity.class);
                intent.putExtra(KM_APP_LIST, GsonUtils.getJsonFromObject(appList, Map.class));
                intent.putExtra(KM_USER_PASSWORD, user.getPassword());
                intent.putExtra(KM_USER_ID, user.getUserId());
                intent.putExtra(ConversationUIService.GROUP_ID, groupId);
                intent.putExtra(IS_GOOGLE_SIGN_IN, isGoogleSignIn);
                context.startActivity(intent);
                if (context instanceof Activity) {
                    ((Activity) context).finish();
                }
            }
        };

        user.setAuthenticationTypeId(User.AuthenticationType.APPLOZIC.getValue());
        new AgentLoginTask(user, listener, isGoogleSignIn, isSSOLogin, context, true).execute();
    }

    public static ResultReceiver getResultReceiver(final Context context, final ProgressDialog dialog) {
        return new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (KmLaunchChatService.RESULT_CODE == resultCode) {
                    String statusCode = resultData.getString(KmLaunchChatService.STATUS_CODE);
                    if (KmLaunchChatService.SUCCESS.equals(statusCode)) {
                        Integer groupId = resultData.getInt(KmLaunchChatService.CHANNEL_KEY);
                        if (groupId != null && groupId != 0) {
                            try {
                                KmConversationHelper.openConversation(context, false, groupId, new KmCallback() {
                                    @Override
                                    public void onSuccess(Object message) {
                                        finishActivity(dialog, context);
                                    }

                                    @Override
                                    public void onFailure(Object error) {
                                        openConversation(context, null);
                                        finishActivity(dialog, context);
                                    }
                                });
                            } catch (KmException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (KmLaunchChatService.FAILED.equals(statusCode)) {
                        openConversation(context, null);
                        finishActivity(dialog, context);
                    }
                }
            }
        };
    }

    public static void finishActivity(ProgressDialog dialog, Context context) {
        if (dialog != null) {
            dialog.dismiss();
        }
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    public static void openConversation(Context context, KmCallback callback) {
        Intent intent = new Intent(context, AllConversationActivity.class);
        context.startActivity(intent);
        if (callback != null) {
            callback.onSuccess("Successfully launched chat list");
        }
    }
}
