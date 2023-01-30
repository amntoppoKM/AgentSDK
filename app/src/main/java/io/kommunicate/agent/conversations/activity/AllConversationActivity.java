package io.kommunicate.agent.conversations.activity;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.kommunicate.views.KmToast;
import com.applozic.mobicommons.ALSpecificSettings;
import com.applozic.mobicommons.commons.core.utils.Utils;
import com.applozic.mobicomkit.listners.KmStatusListener;


import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.api.conversation.Message;
import com.applozic.mobicomkit.broadcast.AlEventManager;
import com.applozic.mobicomkit.listners.ApplozicUIListener;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicommons.data.AlPrefSettings;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.google.android.material.tabs.TabLayout;

import io.kommunicate.agent.KmLoginService;
import io.kommunicate.agent.activities.UserReportActivity;
import io.kommunicate.agent.applist.AppListActivity;
import io.kommunicate.agent.asyncs.KmGetAppListTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.kommunicate.agent.AgentSharedPreference;
import io.kommunicate.agent.MainActivity;
import io.kommunicate.agent.activities.KmUserProfileActivity;
import io.kommunicate.agent.activities.SearchActivity;
import io.kommunicate.agent.adapters.KmConversationPagerAdapter;
import io.kommunicate.agent.asyncs.AgentGetStatusTask;
import io.kommunicate.agent.conversations.KmConversationUtils;
import io.kommunicate.agent.conversations.fragments.KmConversationListFragment;
import io.kommunicate.agent.conversations.viewmodels.KmAppSettingsViewModel;
import io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel;

import io.kommunicate.agent.R;
import io.kommunicate.agent.conversations.adapters.KmNavigationItemAdapter;
import io.kommunicate.agent.conversations.viewmodels.KmNavItemModel;
import io.kommunicate.agent.model.NetworkBoundResourceModel;
import io.kommunicate.agent.viewmodels.KmTeamViewModel;
import io.kommunicate.utils.KmConstants;
import io.kommunicate.users.KMUser;
import io.sentry.Sentry;
import io.sentry.protocol.User;

import static io.kommunicate.agent.conversations.viewmodels.KmConversationViewModel.STATUS_ONLINE;

public class AllConversationActivity extends AppCompatActivity implements KmNavigationItemAdapter.KmNavItemClickListener, ApplozicUIListener, TabLayout.OnTabSelectedListener, KmStatusListener {

    public static final String CONVERSATION_RESULT_RECEIVER = "CONVERSATION_RESULT_RECEIVER";
    private DrawerLayout mDrawerLayout;
    private Message message;
    private KmConversationListFragment assignedConversationListFragment;
    private KmConversationListFragment allConversationListFragment;
    private KmConversationListFragment closedConversationListFragment;
    private KmNavigationItemAdapter kmNavigationItemAdapter;
    private KmConversationPagerAdapter conversationPagerAdapter;
    private TabLayout kmConversationTabLayout;
    private ViewPager kmConversationViewPager;
    private ConstraintLayout kmBillingExpiredLayout;
    private FrameLayout parentFrameLayout;
    private boolean isBillingExpired = false;
    private LinearLayout kmConversationLinearLayout;
    List<String> adminTitleList = new ArrayList<>();
    List<String> operatorTitleList = new ArrayList<>();
    Menu mainMenu;

    KmConversationViewModel viewModel;
    KmTeamViewModel teamViewModel;
    KmAppSettingsViewModel appSettingsViewModel;
    AgentSharedPreference agentSharedPreference;

    private static final String TAG = "AllConversationActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_km_all_conversation);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        agentSharedPreference = AgentSharedPreference.getInstance(getApplicationContext());

