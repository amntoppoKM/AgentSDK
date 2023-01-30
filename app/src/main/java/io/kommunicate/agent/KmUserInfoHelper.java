package io.kommunicate.agent;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.account.user.UserService;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.feed.ApiResponse;
import com.applozic.mobicomkit.uiwidgets.conversation.KmCustomDialog;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.people.contact.Contact;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.regex.Pattern;

import io.kommunicate.agent.activities.KmUserInfoActivity;
import io.kommunicate.agent.conversations.activity.AllConversationActivity;
import io.kommunicate.agent.services.AgentClientService;

public class KmUserInfoHelper {
    private static final String SUCCESS = "success";
    private static final String KM_PSEUDO_USER_KEY = "KM_PSEUDO_USER";

    public static List<String> filterAndReturnUserInfoDataKeySet(List<String> dataKeySet) {
        dataKeySet.remove(KmUserInfoHelper.KM_PSEUDO_USER_KEY); //we don't want to display this to the user, it's used internally
        return dataKeySet;
    }

    public static boolean isValidEmail(String emailId) {
        if (TextUtils.isEmpty(emailId)) {
            return false;
        }

        String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
        Pattern pattern = Pattern.compile(regex);

        return pattern.matcher(emailId).matches();
    }

    public static boolean isValidPhone(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }

        if (phoneNumber.length() < 8) {
            return false;
        }
        return true;
    }

    public static void updateUserDetails(final Activity activity, final User user) {
        new KmUserInfoUpdateTask(activity, false, user, null).execute();
    }

    public static void sendTranscript(final Activity activity, Integer groupId, String email) {
        new KmSendTranscriptTask(activity, groupId, email).execute();
    }

    public static void processDeleteConversation(final Activity activity, final Integer channelKey) {
        new KmCustomDialog.KmDialogBuilder(activity)
                .setTitle(ApplozicService.getContext(activity).getString(R.string.km_delete_dialog_title))
                .setMessage(ApplozicService.getContext(activity).getString(R.string.km_delete_dialog_message))
                .setPositiveButtonLabel(ApplozicService.getContext(activity).getString(R.string.km_delete_dialog_positive_text))
                .setPositiveButtonTextColor(R.color.km_delete_dialog_positive_button_color)
                .show(new KmCustomDialog.KmDialogClickListener() {
                    @Override
                    public void onClickNegativeButton(Dialog dialog) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onClickPositiveButton(Dialog dialog) {
                        new KmUserInfoUpdateTask(activity, true, null, channelKey).execute();
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }
                });
    }

    private static class KmUserInfoUpdateTask extends AsyncTask<Void, Void, String> {

        private WeakReference<Context> context;
        private boolean isForDelete;
        private ProgressDialog progressDialog;
        private User user;
        private Integer channelKey;

        public KmUserInfoUpdateTask(Context context, boolean isForDelete, User user, Integer channelKey) {
            this.context = new WeakReference<>(context);
            this.isForDelete = isForDelete;
            this.user = user;
            this.channelKey = channelKey;
        }

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(context.get());
            progressDialog.setMessage(ApplozicService.getContext(context.get()).getString(R.string.km_please_wait_string));
            progressDialog.setCancelable(false);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            if (isForDelete) {
                return ChannelService.getInstance(context.get()).deleteChannel(channelKey);
            } else {
                return UserService.getInstance(context.get()).updateUser(user);
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (SUCCESS.equals(s)) {
                if (isForDelete) {
                    if (context.get() instanceof Activity) {
                        Intent intent = new Intent(context.get(), AllConversationActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.get().startActivity(intent);
                        ((Activity) context.get()).finish();
                    }
                } else {
                    if (context.get() instanceof KmUserInfoActivity && user != null) {
                        Contact contact = ((KmUserInfoActivity) context.get()).getContact();
                        if (contact != null && user.getUserId() != null && user.getUserId().equals(contact.getContactIds())) {
                            if (!TextUtils.isEmpty(user.getDisplayName())) {
                                contact.setFullName(user.getDisplayName());
                            }

                            if (!TextUtils.isEmpty(user.getEmail())) {
                                contact.setEmailId(user.getEmail());
                            }

                            if (!TextUtils.isEmpty(user.getContactNumber())) {
                                contact.setContactNumber(user.getContactNumber());
                            }

                            if (!TextUtils.isEmpty(user.getImageLink())) {
                                contact.setImageURL(user.getImageLink());
                            }

                            if (user.getMetadata() != null && !user.getMetadata().isEmpty()) {
                                contact.setMetadata(user.getMetadata());
                            }
                            ((KmUserInfoActivity) context.get()).setContact(contact);
                            ((KmUserInfoActivity) context.get()).toggleUserInfoLayout(true, contact, false);
                        }
                    }
                }
            }
        }
    }

    public static class KmSendTranscriptTask extends AsyncTask<Void, Void, ApiResponse> {

        private WeakReference<Context> context;
        private Integer groupId;
        private String email;


        public KmSendTranscriptTask(Context context, Integer groupId, String email) {
            this.context = new WeakReference<>(context);
            this.groupId = groupId;
            this.email = email;
        }

        @Override
        protected ApiResponse doInBackground(Void... voids) {
            if(groupId != null && !TextUtils.isEmpty(email)) {
                AgentClientService agentClientService = new AgentClientService(context.get());
                return agentClientService.sendTranscript(groupId, email);
            }
            return null;
        }

        @Override
        protected void onPostExecute(ApiResponse response) {
            super.onPostExecute(response);
            if(response.isSuccess()) {
                if (context.get() instanceof Activity) {
                    KmToast.success(context.get(), R.string.km_transcript_success, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (context.get() instanceof Activity) {
                    KmToast.error(context.get(), R.string.km_transcript_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

}
