package io.kommunicate.agent.model;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ApplozicService;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicommons.json.GsonUtils;

import io.kommunicate.agent.R;
import io.kommunicate.agent.asyncs.ConversationStatusUpdateTask;
import io.kommunicate.callbacks.KmCallback;

public class KmConversationStatus {
    private static final String TAG = "KmConversationStatus";
    public static final String OPEN_STATUS_NAME = getString(R.string.km_resolve_conversation);
    public static final String RESOLVED_STATUS_NAME = getString(R.string.km_reopen_conversation);
    public static final String MARK_AS_SPAM = getString(R.string.km_status_mark_as_spam);
    public static final int STATUS_OPEN = 0;
    public static final int STATUS_RESOLVED = 2;
    public static final int STATUS_SPAM = 3;
    public static final String OLD_RESOLVED_NAME = "closed";

    public static String getStatusText(int status) {
        switch (status) {
            case 2:
            case 3:
                return RESOLVED_STATUS_NAME;
            default:
                return OPEN_STATUS_NAME;
        }
    }

    public static String getStatusName(int status) {
        switch (status) {
            case 2:
                return getString(R.string.km_status_resolved);
            case 3:
                return getString(R.string.km_status_spam);
            default:
                return getString(R.string.km_status_open);
        }
    }

    public static int getIconId(boolean spam) {
        if (spam) {
            return R.drawable.ic_spam;
        } else {
            return R.drawable.ic_resolve;
        }
    }

    public static int getColorId(int status) {
        switch (status) {
            case 2:
            case 3:
                return R.color.km_reopen_status_color;
            default:
                return R.color.km_resolve_status_color;
        }
    }

    public static int getStatusForUpdate(int currentStatus) {
        if (currentStatus == STATUS_RESOLVED) {
            return STATUS_OPEN;
        }
        return STATUS_RESOLVED;
    }

    public static int getStatusFromName(String name) {
        if (getString(R.string.km_status_resolved).equals(name) || OLD_RESOLVED_NAME.equalsIgnoreCase(name)) {
            return STATUS_RESOLVED;
        } else if (getString(R.string.km_status_spam).equals(name)) {
            return STATUS_SPAM;
        }
        return STATUS_OPEN;
    }

    private static String getString(int resId) {
        return Utils.getString(ApplozicService.getAppContext(), resId);
    }

    public static void updateConversationStatus(Context context, final int newStatus, final Integer conversationId) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage(Utils.getString(context, R.string.km_status_update_text));
        dialog.setCancelable(false);
        dialog.show();
        new ConversationStatusUpdateTask(conversationId, newStatus, true, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                dialog.dismiss();
            }

            @Override
            public void onFailure(Object error) {
                KmToast.error(context, GsonUtils.getJsonFromObject(error, Object.class), Toast.LENGTH_SHORT).show();
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static boolean isConversationResolved(int status) {
        return status == STATUS_RESOLVED || status == STATUS_SPAM;
    }
}