        kmConversationTabLayout = findViewById(R.id.km_conversation_tab_layout);
        kmConversationViewPager = findViewById(R.id.km_conversation_view_pager);
        kmConversationTabLayout.setupWithViewPager(kmConversationViewPager);
        kmConversationTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        kmConversationTabLayout.addOnTabSelectedListener(this);
        kmConversationLinearLayout = findViewById(R.id.km_conversation_linear_layout);
        parentFrameLayout = findViewById(R.id.content_frame);
        kmBillingExpiredLayout = findViewById(R.id.km_billing_expired_layout);
        adminTitleList.add(Utils.getString(this, R.string.km_assigned_conversations));
        adminTitleList.add(Utils.getString(this, R.string.km_all_conversations));
        adminTitleList.add(Utils.getString(this, R.string.km_status_resolved));
        operatorTitleList.add(Utils.getString(this, R.string.km_assigned_conversations));
        operatorTitleList.add(Utils.getString(this, R.string.km_status_resolved));
        mDrawerLayout = findViewById(R.id.km_drawer_layout);
        RecyclerView navigationDrawerRecyclerView = findViewById(R.id.km_nav_drawer_item_list);

        ImageView profileImage = findViewById(R.id.profileImageView);
        TextView alphabeticImageTv = findViewById(R.id.kmAlphabeticTextView);
        TextView displayNameTv = findViewById(R.id.profileNameTv);
        TextView emailIdTv = findViewById(R.id.emailIdTv);
        isBillingExpired = agentSharedPreference.isTrialExpired();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_profile_nav_icon);
            if (isBillingExpired) {
                actionBar.setTitle(R.string.billing_expired_pagename);
            } else {
                actionBar.setTitle(R.string.conversations);
            }
        }
        showBillingExpiredLayout(isBillingExpired);

        Applozic.connectPublishWithVerifyToken(this, getString(R.string.please_wait_info));
        Applozic.subscribeToSupportGroup(this, true);
        Applozic.subscribeToSupportGroup(this, false);

        AlEventManager.getInstance().registerUIListener("agentlistener", this);
        AlEventManager.getInstance().registerStatusListener("agentlistener", this);

        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });

        viewModel = ViewModelProviders.of(this).get(KmConversationViewModel.class);
        viewModel.getLoggedInUser(this).observe(this, contact -> {
            if (contact != null) {
                KmConversationUtils.loadContactImage(this, contact, alphabeticImageTv, profileImage);

                if (!TextUtils.isEmpty(contact.getDisplayName())) {
                    displayNameTv.setText(contact.getDisplayName());
                }

                if (!TextUtils.isEmpty(contact.getEmailId())) {
                    emailIdTv.setText(contact.getEmailId());
                } else if (!TextUtils.isEmpty(contact.getUserId())) {
                    emailIdTv.setText(contact.getUserId());
                }
            }
        });

        viewModel.getAgentStatus().observe(this, new Observer<NetworkBoundResourceModel<Integer>>() {
            @Override
            public void onChanged(NetworkBoundResourceModel<Integer> integerNetworkBoundResourceModel) {
                if (integerNetworkBoundResourceModel != null && integerNetworkBoundResourceModel.getResource() != null) {
                    toggleAwayIndicator(integerNetworkBoundResourceModel.getResource(), findViewById(R.id.onlineTextView));
                    kmNavigationItemAdapter.updateAwayStatus(integerNetworkBoundResourceModel.getResource());
                }
            }
        });

        viewModel.retrieveAgentStatus(AllConversationActivity.this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        navigationDrawerRecyclerView.setLayoutManager(linearLayoutManager);
        kmNavigationItemAdapter = new KmNavigationItemAdapter(this, this);

        navigationDrawerRecyclerView.setAdapter(kmNavigationItemAdapter);

        assignedConversationListFragment = KmConversationListFragment.newInstance(Channel.ASSIGNED_CONVERSATIONS);
        allConversationListFragment = KmConversationListFragment.newInstance(Channel.ALL_CONVERSATIONS);
        closedConversationListFragment = KmConversationListFragment.newInstance(Channel.CLOSED_CONVERSATIONS);

        checkIfBillingExpired();
        setupPagerAdapter();
        processChatId(getIntent());
    }

    private void showBillingExpiredLayout(boolean show) {
        kmConversationTabLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        kmBillingExpiredLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        kmConversationLinearLayout.setVisibility(show ? View.GONE : View.VISIBLE);
        getSupportActionBar().setTitle(show ? "Trial end" : "Conversations");
    }

    private void checkIfBillingExpired() {
        appSettingsViewModel = new ViewModelProvider(this).get(KmAppSettingsViewModel.class);
        appSettingsViewModel.fetchAppSettings().observe(this, appSetting -> {
            showBillingExpiredLayout(KmAppSettingsViewModel.AppSettingsCache.isTrialExpired());
            if (agentSharedPreference.isTrialExpired() != KmAppSettingsViewModel.AppSettingsCache.isTrialExpired()) {
                agentSharedPreference.setTrialExpired(KmAppSettingsViewModel.AppSettingsCache.isTrialExpired());
            }
            kmNavigationItemAdapter.updateNavigationItem();
        });
    }

    public void setupPagerAdapter() {
        teamViewModel = new ViewModelProvider(this).get(KmTeamViewModel.class);
        teamViewModel.getDefaultTeamDetails().observe(this, teamData -> {
            if (teamData.isSuccess() && teamData.getData() != null) {
                agentSharedPreference.setAgentRouting(teamData.getData().getAgentRouting());
                MobiComUserPreference.getInstance(getApplicationContext()).setNotifyEverybody(!teamData.getData().getAgentRouting());
                if (agentSharedPreference.getAgentRoleType().equals(AgentGetStatusTask.AgentDetail.RoleType.OPERATOR.getValue())
                        && agentSharedPreference.isAgentRouting()) {
                    conversationPagerAdapter = new KmConversationPagerAdapter(this, operatorTitleList, getSupportFragmentManager());
                    conversationPagerAdapter.addFragment(assignedConversationListFragment);
                    conversationPagerAdapter.addFragment(closedConversationListFragment);
                    kmConversationViewPager.setAdapter(conversationPagerAdapter);
                    return;
                }
            }
            conversationPagerAdapter = new KmConversationPagerAdapter(this, adminTitleList, getSupportFragmentManager());
            conversationPagerAdapter.addFragment(assignedConversationListFragment);
            conversationPagerAdapter.addFragment(allConversationListFragment);
            conversationPagerAdapter.addFragment(closedConversationListFragment);
            kmConversationViewPager.setAdapter(conversationPagerAdapter);
        });
    }

    public void toggleAwayIndicator(int status, TextView indicatorTextView) {
        GradientDrawable drawable = (GradientDrawable) indicatorTextView.getBackground();
        if (drawable != null) {
            drawable.setColor(Utils.getColor(this, status == STATUS_ONLINE ? R.color.km_user_online_text_color : R.color.km_user_away_text_color));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processChatId(intent);
    }

    public void processChatId(Intent intent) {
        if (intent != null) {
            int chatId = intent.getIntExtra(ConversationUIService.GROUP_ID, 0);
            if (chatId > 0) {
                Intent launchIntent = new Intent(this, CustomConversationActivity.class);
                launchIntent.putExtra(ConversationUIService.GROUP_ID, chatId);
                launchIntent.putExtra(ConversationUIService.TAKE_ORDER, true);
                startActivity(launchIntent);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            finish();
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStackImmediate();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.retrieveAgentStatus(AllConversationActivity.this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.search_option:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onKmNavItemClick(int position, KmNavItemModel model) {
        mDrawerLayout.closeDrawer(GravityCompat.START);
        switch (model.getPosition()) {
            case 0:
                viewModel.toggleAgentStatus(this).observe(this, new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer integer) {
                    }
                });
                break;
            case 1:
                startActivity(new Intent(this, KmUserProfileActivity.class));
                break;
            case 2:
                KMUser user = KMUser.getLoggedInUser(this);
                if (!agentSharedPreference.isGoogleLogin() && user.getPassword() == null) {
                    KmToast.error(AllConversationActivity.this, getString(R.string.km_switch_application_error), Toast.LENGTH_LONG).show();
                    break;
                }
                switchApplicationHandle(user.getUserId(), agentSharedPreference.isGoogleLogin() ? "" : user.getPassword(), agentSharedPreference.isGoogleLogin());
                break;
            case 3:
                startActivity(new Intent(this, UserReportActivity.class));
                break;
            case 4:
                MainActivity.performLogout(this, "io.kommunicate.agent.MainActivity");
                break;
        }
    }

    private void switchApplicationHandle(final String email, final String password, final boolean isGoogleSignIn) {
        Intent intent = new Intent(AllConversationActivity.this, AppListActivity.class);
        intent.putExtra(MainActivity.KM_APP_LIST, GsonUtils.getJsonFromObject(agentSharedPreference.getAppList(), Map.class));
        intent.putExtra(MainActivity.KM_USER_PASSWORD, password);
        intent.putExtra(MainActivity.KM_USER_ID, email);
        intent.putExtra(MainActivity.IS_GOOGLE_SIGN_IN, isGoogleSignIn);
        startActivity(intent);
        ((Activity) this).finish();
    }

    @Override
    public void onMessageSent(Message message) {

    }

    @Override
    public void onMessageReceived(Message message) {
        processMessage(message);
    }

    @Override
    public void onLoadMore(boolean loadMore) {

    }

    @Override
    public void onMessageSync(Message message, String key) {
        processMessage(message);
    }

    @Override
    public void onMessageDeleted(String messageKey, String userId) {
        notifyAllFragments();
    }

    @Override
    public void onMessageDelivered(Message message, String userId) {
    }

    @Override
    public void onAllMessagesDelivered(String userId) {

    }

    @Override
    public void onAllMessagesRead(String userId) {
        notifyAllFragments();
    }

    @Override
    public void onConversationDeleted(String userId, Integer channelKey, String response) {
        if ("success".equals(response)) {
            assignedConversationListFragment.removeConversation(userId, channelKey);
            allConversationListFragment.removeConversation(userId, channelKey);
            closedConversationListFragment.removeConversation(userId, channelKey);
        }
    }

    @Override
    public void onUpdateTypingStatus(String userId, String isTyping) {

    }

    @Override
    public void onUpdateLastSeen(String userId) {

    }

    @Override
    public void onMqttDisconnected() {

    }

    @Override
    public void onMqttConnected() {

    }

    @Override
    public void onUserOnline() {

    }

    @Override
    public void onUserOffline() {

    }

    @Override
    public void onUserActivated(boolean isActivated) {

    }

    @Override
    public void onChannelUpdated() {
        notifyAllFragments();
    }

    @Override
    public void onConversationRead(String userId, boolean isGroup) {
        notifyAllFragments();
    }

    @Override
    public void onUserDetailUpdated(String userId) {
        notifyAllFragments();
    }

    @Override
    public void onMessageMetadataUpdated(String keyString) {
        notifyAllFragments();
    }

    @Override
    public void onUserMute(boolean mute, String userId) {

    }

    @Override
    public void onGroupMute(Integer groupId) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isBillingExpired) {
            getMenuInflater().inflate(R.menu.km_main_menu, menu);
        } else {
            getMenuInflater().inflate(R.menu.km_trial_expired_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        if (Utils.getString(this, R.string.km_all_conversations).equals(tab.getText().toString())) {
            allConversationListFragment.refreshMessageList();
        } else if (Utils.getString(this, R.string.km_status_resolved).equals(tab.getText().toString())) {
            closedConversationListFragment.refreshMessageList();
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    public interface MessageListLoadFinishCallback {
        void onFinish(int status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AlEventManager.getInstance().unregisterUIListener("agentlistener");
        AlEventManager.getInstance().unregisterStatusListener("agentlistener");
        Applozic.disconnectPublish(this);
        Applozic.unSubscribeToSupportGroup(this, true);
        Applozic.unSubscribeToSupportGroup(this, false);
    }

    @Override
    public void onStatusChange(String userid, Integer status) {
        if (TextUtils.isEmpty(userid) || status == null) {
            return;
        }
        String loggedInUserId = MobiComUserPreference.getInstance(this).getUserId();
        if (!TextUtils.isEmpty(loggedInUserId) && loggedInUserId.equals(userid)) {
            toggleAwayIndicator(status == KmConstants.STATUS_AWAY ? KmConversationViewModel.STATUS_AWAY : KmConversationViewModel.STATUS_ONLINE, findViewById(R.id.onlineTextView));
            kmNavigationItemAdapter.updateAwayStatus(status == KmConstants.STATUS_AWAY ? KmConversationViewModel.STATUS_AWAY : KmConversationViewModel.STATUS_ONLINE);
            NetworkBoundResourceModel<Integer> agentStatus = new NetworkBoundResourceModel<>(status == KmConstants.STATUS_AWAY ? KmConversationViewModel.STATUS_AWAY : KmConversationViewModel.STATUS_ONLINE, NetworkBoundResourceModel.RESOURCE_OK);
            viewModel.getAgentStatus().postValue(agentStatus);
        }
    }

    public void processMessage(Message message) {
        this.message = message;
        if (KmConversationUtils.isTypeClosed(message)) {
            assignedConversationListFragment.removeMessage(message);
            allConversationListFragment.removeMessage(message);
            if (agentSharedPreference.getAgentRoleType().equals(AgentGetStatusTask.AgentDetail.RoleType.OPERATOR.getValue()) && agentSharedPreference.isAgentRouting()) {
                if (message.getGroupId() != null && message.getGroupId() != 0) {
                    Channel channel = ChannelService.getInstance(this).getChannel(message.getGroupId());
                    if (channel != null && MobiComUserPreference.getInstance(this).getUserId().equals(channel.getConversationAssignee())) {
                        closedConversationListFragment.addMessage(message);
                    }
                }
            } else {
                closedConversationListFragment.addMessage(message);
            }
        } else if (KmConversationUtils.isTypeOpen(message)) {
            closedConversationListFragment.removeMessage(message);
            if (message.getGroupId() != null && message.getGroupId() != 0) {
                Channel channel = ChannelService.getInstance(this).getChannel(message.getGroupId());
                if (channel != null && Channel.GroupType.SUPPORT_GROUP.getValue().equals(channel.getType())) {
                    if (MobiComUserPreference.getInstance(this).getUserId().equals(channel.getConversationAssignee())) {
                        assignedConversationListFragment.addMessage(message);
                    }
                    allConversationListFragment.addMessage(message);
                }
            }
        } else if (KmConversationUtils.isTypeDelete(message)) {
            assignedConversationListFragment.removeMessage(message);
            allConversationListFragment.removeMessage(message);
            closedConversationListFragment.removeMessage(message);
        } else if (KmConversationUtils.isTypeAssigneeSwitch(message)) {
            if (MobiComUserPreference.getInstance(this).getUserId().equals(message.getAssigneId())) {
                assignedConversationListFragment.addMessage(message);
            } else {
                allConversationListFragment.addMessage(message);
                assignedConversationListFragment.removeMessage(message);
            }
            closedConversationListFragment.removeMessage(message);
        } else {
            int status = KmConversationUtils.addToStatus(this, message);
            if (status == 0) {
                assignedConversationListFragment.addMessage(message);
            } else if (status == Channel.ASSIGNED_CONVERSATIONS) {
                assignedConversationListFragment.addMessage(message);
                allConversationListFragment.addMessage(message);
            } else if (status == Channel.ALL_CONVERSATIONS) {
                allConversationListFragment.addMessage(message);
            } else if (status == Channel.CLOSED_CONVERSATIONS) {
                closedConversationListFragment.addMessage(message);
            }
        }
    }

    public void notifyAllFragments() {
        assignedConversationListFragment.notifyAdapter();
        allConversationListFragment.notifyAdapter();
        closedConversationListFragment.notifyAdapter();
    }
}
