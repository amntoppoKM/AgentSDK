package io.kommunicate.agent.conversations.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.core.app.NavUtils;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.api.MobiComKitConstants;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicomkit.uiwidgets.conversation.fragment.ConversationFragment;
import com.applozic.mobicommons.commons.core.utils.Utils;

import io.kommunicate.agent.KmAssigneeListHelper;
import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.KmConversationStatusListener;

public class CustomConversationActivity extends ConversationActivity implements KmConversationStatusListener {

    public ResultReceiver resultReceiver;
    public static final int STATUS_RESULT_CODE = 100;
    public static final int ASSIGNEE_RESULT_CODE = 101;
    public static final String NEW_STATUS = "NEW_STATUS";
    public static final String NEW_ASSIGNEE_ID = "NEW_ASSIGNEE_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resultReceiver = (ResultReceiver) getIntent().getParcelableExtra(AllConversationActivity.CONVERSATION_RESULT_RECEIVER);
        KmAssigneeListHelper.fetchAssigneeList(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        resultReceiver = (ResultReceiver) intent.getParcelableExtra(AllConversationActivity.CONVERSATION_RESULT_RECEIVER);
        if (intent.hasExtra(MobiComKitConstants.QUICK_LIST)) {
            Intent conversationListActivity = new Intent(this, AllConversationActivity.class);
            startActivity(conversationListActivity);
            finish();
        } else {
            super.onNewIntent(intent);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
                try {
                    String parentActivity = NavUtils.getParentActivityName(this);
                    if (parentActivity != null) {
                        Intent intent = new Intent(this, Class.forName(parentActivity));
                        startActivity(intent);
                    }
                    finish();
                    return true;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            boolean takeOrder = getIntent().getBooleanExtra(TAKE_ORDER, false);
            if (takeOrder && getSupportFragmentManager().getBackStackEntryCount() == 2) {
                try {
                    String parentActivity = NavUtils.getParentActivityName(this);
                    if (parentActivity != null) {
                        Intent intent = new Intent(this, AllConversationActivity.class);
                        startActivity(intent);
                    }
                    finish();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                getSupportFragmentManager().popBackStack();
            }
            Utils.toggleSoftKeyBoard(this, true);
            return true;
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            try {
                try {
                    String parentActivity = NavUtils.getParentActivityName(this);
                    if (parentActivity != null) {
                        Intent intent = new Intent(this, Class.forName(parentActivity));
                        startActivity(intent);
                    }
                    finish();
                    return;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
            return;
        }
        boolean takeOrder = getIntent().getBooleanExtra(TAKE_ORDER, false);
        ConversationFragment conversationFragment = (ConversationFragment) getSupportFragmentManager().findFragmentByTag(ConversationUIService.CONVERSATION_FRAGMENT);
        if (conversationFragment != null && conversationFragment.isVisible() && conversationFragment.isAttachmentOptionsOpen()) {
            conversationFragment.handleAttachmentToggle();
            return;
        }

        if (takeOrder && getSupportFragmentManager().getBackStackEntryCount() == 2) {
            try {
                String parentActivity = NavUtils.getParentActivityName(this);
                if (parentActivity != null) {
                    Intent intent = new Intent(this, Class.forName(parentActivity));
                    startActivity(intent);
                }
                finish();
                return;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            finish();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onStatusChange(int newStatus) {
        if (resultReceiver != null) {
            Bundle bundle = new Bundle();
            bundle.putInt(NEW_STATUS, newStatus);
            resultReceiver.send(STATUS_RESULT_CODE, bundle);
        }
    }

    @Override
    public void onAssigneeChange(String newAssigneeId, String teamId) {
        if (resultReceiver != null) {
            Bundle bundle = new Bundle();
            bundle.putString(NEW_ASSIGNEE_ID, newAssigneeId);
            resultReceiver.send(ASSIGNEE_RESULT_CODE, bundle);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Applozic.connectPublishWithVerifyToken(this, getString(R.string.please_wait_info));
    }

    @Override
    protected void syncMessages() {
        //Do not sync messages for agent app
        //We trust push notifications
    }
}
